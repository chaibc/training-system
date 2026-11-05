package com.hanmo.calligraphy.admin;

import com.hanmo.calligraphy.common.BaseCrudController;
import com.hanmo.calligraphy.domain.entity.CourseOutline;
import com.hanmo.calligraphy.domain.mapper.CourseOutlineMapper;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "管理端-内容大纲")
@RestController
@RequestMapping("/api/admin/course-outlines")
public class CourseOutlineController extends BaseCrudController<CourseOutline> {
    public CourseOutlineController(CourseOutlineMapper mapper) {
        super(mapper, CourseOutline.class);
    }
}
