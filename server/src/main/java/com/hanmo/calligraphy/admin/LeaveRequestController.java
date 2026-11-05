package com.hanmo.calligraphy.admin;

import com.hanmo.calligraphy.common.BaseCrudController;
import com.hanmo.calligraphy.domain.entity.LeaveRequest;
import com.hanmo.calligraphy.domain.mapper.LeaveRequestMapper;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 请假申请记录：leave_request 表的通用维护通道。
 *
 * <p>业务模块：请假审批；数据表 leave_request；接口前缀 {@code /api/admin/leave}。</p>
 * <p>后台页面：管理端「请假审批」(/leave，Leave.vue) —— 页面用 /page 拉列表，审批动作走
 * /api/admin/workflow/leaves/{id}/approve，补课安排走 /api/admin/workflow/leaves/{id}/makeup。</p>
 * <p>权限：该前缀不在 SecurityConfig 的「写操作仅 ADMIN」清单内，GET/POST/PUT/DELETE 均允许 ADMIN 与 ACADEMIC。</p>
 * <p>通道：继承 {@link BaseCrudController}，page/list/{id}/create/update/delete 六个通道全部开放；
 * 该表无可搜索列，分页 keyword 会被忽略；删除为物理删除。</p>
 * <p>风险提示（基于实现）：直接用 PUT 改 status 会绕过审批联动 —— 不写 attendance、不记审计日志，
 * 也不校验是否仍为 PENDING，与 workflow 审批口径不一致。</p>
 */
@Tag(name = "管理端-请假记录")
@RestController
@RequestMapping("/api/admin/leave")
public class LeaveRequestController extends BaseCrudController<LeaveRequest> {
    public LeaveRequestController(LeaveRequestMapper mapper) {
        super(mapper, LeaveRequest.class);
    }
}
