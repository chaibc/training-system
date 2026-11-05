package com.hanmo.calligraphy.admin;

import com.hanmo.calligraphy.common.BaseCrudController;
import com.hanmo.calligraphy.domain.entity.Qualification;
import com.hanmo.calligraphy.domain.mapper.QualificationMapper;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 资质公示管理：qualification 表（名称、证书编号、有效期、核验状态、是否公示）的增删改查。
 *
 * <p>业务模块：机构内容；接口前缀 {@code /api/admin/qualifications}。</p>
 * <p>后台页面：管理端「机构内容」(/org，Org.vue) 的资质区块 —— 显隐切换即改 visible。</p>
 * <p>权限：GET 允许 ADMIN 与 ACADEMIC；POST/PUT/DELETE 由 SecurityConfig 限定仅 ADMIN。</p>
 * <p>通道：继承 {@link BaseCrudController}，六个通道全部开放；分页 keyword 命中 name 列；删除为物理删除。</p>
 */
@Tag(name = "管理端-资质")
@RestController
@RequestMapping("/api/admin/qualifications")
public class QualificationController extends BaseCrudController<Qualification> {
    public QualificationController(QualificationMapper mapper) {
        super(mapper, Qualification.class);
    }
}
