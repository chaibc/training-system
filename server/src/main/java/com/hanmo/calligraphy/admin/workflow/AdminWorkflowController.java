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

@Tag(name = "后台-点名与请假联动")
@RestController
@RequestMapping("/api/admin/workflow")
@RequiredArgsConstructor
public class AdminWorkflowController {

    private final AdminWorkflowService workflowService;

    @Operation(summary = "课次列表（点名用）")
    @GetMapping("/lessons")
    public ApiResponse<List<Map<String, Object>>> lessons(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(required = false) String status) {
        return ApiResponse.ok(workflowService.lessons(date, status));
    }

    @Operation(summary = "课次点名名单")
    @GetMapping("/lessons/{id}/roster")
    public ApiResponse<Map<String, Object>> roster(@PathVariable Long id) {
        return ApiResponse.ok(workflowService.roster(id));
    }

    @Operation(summary = "后台点名并自动消课")
    @PostMapping("/lessons/{id}/rollcall")
    public ApiResponse<Map<String, Object>> rollcall(@AuthenticationPrincipal LoginUser user,
                                                     @PathVariable Long id,
                                                     @RequestBody RollcallRequest request) {
        return ApiResponse.ok(workflowService.rollcall(id, request, user));
    }

    @Operation(summary = "安排补课")
    @PostMapping("/leaves/{id}/makeup")
    public ApiResponse<Map<String, Object>> arrangeMakeup(@AuthenticationPrincipal LoginUser user,
                                                          @PathVariable Long id,
                                                          @Valid @RequestBody MakeupRequest request) {
        return ApiResponse.ok(workflowService.arrangeMakeup(id, request, user));
    }

    @Operation(summary = "后台审批请假并联动课时")
    @PostMapping("/leaves/{id}/approve")
    public ApiResponse<Map<String, Object>> approveLeave(@AuthenticationPrincipal LoginUser user,
                                                         @PathVariable Long id,
                                                         @RequestBody LeaveActionRequest request) {
        return ApiResponse.ok(workflowService.handleLeave(id, request, user));
    }
}
