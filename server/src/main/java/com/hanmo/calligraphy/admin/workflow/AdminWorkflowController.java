package com.hanmo.calligraphy.admin.workflow;

import com.hanmo.calligraphy.admin.workflow.AdminWorkflowDtos.LeaveActionRequest;
import com.hanmo.calligraphy.admin.workflow.AdminWorkflowDtos.MakeupRequest;
import com.hanmo.calligraphy.admin.workflow.AdminWorkflowDtos.RollcallRequest;
import com.hanmo.calligraphy.common.ApiResponse;
import com.hanmo.calligraphy.security.LoginUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * 后台点名与请假联动接口：课次列表、点名名单、点名消课、请假审批、补课安排。
 *
 * <p>路由前缀 /api/admin/workflow。逻辑与事务全部在 AdminWorkflowService，
 * 操作人由 @AuthenticationPrincipal 注入，用于出勤留痕与审计。</p>
 * 落库表：lesson、attendance、leave_request、makeup_arrangement、hour_account、hour_ledger、sys_audit_log（经 service 落库）
 */
@Tag(name = "后台-点名与请假联动")
@RestController
@RequestMapping("/api/admin/workflow")
@RequiredArgsConstructor
public class AdminWorkflowController {

    private final AdminWorkflowService workflowService;

    /**
     * 点名用课次列表。
     *
     * <p>只读；date 按 ISO 日期格式传入，status 为空不过滤。</p>
     */
    @Operation(summary = "课次列表（点名用）")
    @GetMapping("/lessons")
    public ApiResponse<List<Map<String, Object>>> lessons(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(required = false) String status) {
        return ApiResponse.ok(workflowService.lessons(date, status));
    }

    /**
     * 课次点名名单。
     *
     * <p>只读；含每位学员当前出勤标记与待审批请假提示。</p>
     *
     * @throws BusinessException 404 课次不存在
     * @return lesson/students/summary
     */
    @Operation(summary = "课次点名名单")
    @GetMapping("/lessons/{id}/roster")
    public ApiResponse<Map<String, Object>> roster(@PathVariable Long id) {
        return ApiResponse.ok(workflowService.roster(id));
    }

    /**
     * 后台点名并自动消课。
     *
     * <p>副作用：写 attendance、按差额调整 hour_account、写 hour_ledger、课次置 DONE、
     * 写审计日志，全部在同一事务；重复提交同状态不会重复扣课。</p>
     *
     * @throws BusinessException 课次不存在/已取消、名单为空或重复、学员不在班级
     */
    @Operation(summary = "后台点名并自动消课")
    @PostMapping("/lessons/{id}/rollcall")
    public ApiResponse<Map<String, Object>> rollcall(@AuthenticationPrincipal LoginUser user,
                                                     @PathVariable Long id,
                                                     @RequestBody RollcallRequest request) {
        return ApiResponse.ok(workflowService.rollcall(id, request, user));
    }

    /**
     * 为已通过的请假安排补课。
     *
     * <p>副作用：写 makeup_arrangement、请假置 ARRANGED、写审计；不扣课时。</p>
     *
     * @throws BusinessException 请假未通过/已安排、补课课次不存在或已取消
     */
    @Operation(summary = "安排补课")
    @PostMapping("/leaves/{id}/makeup")
    public ApiResponse<Map<String, Object>> arrangeMakeup(@AuthenticationPrincipal LoginUser user,
                                                          @PathVariable Long id,
                                                          @Valid @RequestBody MakeupRequest request) {
        return ApiResponse.ok(workflowService.arrangeMakeup(id, request, user));
    }

    /**
     * 后台审批请假并联动课时。
     *
     * <p>副作用：更新请假状态与审批人；通过时写 LEAVE 出勤、按 deduct_hours 补扣/返还课时、
     * 写课时流水；写审计日志，同一事务。</p>
     *
     * @throws BusinessException 请假不存在/已处理、驳回未填原因
     */
    @Operation(summary = "后台审批请假并联动课时")
    @PostMapping("/leaves/{id}/approve")
    public ApiResponse<Map<String, Object>> approveLeave(@AuthenticationPrincipal LoginUser user,
                                                         @PathVariable Long id,
                                                         @RequestBody LeaveActionRequest request) {
        return ApiResponse.ok(workflowService.handleLeave(id, request, user));
    }
}
