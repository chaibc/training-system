package com.hanmo.calligraphy.admin;

import com.hanmo.calligraphy.common.BaseCrudController;
import com.hanmo.calligraphy.domain.entity.HourLedger;
import com.hanmo.calligraphy.domain.mapper.HourLedgerMapper;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 课时流水：hour_ledger 表（type、changeHours、balanceAfter、operatorId、reason）的通用 CRUD 通道。
 *
 * <p>业务模块：课时与消课；接口前缀 {@code /api/admin/hour-ledger}。</p>
 * <p>后台页面：没有独立页面 —— admin-web 不调用该前缀；学员详情里的课时流水由 students 接口一并返回。</p>
 * <p>权限：该前缀不在「写操作仅 ADMIN」清单内，GET/POST/PUT/DELETE 均允许 ADMIN 与 ACADEMIC。</p>
 * <p>通道：继承 {@link BaseCrudController}，六个通道全部开放；该表无可搜索列，分页 keyword 会被忽略；
 * 删除为物理删除。</p>
 * <p>风险提示（基于实现）：流水按业务口径应为只追加，但基类同时开放了 PUT 与 DELETE，
 * 历史流水可以被改写或删除而不留痕。</p>
 */
@Tag(name = "管理端-课时流水")
@RestController
@RequestMapping("/api/admin/hour-ledger")
public class HourLedgerController extends BaseCrudController<HourLedger> {
    public HourLedgerController(HourLedgerMapper mapper) {
        super(mapper, HourLedger.class);
    }
}
