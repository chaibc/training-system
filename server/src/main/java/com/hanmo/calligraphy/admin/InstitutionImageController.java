package com.hanmo.calligraphy.admin;

import com.hanmo.calligraphy.common.BaseCrudController;
import com.hanmo.calligraphy.domain.entity.InstitutionImage;
import com.hanmo.calligraphy.domain.mapper.InstitutionImageMapper;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 机构环境图片管理：institution_image 表（imageUrl、title、sort）的增删改查。
 *
 * <p>业务模块：机构内容；接口前缀 {@code /api/admin/institution-images}。</p>
 * <p>后台页面：管理端「机构内容」(/org，Org.vue) 的环境图区块 —— 先用 /api/upload（bizType=institution）
 * 上传图片拿到 URL，再调本接口保存；前端实际只用到 list/create/update/delete。</p>
 * <p>权限：GET 允许 ADMIN 与 ACADEMIC；POST/PUT/DELETE 由 SecurityConfig 限定仅 ADMIN。</p>
 * <p>通道：继承 {@link BaseCrudController}，六个通道全部开放；分页 keyword 命中 title 列；
 * 删除为物理删除，且不会删除磁盘上的图片文件。</p>
 */
@Tag(name = "管理端-机构图片")
@RestController
@RequestMapping("/api/admin/institution-images")
public class InstitutionImageController extends BaseCrudController<InstitutionImage> {
    public InstitutionImageController(InstitutionImageMapper mapper) {
        super(mapper, InstitutionImage.class);
    }
}
