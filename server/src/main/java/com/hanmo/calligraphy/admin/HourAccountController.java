package com.hanmo.calligraphy.admin;

import com.hanmo.calligraphy.common.BaseCrudController;
import com.hanmo.calligraphy.domain.entity.HourAccount;
import com.hanmo.calligraphy.domain.mapper.HourAccountMapper;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 课时账户维护：hour_account 表（totalHours / usedHours / remainingHours / validUntil）的查询通道（写入已关闭）。
 *
 * <p>业务模块：课时与消课；接口前缀 {@code /api/admin/hour-accounts}。</p>
 * <p>后台页面：没有独立页面 —— admin-web 不调用该前缀。学员课时余额由 /api/admin/students/{id} 详情带出，
 * 充值走 /api/admin/reports/renew 或学员 adjust-hours，扣课走 /api/admin/workflow/** 点名。</p>
 * <p>权限：该前缀不在「写操作仅 ADMIN」清单内，GET 允许 ADMIN 与 ACADEMIC（POST/PUT/DELETE 已关闭，返回 405）。</p>
 * <p>通道：继承 {@link BaseCrudController}，只开放 page / list / {id} 查询通道；create / update / delete 已关闭（见 {@link #writeChannelsEnabled()}）；该表无可搜索列，分页 keyword 会被忽略。</p>
 * <p>写入约束：通用写通道已关闭，POST / PUT / DELETE 返回 405；写入统一走对应的业务接口，避免绕过授权校验、课时扣减与审批联动。</p>
 */
@Tag(name = "管理端-课时账户")
@RestController
@RequestMapping("/api/admin/hour-accounts")
public class HourAccountController extends BaseCrudController<HourAccount> {
    public HourAccountController(HourAccountMapper mapper) {
        super(mapper, HourAccount.class);
    }

    /**
     * 关闭通用写通道：课时余额必须由续费、点名消课、课时调整维护（直改会与流水脱节）。
     *
     * <p>保留 page / list / {id} 查询；写入请走 {@code /api/admin/students/{id}/adjust-hours} 等业务接口。</p>
     *
     * @return false，POST / PUT / DELETE 统一返回 405
     */
    @Override
    protected boolean writeChannelsEnabled() {
        return false;
    }
}
