package com.hanmo.calligraphy.admin;

import com.hanmo.calligraphy.common.BaseCrudController;
import com.hanmo.calligraphy.domain.entity.PaymentMethod;
import com.hanmo.calligraphy.domain.mapper.PaymentMethodMapper;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 缴费方式字典：payment_method 表（名称、说明、排序、启停）的增删改查。
 *
 * <p>业务模块：基础数据字典；接口前缀 {@code /api/admin/payment-methods}。</p>
 * <p>后台页面：管理端「基础数据」(/basic，BasicData.vue) 的缴费方式页签；「报名与缴费」与「统计报表」页的
 * 缴费方式下拉同样取自这里。</p>
 * <p>权限：GET 允许 ADMIN 与 ACADEMIC；POST/PUT/DELETE 由 SecurityConfig 限定仅 ADMIN。</p>
 * <p>通道：继承 {@link BaseCrudController}，六个通道全部开放；分页 keyword 命中 name 列；
 * 删除为物理删除，不校验 payment_record.payment_method_id 的引用。</p>
 */
@Tag(name = "管理端-缴费方式")
@RestController
@RequestMapping("/api/admin/payment-methods")
public class PaymentMethodController extends BaseCrudController<PaymentMethod> {
    public PaymentMethodController(PaymentMethodMapper mapper) {
        super(mapper, PaymentMethod.class);
    }
}
