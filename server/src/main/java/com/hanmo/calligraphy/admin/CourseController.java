package com.hanmo.calligraphy.admin;

import com.hanmo.calligraphy.common.BaseCrudController;
import com.hanmo.calligraphy.domain.entity.Course;
import com.hanmo.calligraphy.domain.mapper.CourseMapper;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "管理端-课程")
@RestController
@RequestMapping("/api/admin/courses")
public class CourseController extends BaseCrudController<Course> {
    public CourseController(CourseMapper mapper) {
        super(mapper, Course.class);
    }
}
