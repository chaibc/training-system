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

/**
 * 统计报表接口：报表聚合查询与「登记续费」。
 *
 * <p>路由前缀 /api/admin/reports。只做参数接收与 ApiResponse 包装，统计口径与写库事务
 * 都在 AdminReportService；续费登记需要操作人，用于缴费登记人与审计。</p>
 * 落库表：enrollment、payment_record、attendance、hour_account、hour_ledger、business_rule、sys_audit_log（经 service 读取/写入）
 */
@Tag(name = "后台-统计报表")
@RestController
@RequestMapping("/api/admin/reports")
@RequiredArgsConstructor
public class AdminReportController {

    private final AdminReportService reportService;

    /**
     * 统计报表聚合数据。
     *
     * <p>只读；months 只认 1/6/12 三档，其它值由 service 收敛。</p>
     *
     * @param months 统计区间月数，缺省 6
     * @return 指标、趋势、收入构成、教师/班级统计与续费预警
     */
    @Operation(summary = "统计报表聚合")
    @GetMapping("/overview")
    public ApiResponse<Map<String, Object>> overview(@RequestParam(defaultValue = "6") int months) {
        return ApiResponse.ok(reportService.overview(months));
    }

    /**
     * 登记线下续费并立即充值课时。
     *
     * <p>副作用：写缴费记录、课时账户、课时流水与审计日志（同一事务）；无幂等保护，
     * 重复提交会重复充值。</p>
     *
     * @param request 学员、金额、缴费方式、课时，@Valid 校验必填与下限
     * @param user    当前后台操作人
     * @throws BusinessException 学员/缴费方式不存在、金额或课时非正
     */
    @Operation(summary = "登记线下续费并充值课时")
    @PostMapping("/renew")
    public ApiResponse<Map<String, Object>> renew(@Valid @RequestBody RenewalRequest request,
                                                  @AuthenticationPrincipal LoginUser user) {
        return ApiResponse.ok(reportService.renew(request, user));
    }
}
