package com.hanmo.calligraphy.admin;

import com.hanmo.calligraphy.common.BaseCrudController;
import com.hanmo.calligraphy.domain.entity.TrialBooking;
import com.hanmo.calligraphy.domain.mapper.TrialBookingMapper;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 试听预约线索：trial_booking 表的通用维护通道。
 *
 * <p>业务模块：报名与缴费（试听线索跟进）；接口前缀 {@code /api/admin/trials}。</p>
 * <p>后台页面：没有直接页面 —— 「报名与缴费」页的试听列表走 /api/admin/enrollments/trials/page，
 * 跟进走 /api/admin/enrollments/trials/{id}/follow-up。</p>
 * <p>权限：该前缀不在「写操作仅 ADMIN」清单内，GET/POST/PUT/DELETE 均允许 ADMIN 与 ACADEMIC。</p>
 * <p>通道：继承 {@link BaseCrudController}，六个通道全部开放；分页 keyword 只命中 student_name 列 ——
 * 基类按 name/title/studentName 顺序探测，命中 studentName 即返回，同表的 parent_name、phone 不参与搜索；
 * 删除为物理删除。</p>
 * <p>风险提示（基于实现）：直改 status / followUpNote 不走跟进口径，也不校验状态流转，
 * enrolled 线索的跟进记录会与 workflow 之外的入口不一致。</p>
 */
@Tag(name = "管理端-试听预约")
@RestController
@RequestMapping("/api/admin/trials")
public class TrialBookingController extends BaseCrudController<TrialBooking> {
    public TrialBookingController(TrialBookingMapper mapper) {
        super(mapper, TrialBooking.class);
    }
}
