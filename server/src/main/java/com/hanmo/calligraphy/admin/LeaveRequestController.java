package com.hanmo.calligraphy.admin;

import com.hanmo.calligraphy.common.BaseCrudController;
import com.hanmo.calligraphy.domain.entity.LeaveRequest;
import com.hanmo.calligraphy.domain.mapper.LeaveRequestMapper;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 请假申请记录：leave_request 表的查询通道（写入已关闭）。
 *
 * <p>业务模块：请假审批；数据表 leave_request；接口前缀 {@code /api/admin/leave}。</p>
 * <p>后台页面：管理端「请假审批」(/leave，Leave.vue) —— 页面用 /page 拉列表，审批动作走
 * /api/admin/workflow/leaves/{id}/approve，补课安排走 /api/admin/workflow/leaves/{id}/makeup。</p>
 * <p>权限：该前缀不在 SecurityConfig 的「写操作仅 ADMIN」清单内，GET 允许 ADMIN 与 ACADEMIC（POST/PUT/DELETE 已关闭，返回 405）。</p>
 * <p>通道：继承 {@link BaseCrudController}，只开放 page / list / {id} 查询通道；create / update / delete 已关闭（见 {@link #writeChannelsEnabled()}）；
 * 该表无可搜索列，分页 keyword 会被忽略。</p>
 * <p>写入约束：通用写通道已关闭，POST / PUT / DELETE 返回 405；写入统一走对应的业务接口，避免绕过授权校验、课时扣减与审批联动。</p>
 */
@Tag(name = "管理端-请假记录")
@RestController
@RequestMapping("/api/admin/leave")
public class LeaveRequestController extends BaseCrudController<LeaveRequest> {
    public LeaveRequestController(LeaveRequestMapper mapper) {
        super(mapper, LeaveRequest.class);
    }

    /**
     * 关闭通用写通道：请假审批必须走审批接口（联动出勤记录与课时扣减）。
     *
     * <p>保留 page / list / {id} 查询；写入请走 {@code /api/admin/workflow/leaves/{id}/approve}。</p>
     *
     * @return false，POST / PUT / DELETE 统一返回 405
     */
    @Override
    protected boolean writeChannelsEnabled() {
        return false;
    }
}
