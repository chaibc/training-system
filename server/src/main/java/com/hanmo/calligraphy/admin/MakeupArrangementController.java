package com.hanmo.calligraphy.admin;

import com.hanmo.calligraphy.common.BaseCrudController;
import com.hanmo.calligraphy.domain.entity.MakeupArrangement;
import com.hanmo.calligraphy.domain.mapper.MakeupArrangementMapper;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 补课安排记录：makeup_arrangement 表（请假单、学员、补课课次、状态、安排人）的查询通道（写入已关闭）。
 *
 * <p>业务模块：请假审批；接口前缀 {@code /api/admin/makeup-arrangements}。</p>
 * <p>后台页面：没有独立页面 —— 补课由「请假审批」页调用 /api/admin/workflow/leaves/{id}/makeup 创建。</p>
 * <p>权限：该前缀不在「写操作仅 ADMIN」清单内，GET 允许 ADMIN 与 ACADEMIC（POST/PUT/DELETE 已关闭，返回 405）。</p>
 * <p>通道：继承 {@link BaseCrudController}，只开放 page / list / {id} 查询通道；create / update / delete 已关闭（见 {@link #writeChannelsEnabled()}）；该表无可搜索列，分页 keyword 会被忽略。</p>
 * <p>写入约束：通用写通道已关闭，POST / PUT / DELETE 返回 405；写入统一走对应的业务接口，避免绕过授权校验、课时扣减与审批联动。</p>
 */
@Tag(name = "管理端-补课安排")
@RestController
@RequestMapping("/api/admin/makeup-arrangements")
public class MakeupArrangementController extends BaseCrudController<MakeupArrangement> {
    public MakeupArrangementController(MakeupArrangementMapper mapper) {
        super(mapper, MakeupArrangement.class);
    }

    /**
     * 关闭通用写通道：补课必须经安排接口写入（校验请假 APPROVED、防止重复安排）。
     *
     * <p>保留 page / list / {id} 查询；写入请走 {@code /api/admin/workflow/leaves/{id}/makeup}。</p>
     *
     * @return false，POST / PUT / DELETE 统一返回 405
     */
    @Override
    protected boolean writeChannelsEnabled() {
        return false;
    }
}
