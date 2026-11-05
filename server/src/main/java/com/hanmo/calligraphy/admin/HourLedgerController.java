package com.hanmo.calligraphy.admin;

import com.hanmo.calligraphy.common.BaseCrudController;
import com.hanmo.calligraphy.domain.entity.HourLedger;
import com.hanmo.calligraphy.domain.mapper.HourLedgerMapper;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 课时流水：hour_ledger 表（type、changeHours、balanceAfter、operatorId、reason）的查询通道（写入已关闭）。
 *
 * <p>业务模块：课时与消课；接口前缀 {@code /api/admin/hour-ledger}。</p>
 * <p>后台页面：没有独立页面 —— admin-web 不调用该前缀；学员详情里的课时流水由 students 接口一并返回。</p>
 * <p>权限：该前缀不在「写操作仅 ADMIN」清单内，GET 允许 ADMIN 与 ACADEMIC（POST/PUT/DELETE 已关闭，返回 405）。</p>
 * <p>通道：继承 {@link BaseCrudController}，只开放 page / list / {id} 查询通道；create / update / delete 已关闭（见 {@link #writeChannelsEnabled()}）；该表无可搜索列，分页 keyword 会被忽略。</p>
 * <p>写入约束：通用写通道已关闭，POST / PUT / DELETE 返回 405；写入统一走对应的业务接口，避免绕过授权校验、课时扣减与审批联动。</p>
 */
@Tag(name = "管理端-课时流水")
@RestController
@RequestMapping("/api/admin/hour-ledger")
public class HourLedgerController extends BaseCrudController<HourLedger> {
    public HourLedgerController(HourLedgerMapper mapper) {
        super(mapper, HourLedger.class);
    }

    /**
     * 关闭通用写通道：课时流水是账本，只允许查询与追加，不允许改删。
     *
     * <p>保留 page / list / {id} 查询；写入请走 无写入接口（仅由业务侧追加流水）。</p>
     *
     * @return false，POST / PUT / DELETE 统一返回 405
     */
    @Override
    protected boolean writeChannelsEnabled() {
        return false;
    }
}
