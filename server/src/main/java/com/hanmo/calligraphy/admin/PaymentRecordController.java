package com.hanmo.calligraphy.admin;

import com.hanmo.calligraphy.common.BaseCrudController;
import com.hanmo.calligraphy.domain.entity.PaymentRecord;
import com.hanmo.calligraphy.domain.mapper.PaymentRecordMapper;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 缴费记录：payment_record 表的通用维护通道。
 *
 * <p>业务模块：报名与缴费；接口前缀 {@code /api/admin/payments}。</p>
 * <p>后台页面：没有直接页面 —— admin-web 的缴费列表走 /api/admin/enrollments/payments/page；
 * 缴费记录的正式来源是 /api/admin/enrollments/{id}/pay 与 /api/admin/reports/renew。</p>
 * <p>权限：该前缀不在「写操作仅 ADMIN」清单内，GET/POST/PUT/DELETE 均允许 ADMIN 与 ACADEMIC。</p>
 * <p>通道：继承 {@link BaseCrudController}，六个通道全部开放；该表没有 name/title 等候选列
 * （paymentNo 不在基类候选字段内），分页 keyword 会被忽略；删除为物理删除。</p>
 * <p>风险提示（基于实现）：直写本表不会改报名状态、不充课时、也不写 hour_ledger，与缴费业务口径脱节。</p>
 */
@Tag(name = "管理端-缴费记录")
@RestController
@RequestMapping("/api/admin/payments")
public class PaymentRecordController extends BaseCrudController<PaymentRecord> {
    public PaymentRecordController(PaymentRecordMapper mapper) {
        super(mapper, PaymentRecord.class);
    }
}
