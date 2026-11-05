package com.hanmo.calligraphy.admin;

import com.hanmo.calligraphy.common.BaseCrudController;
import com.hanmo.calligraphy.domain.entity.PaymentMethod;
import com.hanmo.calligraphy.domain.mapper.PaymentMethodMapper;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "管理端-缴费方式")
@RestController
@RequestMapping("/api/admin/payment-methods")
public class PaymentMethodController extends BaseCrudController<PaymentMethod> {
    public PaymentMethodController(PaymentMethodMapper mapper) {
        super(mapper, PaymentMethod.class);
    }
}
