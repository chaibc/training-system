package com.hanmo.calligraphy.admin;

import com.hanmo.calligraphy.common.BaseCrudController;
import com.hanmo.calligraphy.domain.entity.WorkComment;
import com.hanmo.calligraphy.domain.mapper.WorkCommentMapper;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "管理端-作品评论")
@RestController
@RequestMapping("/api/admin/work-comments")
public class WorkCommentController extends BaseCrudController<WorkComment> {
    public WorkCommentController(WorkCommentMapper mapper) {
        super(mapper, WorkComment.class);
    }
}
