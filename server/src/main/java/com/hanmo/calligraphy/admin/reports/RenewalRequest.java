package com.hanmo.calligraphy.admin.reports;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

/**
 * 登记续费请求体。
 *
 * <p>studentId / amount / paymentMethodId / lessons 必填，lessons 至少 1；
 * item 为空时 service 生成「续费 N 课时」，note 原样存入缴费记录。
 */
public record RenewalRequest(
        @NotNull(message = "请选择学员") Long studentId,
        @NotNull(message = "请输入续费金额") BigDecimal amount,
        @NotNull(message = "请选择缴费方式") Long paymentMethodId,
        @NotNull(message = "请输入续费课时") @Min(value = 1, message = "续费课时必须大于 0") Integer lessons,
        String item,
        String note
) {}
