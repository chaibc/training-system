package com.hanmo.calligraphy.admin;

import com.hanmo.calligraphy.common.BaseCrudController;
import com.hanmo.calligraphy.domain.entity.TrialBooking;
import com.hanmo.calligraphy.domain.mapper.TrialBookingMapper;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 试听预约线索：trial_booking 表的查询通道（写入已关闭）。
 *
 * <p>业务模块：报名与缴费（试听线索跟进）；接口前缀 {@code /api/admin/trials}。</p>
 * <p>后台页面：没有直接页面 —— 「报名与缴费」页的试听列表走 /api/admin/enrollments/trials/page，
 * 跟进走 /api/admin/enrollments/trials/{id}/follow-up。</p>
 * <p>权限：该前缀不在「写操作仅 ADMIN」清单内，GET 允许 ADMIN 与 ACADEMIC（POST/PUT/DELETE 已关闭，返回 405）。</p>
 * <p>通道：继承 {@link BaseCrudController}，只开放 page / list / {id} 查询通道；create / update / delete 已关闭（见 {@link #writeChannelsEnabled()}）；分页 keyword 只命中 student_name 列 ——
 * 基类按 name/title/studentName 顺序探测，命中 studentName 即返回，同表的 parent_name、phone 不参与搜索。</p>
 * <p>写入约束：通用写通道已关闭，POST / PUT / DELETE 返回 405；写入统一走对应的业务接口，避免绕过授权校验、课时扣减与审批联动。</p>
 */
@Tag(name = "管理端-试听预约")
@RestController
@RequestMapping("/api/admin/trials")
public class TrialBookingController extends BaseCrudController<TrialBooking> {
    public TrialBookingController(TrialBookingMapper mapper) {
        super(mapper, TrialBooking.class);
    }

    /**
     * 关闭通用写通道：试听线索必须经小程序预约与后台跟进接口写入。
     *
     * <p>保留 page / list / {id} 查询；写入请走 {@code /api/admin/enrollments/trials/{id}/follow-up}。</p>
     *
     * @return false，POST / PUT / DELETE 统一返回 405
     */
    @Override
    protected boolean writeChannelsEnabled() {
        return false;
    }
}
