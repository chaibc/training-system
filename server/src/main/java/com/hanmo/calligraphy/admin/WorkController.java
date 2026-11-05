package com.hanmo.calligraphy.admin;

import com.hanmo.calligraphy.common.BaseCrudController;
import com.hanmo.calligraphy.domain.entity.Work;
import com.hanmo.calligraphy.domain.mapper.WorkMapper;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 作品管理：work 表的通用 CRUD 通道。
 *
 * <p>业务模块：作品管理；接口前缀 {@code /api/admin/works}。</p>
 * <p>后台页面：管理端「作品管理」(/works，Works.vue)。上传作品走 /api/admin/works/upload
 * （创建前校验家长 ACTIVE 授权、作品置 PENDING），审核上下架走 /api/admin/works/{id}/review，
 * 这两个接口在 admin.work 子包中，与本控制器共用同一前缀。</p>
 * <p>权限：该前缀不在「写操作仅 ADMIN」清单内，GET/POST/PUT/DELETE 均允许 ADMIN 与 ACADEMIC。</p>
 * <p>通道：继承 {@link BaseCrudController}，六个通道全部开放；分页 keyword 命中 title 列；
 * 删除为逻辑删除（work.deleted）。</p>
 * <p>风险提示（基于实现）：POST/PUT 直接落库不校验家长授权，可以把未授权作品改成 DISPLAYED，
 * 绕过 review 接口的授权复核。</p>
 */
@Tag(name = "管理端-作品")
@RestController
@RequestMapping("/api/admin/works")
public class WorkController extends BaseCrudController<Work> {
    public WorkController(WorkMapper mapper) {
        super(mapper, Work.class);
    }
}
