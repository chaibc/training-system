package com.hanmo.calligraphy.admin.reports;

import com.hanmo.calligraphy.common.ApiResponse;
import com.hanmo.calligraphy.security.LoginUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@Tag(name = "后台-统计报表")
@RestController
@RequestMapping("/api/admin/reports")
@RequiredArgsConstructor
public class AdminReportController {

    private final AdminReportService reportService;

    @Operation(summary = "统计报表聚合")
    @GetMapping("/overview")
    public ApiResponse<Map<String, Object>> overview(@RequestParam(defaultValue = "6") int months) {
        return ApiResponse.ok(reportService.overview(months));
    }

    @Operation(summary = "登记线下续费并充值课时")
    @PostMapping("/renew")
    public ApiResponse<Map<String, Object>> renew(@Valid @RequestBody RenewalRequest request,
                                                  @AuthenticationPrincipal LoginUser user) {
        return ApiResponse.ok(reportService.renew(request, user));
    }
}
