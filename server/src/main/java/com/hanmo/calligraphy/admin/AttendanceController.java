package com.hanmo.calligraphy.admin;

import com.hanmo.calligraphy.common.BaseCrudController;
import com.hanmo.calligraphy.domain.entity.Attendance;
import com.hanmo.calligraphy.domain.mapper.AttendanceMapper;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 上课记录（点名结果）：attendance 表的通用维护通道。
 *
 * <p>业务模块：点名与消课；数据表 attendance；接口前缀 {@code /api/admin/attendance-records}。</p>
 * <p>后台页面：管理端「点名与消课」(/attendance，Attendance.vue) —— 页面用 /list 全量拉取记录，
 * 既算顶部出勤/消课统计卡片，也渲染下方记录表格；点名名单与点名动作走 admin.workflow 的
 * /api/admin/workflow/lessons/{id}/roster 与 /rollcall。</p>
 * <p>权限：该前缀不在 SecurityConfig 的「写操作仅 ADMIN」清单内，GET/POST/PUT/DELETE 均允许 ADMIN 与 ACADEMIC。</p>
 * <p>通道：继承 {@link BaseCrudController}，page/list/{id}/create/update/delete 六个通道全部开放；
 * attendance 没有 name/title 等候选列，分页 keyword 会被忽略；删除为物理删除。</p>
 * <p>风险提示（基于实现）：本通道直接写表，不扣减课时账户、不写 hour_ledger，也不校验课次状态与学员是否在班，
 * 与点名消课口径不一致，仅建议用于查询与数据修正。</p>
 */
@Tag(name = "管理端-上课记录")
@RestController
@RequestMapping("/api/admin/attendance-records")
public class AttendanceController extends BaseCrudController<Attendance> {
    public AttendanceController(AttendanceMapper mapper) {
        super(mapper, Attendance.class);
    }
}
