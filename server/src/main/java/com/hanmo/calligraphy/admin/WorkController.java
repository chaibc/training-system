package com.hanmo.calligraphy.admin;

import com.hanmo.calligraphy.common.BaseCrudController;
import com.hanmo.calligraphy.domain.entity.Work;
import com.hanmo.calligraphy.domain.mapper.WorkMapper;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 作品管理：work 表的查询通道（写入已关闭）。
 *
 * <p>业务模块：作品管理；接口前缀 {@code /api/admin/works}。</p>
 * <p>后台页面：管理端「作品管理」(/works，Works.vue)。上传作品走 /api/admin/works/upload
 * （创建前校验家长 ACTIVE 授权、作品置 PENDING），审核上下架走 /api/admin/works/{id}/review，
 * 这两个接口在 admin.work 子包中，与本控制器共用同一前缀。</p>
 * <p>权限：该前缀不在「写操作仅 ADMIN」清单内，GET 允许 ADMIN 与 ACADEMIC（POST/PUT/DELETE 已关闭，返回 405）。</p>
 * <p>通道：继承 {@link BaseCrudController}，只开放 page / list / {id} 查询通道；create / update / delete 已关闭（见 {@link #writeChannelsEnabled()}）；分页 keyword 命中 title 列。</p>
 * <p>写入约束：通用写通道已关闭，POST / PUT / DELETE 返回 405；写入统一走对应的业务接口，避免绕过授权校验、课时扣减与审批联动。</p>
 */
@Tag(name = "管理端-作品")
@RestController
@RequestMapping("/api/admin/works")
public class WorkController extends BaseCrudController<Work> {
    public WorkController(WorkMapper mapper) {
        super(mapper, Work.class);
    }

    /**
     * 关闭通用写通道：作品必须经上传与审核接口写入（校验家长 ACTIVE 授权与展示状态机）。
     *
     * <p>保留 page / list / {id} 查询；写入请走 {@code /api/admin/works/upload}、{@code /api/admin/works/{id}/review}。</p>
     *
     * @return false，POST / PUT / DELETE 统一返回 405
     */
    @Override
    protected boolean writeChannelsEnabled() {
        return false;
    }
}
