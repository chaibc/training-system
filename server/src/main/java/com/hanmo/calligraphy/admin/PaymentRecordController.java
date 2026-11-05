package com.hanmo.calligraphy.admin;

import com.hanmo.calligraphy.common.BaseCrudController;
import com.hanmo.calligraphy.domain.entity.PaymentRecord;
import com.hanmo.calligraphy.domain.mapper.PaymentRecordMapper;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 缴费记录：payment_record 表的查询通道（写入已关闭）。
 *
 * <p>业务模块：报名与缴费；接口前缀 {@code /api/admin/payments}。</p>
 * <p>后台页面：没有直接页面 —— admin-web 的缴费列表走 /api/admin/enrollments/payments/page；
 * 缴费记录的正式来源是 /api/admin/enrollments/{id}/pay 与 /api/admin/reports/renew。</p>
 * <p>权限：该前缀不在「写操作仅 ADMIN」清单内，GET 允许 ADMIN 与 ACADEMIC（POST/PUT/DELETE 已关闭，返回 405）。</p>
 * <p>通道：继承 {@link BaseCrudController}，只开放 page / list / {id} 查询通道；create / update / delete 已关闭（见 {@link #writeChannelsEnabled()}）；该表没有 name/title 等候选列
 * （paymentNo 不在基类候选字段内），分页 keyword 会被忽略。</p>
 * <p>写入约束：通用写通道已关闭，POST / PUT / DELETE 返回 405；写入统一走对应的业务接口，避免绕过授权校验、课时扣减与审批联动。</p>
 */
@Tag(name = "管理端-缴费记录")
@RestController
@RequestMapping("/api/admin/payments")
public class PaymentRecordController extends BaseCrudController<PaymentRecord> {
    public PaymentRecordController(PaymentRecordMapper mapper) {
        super(mapper, PaymentRecord.class);
    }

    /**
     * 关闭通用写通道：缴费记录必须由报名缴费或续费业务写入（会同步充值课时）。
     *
     * <p>保留 page / list / {id} 查询；写入请走 {@code /api/admin/enrollments/{id}/pay}、{@code /api/admin/reports/renew}。</p>
     *
     * @return false，POST / PUT / DELETE 统一返回 405
     */
    @Override
    protected boolean writeChannelsEnabled() {
        return false;
    }
}
