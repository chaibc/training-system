package com.hanmo.calligraphy.admin;

import com.hanmo.calligraphy.common.BaseCrudController;
import com.hanmo.calligraphy.domain.entity.WorkComment;
import com.hanmo.calligraphy.domain.mapper.WorkCommentMapper;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 作品评论：work_comment 表的通用维护通道，管理端主要用于评论审核。
 *
 * <p>业务模块：作品管理；接口前缀 {@code /api/admin/work-comments}。</p>
 * <p>后台页面：管理端「作品管理」(/works，Works.vue) 的评论审核区块 —— 只用到 /page 与 PUT
 * （把 status 改为 APPROVED / HIDDEN / REJECTED）。</p>
 * <p>权限：该前缀不在「写操作仅 ADMIN」清单内，GET/POST/PUT/DELETE 均允许 ADMIN 与 ACADEMIC。</p>
 * <p>通道：继承 {@link BaseCrudController}，六个通道全部开放；该表无可搜索列，分页 keyword 会被忽略；
 * 删除为物理删除。</p>
 * <p>注意：reviewed_by / reviewed_at 不由基类填充，审核留痕需请求体自带，否则为空。</p>
 */
@Tag(name = "管理端-作品评论")
@RestController
@RequestMapping("/api/admin/work-comments")
public class WorkCommentController extends BaseCrudController<WorkComment> {
    public WorkCommentController(WorkCommentMapper mapper) {
        super(mapper, WorkComment.class);
    }
}
