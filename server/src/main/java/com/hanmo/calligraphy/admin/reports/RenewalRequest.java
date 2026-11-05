package com.hanmo.calligraphy.admin.reports;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record RenewalRequest(
        @NotNull(message = "请选择学员") Long studentId,
        @NotNull(message = "请输入续费金额") BigDecimal amount,
        @NotNull(message = "请选择缴费方式") Long paymentMethodId,
        @NotNull(message = "请输入续费课时") @Min(value = 1, message = "续费课时必须大于 0") Integer lessons,
        String item,
        String note
) {}
