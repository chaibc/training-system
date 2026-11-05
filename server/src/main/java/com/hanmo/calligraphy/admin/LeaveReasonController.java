package com.hanmo.calligraphy.admin;

import com.hanmo.calligraphy.common.BaseCrudController;
import com.hanmo.calligraphy.domain.entity.LeaveReason;
import com.hanmo.calligraphy.domain.mapper.LeaveReasonMapper;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 请假原因字典：leave_reason 表（名称、说明、排序、启停）的增删改查。
 *
 * <p>业务模块：基础数据字典；接口前缀 {@code /api/admin/leave-reasons}。</p>
 * <p>后台页面：管理端「基础数据」(/basic，BasicData.vue) 的请假原因页签；家长端请假原因下拉也来自该表。</p>
 * <p>权限：GET 允许 ADMIN 与 ACADEMIC；POST/PUT/DELETE 由 SecurityConfig 限定仅 ADMIN。</p>
 * <p>通道：继承 {@link BaseCrudController}，六个通道全部开放；分页 keyword 命中 name 列；
 * 删除为物理删除，不校验 leave_request.reason_id 的引用。</p>
 */
@Tag(name = "管理端-请假原因")
@RestController
@RequestMapping("/api/admin/leave-reasons")
public class LeaveReasonController extends BaseCrudController<LeaveReason> {
    public LeaveReasonController(LeaveReasonMapper mapper) {
        super(mapper, LeaveReason.class);
    }
}
