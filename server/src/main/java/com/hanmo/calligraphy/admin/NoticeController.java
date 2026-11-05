package com.hanmo.calligraphy.admin;

import com.hanmo.calligraphy.common.BaseCrudController;
import com.hanmo.calligraphy.domain.entity.Notice;
import com.hanmo.calligraphy.domain.mapper.NoticeMapper;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 公告管理：notice 表（标题、类型、正文、发布日期、状态、创建人）的增删改查。
 *
 * <p>业务模块：机构内容；接口前缀 {@code /api/admin/notices}。</p>
 * <p>后台页面：管理端「机构内容」(/org，Org.vue) 的公告区块 —— 发布 / 隐藏即把 status 改成 PUBLISHED / HIDDEN。</p>
 * <p>权限：GET 允许 ADMIN 与 ACADEMIC；POST/PUT/DELETE 由 SecurityConfig 限定仅 ADMIN。</p>
 * <p>通道：继承 {@link BaseCrudController}，六个通道全部开放；分页 keyword 命中 title 列；删除为物理删除。</p>
 * <p>注意：created_by 基类不会自动填充当前登录人，需要请求体显式传入，否则插入为 null。</p>
 */
@Tag(name = "管理端-公告")
@RestController
@RequestMapping("/api/admin/notices")
public class NoticeController extends BaseCrudController<Notice> {
    public NoticeController(NoticeMapper mapper) {
        super(mapper, Notice.class);
    }
}
