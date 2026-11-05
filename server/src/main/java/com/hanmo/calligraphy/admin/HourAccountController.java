package com.hanmo.calligraphy.admin;

import com.hanmo.calligraphy.common.BaseCrudController;
import com.hanmo.calligraphy.domain.entity.HourAccount;
import com.hanmo.calligraphy.domain.mapper.HourAccountMapper;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 课时账户维护：hour_account 表（totalHours / usedHours / remainingHours / validUntil）的通用 CRUD 通道。
 *
 * <p>业务模块：课时与消课；接口前缀 {@code /api/admin/hour-accounts}。</p>
 * <p>后台页面：没有独立页面 —— admin-web 不调用该前缀。学员课时余额由 /api/admin/students/{id} 详情带出，
 * 充值走 /api/admin/reports/renew 或学员 adjust-hours，扣课走 /api/admin/workflow/** 点名。</p>
 * <p>权限：该前缀不在「写操作仅 ADMIN」清单内，GET/POST/PUT/DELETE 均允许 ADMIN 与 ACADEMIC。</p>
 * <p>通道：继承 {@link BaseCrudController}，六个通道全部开放；该表无可搜索列，分页 keyword 会被忽略；
 * 删除为物理删除。</p>
 * <p>风险提示（基于实现）：PUT 直接改 remaining_hours 不会写 hour_ledger，账户余额与流水会对不上；
 * 余额变更应走 adjust-hours / renew / rollcall 等业务接口。</p>
 */
@Tag(name = "管理端-课时账户")
@RestController
@RequestMapping("/api/admin/hour-accounts")
public class HourAccountController extends BaseCrudController<HourAccount> {
    public HourAccountController(HourAccountMapper mapper) {
        super(mapper, HourAccount.class);
    }
}
