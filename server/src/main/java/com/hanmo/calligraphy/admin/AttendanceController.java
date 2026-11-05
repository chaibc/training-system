package com.hanmo.calligraphy.admin;

import com.hanmo.calligraphy.common.BaseCrudController;
import com.hanmo.calligraphy.domain.entity.Attendance;
import com.hanmo.calligraphy.domain.mapper.AttendanceMapper;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 上课记录（点名结果）：attendance 表的查询通道（写入已关闭）。
 *
 * <p>业务模块：点名与消课；数据表 attendance；接口前缀 {@code /api/admin/attendance-records}。</p>
 * <p>后台页面：管理端「点名与消课」(/attendance，Attendance.vue) —— 页面用 /list 全量拉取记录，
 * 既算顶部出勤/消课统计卡片，也渲染下方记录表格；点名名单与点名动作走 admin.workflow 的
 * /api/admin/workflow/lessons/{id}/roster 与 /rollcall。</p>
 * <p>权限：该前缀不在 SecurityConfig 的「写操作仅 ADMIN」清单内，GET 允许 ADMIN 与 ACADEMIC（POST/PUT/DELETE 已关闭，返回 405）。</p>
 * <p>通道：继承 {@link BaseCrudController}，只开放 page / list / {id} 查询通道；create / update / delete 已关闭（见 {@link #writeChannelsEnabled()}）；
 * attendance 没有 name/title 等候选列，分页 keyword 会被忽略。</p>
 * <p>写入约束：通用写通道已关闭，POST / PUT / DELETE 返回 405；写入统一走对应的业务接口，避免绕过授权校验、课时扣减与审批联动。</p>
 */
@Tag(name = "管理端-上课记录")
@RestController
@RequestMapping("/api/admin/attendance-records")
public class AttendanceController extends BaseCrudController<Attendance> {
    public AttendanceController(AttendanceMapper mapper) {
        super(mapper, Attendance.class);
    }

    /**
     * 关闭通用写通道：点名记录必须由点名动作写入（按差额消课、幂等、同步写 hour_ledger）。
     *
     * <p>保留 page / list / {id} 查询；写入请走 {@code /api/admin/workflow/lessons/{id}/rollcall}。</p>
     *
     * @return false，POST / PUT / DELETE 统一返回 405
     */
    @Override
    protected boolean writeChannelsEnabled() {
        return false;
    }
}
