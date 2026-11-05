package com.hanmo.calligraphy.admin;

import com.hanmo.calligraphy.common.BaseCrudController;
import com.hanmo.calligraphy.domain.entity.WorkAuth;
import com.hanmo.calligraphy.domain.mapper.WorkAuthMapper;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 家长作品授权：work_auth 表的通用维护通道。
 *
 * <p>业务模块：作品管理；接口前缀 {@code /api/admin/work-auths}。</p>
 * <p>后台页面：管理端「作品管理」(/works，Works.vue) 的授权区块 —— 用 /page 查、用 PUT 改；
 * 补录授权走 /api/admin/work-auths/record，撤回走 /api/admin/work-auths/{id}/revoke（二者在 admin.work 子包）。</p>
 * <p>权限：该前缀不在「写操作仅 ADMIN」清单内，GET/POST/PUT/DELETE 均允许 ADMIN 与 ACADEMIC。</p>
 * <p>通道：继承 {@link BaseCrudController}，六个通道全部开放；该表无可搜索列，分页 keyword 会被忽略；
 * 删除为物理删除。</p>
 * <p>风险提示（基于实现）：用 PUT 把 status 改成 REVOKED 不会联动下架该学员已展示作品，
 * 也不刷新作品墙 —— 撤回授权请走 revoke 接口。</p>
 */
@Tag(name = "管理端-作品授权")
@RestController
@RequestMapping("/api/admin/work-auths")
public class WorkAuthController extends BaseCrudController<WorkAuth> {
    public WorkAuthController(WorkAuthMapper mapper) {
        super(mapper, WorkAuth.class);
    }
}
