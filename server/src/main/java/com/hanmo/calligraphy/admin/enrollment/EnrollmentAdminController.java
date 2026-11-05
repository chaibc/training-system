package com.hanmo.calligraphy.admin.enrollment;

import com.hanmo.calligraphy.admin.enrollment.EnrollmentAdminDtos.ConfirmRequest;
import com.hanmo.calligraphy.admin.enrollment.EnrollmentAdminDtos.CreateEnrollmentRequest;
import com.hanmo.calligraphy.admin.enrollment.EnrollmentAdminDtos.EnrollmentRow;
import com.hanmo.calligraphy.admin.enrollment.EnrollmentAdminDtos.FollowUpRequest;
import com.hanmo.calligraphy.admin.enrollment.EnrollmentAdminDtos.PayRequest;
import com.hanmo.calligraphy.admin.enrollment.EnrollmentAdminDtos.PaymentRow;
import com.hanmo.calligraphy.admin.enrollment.EnrollmentAdminDtos.TrialRow;
import com.hanmo.calligraphy.common.ApiResponse;
import com.hanmo.calligraphy.common.PageResult;
import com.hanmo.calligraphy.security.LoginUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 报名与缴费后台接口层：暴露报名分页、后台新建报名、确认、缴费入班、取消，以及试听跟进与缴费记录查询。
 *
 * <p>路由前缀 /api/admin/enrollments。本类只做参数接收与 ApiResponse 包装，业务规则与事务
 * 全部在 EnrollmentAdminService；操作人由 @AuthenticationPrincipal 注入，用于审计留痕。</p>
 *
 * <p>依赖表：无直接访问，全部经 service 落到 enrollment / payment_record / hour_account /
 * hour_ledger / class_student / trial_booking。</p>
 */
@Tag(name = "管理端-报名与缴费")
@RestController
@RequestMapping("/api/admin/enrollments")
@RequiredArgsConstructor
public class EnrollmentAdminController {

    private final EnrollmentAdminService service;

    /**
     * 报名申请分页。
     *
     * <p>只读；透传 status 与 keyword（报名单号/学员姓名）。</p>
     *
     * @param current 页码
     * @param size    每页条数（service 侧限制 1~200）
     * @param status  状态过滤，可为空
     * @param keyword 关键字，可为空
     * @return 报名行分页
     */
    @Operation(summary = "报名申请分页")
    @GetMapping("/page")
    public ApiResponse<PageResult<EnrollmentRow>> page(
            @RequestParam(defaultValue = "1") long current,
            @RequestParam(defaultValue = "10") long size,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String keyword) {
        return ApiResponse.ok(service.page(current, size, status, keyword));
    }

    /**
     * 后台新建报名（前台/线下代录），初始状态 PENDING_CONFIRM。
     *
     * <p>副作用：service 内写 enrollment 与审计日志，不开缴费、不充值课时。</p>
     *
     * @throws BusinessException 学员/课程/班级不存在，课程下架、班级满员或重复报名（400/404）
     */
    @Operation(summary = "后台新建报名（前台/线下录入）")
    @PostMapping
    public ApiResponse<EnrollmentRow> create(@RequestBody CreateEnrollmentRequest request,
                                             @AuthenticationPrincipal LoginUser user) {
        return ApiResponse.ok(service.create(request, user));
    }

    /**
     * 确认报名：待确认 → 待缴费，可调整班级与应缴金额。
     *
     * <p>副作用：更新 enrollment 状态并写审计；不充值。</p>
     *
     * @param id      报名 ID
     * @param request 可选的班级/金额调整，允许为空体
     * @throws BusinessException 状态不允许确认、班级不匹配或满员
     */
    @Operation(summary = "确认报名")
    @PostMapping("/{id}/confirm")
    public ApiResponse<EnrollmentRow> confirm(@PathVariable Long id,
                                              @RequestBody(required = false) ConfirmRequest request,
                                              @AuthenticationPrincipal LoginUser operator) {
        return ApiResponse.ok(service.confirm(id, request, operator));
    }

    /**
     * 登记线下缴费并入班：写缴费、置 ENROLLED、充值课时、写课时流水、加入班级。
     *
     * <p>副作用最大且事务化：以上 5 类写库 + 审计日志在同一事务内；已入班重复调用直接返回。</p>
     *
     * @param id      报名 ID
     * @param request 缴费方式、金额、备注
     * @throws BusinessException 未确认即缴费、班级满员、金额非正、缺少缴费方式
     */
    @Operation(summary = "登记线下缴费并入班")
    @PostMapping("/{id}/pay")
    public ApiResponse<EnrollmentRow> pay(@PathVariable Long id,
                                          @RequestBody PayRequest request,
                                          @AuthenticationPrincipal LoginUser operator) {
        return ApiResponse.ok(service.pay(id, request, operator));
    }

    /**
     * 取消报名（仅未入班可取消）。
     *
     * <p>副作用：enrollment 置 CANCELLED 并写审计；不回滚课时、不移出班级。</p>
     *
     * @throws BusinessException 已入班报名不能取消
     */
    @Operation(summary = "取消报名")
    @PostMapping("/{id}/cancel")
    public ApiResponse<EnrollmentRow> cancel(@PathVariable Long id,
                                             @AuthenticationPrincipal LoginUser operator) {
        return ApiResponse.ok(service.cancel(id, operator));
    }

    /**
     * 试听预约分页，供教务跟进。
     *
     * <p>只读；手机号在 service 层已脱敏。</p>
     */
    @Operation(summary = "试听预约分页")
    @GetMapping("/trials/page")
    public ApiResponse<PageResult<TrialRow>> trials(
            @RequestParam(defaultValue = "1") long current,
            @RequestParam(defaultValue = "10") long size,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String keyword) {
        return ApiResponse.ok(service.trialPage(current, size, status, keyword));
    }

    /**
     * 试听跟进：更新状态、跟进备注与下次跟进日期。
     *
     * <p>副作用：更新 trial_booking；无审计、无课时变动。</p>
     *
     * @throws BusinessException 预约不存在（404）
     */
    @Operation(summary = "试听跟进")
    @PostMapping("/trials/{id}/follow-up")
    public ApiResponse<TrialRow> followUp(@PathVariable Long id,
                                          @RequestBody FollowUpRequest request) {
        return ApiResponse.ok(service.followUp(id, request));
    }

    /**
     * 缴费记录分页。
     *
     * <p>只读；keyword 匹配缴费单号或学员姓名，返回登记人与缴费方式。</p>
     */
    @Operation(summary = "缴费记录分页")
    @GetMapping("/payments/page")
    public ApiResponse<PageResult<PaymentRow>> payments(
            @RequestParam(defaultValue = "1") long current,
            @RequestParam(defaultValue = "10") long size,
            @RequestParam(required = false) String keyword) {
        return ApiResponse.ok(service.paymentPage(current, size, keyword));
    }
}
