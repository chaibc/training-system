package com.hanmo.calligraphy.admin;

import com.hanmo.calligraphy.common.BaseCrudController;
import com.hanmo.calligraphy.domain.entity.PaymentRecord;
import com.hanmo.calligraphy.domain.mapper.PaymentRecordMapper;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "管理端-缴费记录")
@RestController
@RequestMapping("/api/admin/payments")
public class PaymentRecordController extends BaseCrudController<PaymentRecord> {
    public PaymentRecordController(PaymentRecordMapper mapper) {
        super(mapper, PaymentRecord.class);
    }
}
