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

@Tag(name = "管理端-报名与缴费")
@RestController
@RequestMapping("/api/admin/enrollments")
@RequiredArgsConstructor
public class EnrollmentAdminController {

    private final EnrollmentAdminService service;

    @Operation(summary = "报名申请分页")
    @GetMapping("/page")
    public ApiResponse<PageResult<EnrollmentRow>> page(
            @RequestParam(defaultValue = "1") long current,
            @RequestParam(defaultValue = "10") long size,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String keyword) {
        return ApiResponse.ok(service.page(current, size, status, keyword));
    }

    @Operation(summary = "后台新建报名（前台/线下录入）")
    @PostMapping
    public ApiResponse<EnrollmentRow> create(@RequestBody CreateEnrollmentRequest request,
                                             @AuthenticationPrincipal LoginUser user) {
        return ApiResponse.ok(service.create(request, user));
    }

    @Operation(summary = "确认报名")
    @PostMapping("/{id}/confirm")
    public ApiResponse<EnrollmentRow> confirm(@PathVariable Long id,
                                              @RequestBody(required = false) ConfirmRequest request,
                                              @AuthenticationPrincipal LoginUser operator) {
        return ApiResponse.ok(service.confirm(id, request, operator));
    }

    @Operation(summary = "登记线下缴费并入班")
    @PostMapping("/{id}/pay")
    public ApiResponse<EnrollmentRow> pay(@PathVariable Long id,
                                          @RequestBody PayRequest request,
                                          @AuthenticationPrincipal LoginUser operator) {
        return ApiResponse.ok(service.pay(id, request, operator));
    }

    @Operation(summary = "取消报名")
    @PostMapping("/{id}/cancel")
    public ApiResponse<EnrollmentRow> cancel(@PathVariable Long id,
                                             @AuthenticationPrincipal LoginUser operator) {
        return ApiResponse.ok(service.cancel(id, operator));
    }

    @Operation(summary = "试听预约分页")
    @GetMapping("/trials/page")
    public ApiResponse<PageResult<TrialRow>> trials(
            @RequestParam(defaultValue = "1") long current,
            @RequestParam(defaultValue = "10") long size,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String keyword) {
        return ApiResponse.ok(service.trialPage(current, size, status, keyword));
    }

    @Operation(summary = "试听跟进")
    @PostMapping("/trials/{id}/follow-up")
    public ApiResponse<TrialRow> followUp(@PathVariable Long id,
                                          @RequestBody FollowUpRequest request) {
        return ApiResponse.ok(service.followUp(id, request));
    }

    @Operation(summary = "缴费记录分页")
    @GetMapping("/payments/page")
    public ApiResponse<PageResult<PaymentRow>> payments(
            @RequestParam(defaultValue = "1") long current,
            @RequestParam(defaultValue = "10") long size,
            @RequestParam(required = false) String keyword) {
        return ApiResponse.ok(service.paymentPage(current, size, keyword));
    }
}
