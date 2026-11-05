package com.hanmo.calligraphy.admin;

import com.hanmo.calligraphy.common.BaseCrudController;
import com.hanmo.calligraphy.domain.entity.MakeupArrangement;
import com.hanmo.calligraphy.domain.mapper.MakeupArrangementMapper;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 补课安排记录：makeup_arrangement 表（请假单、学员、补课课次、状态、安排人）的通用维护通道。
 *
 * <p>业务模块：请假审批；接口前缀 {@code /api/admin/makeup-arrangements}。</p>
 * <p>后台页面：没有独立页面 —— 补课由「请假审批」页调用 /api/admin/workflow/leaves/{id}/makeup 创建。</p>
 * <p>权限：该前缀不在「写操作仅 ADMIN」清单内，GET/POST/PUT/DELETE 均允许 ADMIN 与 ACADEMIC。</p>
 * <p>通道：继承 {@link BaseCrudController}，六个通道全部开放；该表无可搜索列，分页 keyword 会被忽略；
 * 删除为物理删除。</p>
 * <p>风险提示（基于实现）：本通道直接插表，不校验请假单是否 APPROVED、不防重复安排，
 * 也不回写 leave_request.makeup_status（只有 workflow 的 makeup 接口会做这两件事）。</p>
 */
@Tag(name = "管理端-补课安排")
@RestController
@RequestMapping("/api/admin/makeup-arrangements")
public class MakeupArrangementController extends BaseCrudController<MakeupArrangement> {
    public MakeupArrangementController(MakeupArrangementMapper mapper) {
        super(mapper, MakeupArrangement.class);
    }
}
