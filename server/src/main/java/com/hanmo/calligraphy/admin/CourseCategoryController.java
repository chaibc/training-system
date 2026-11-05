package com.hanmo.calligraphy.admin;

import com.hanmo.calligraphy.common.BaseCrudController;
import com.hanmo.calligraphy.domain.entity.CourseCategory;
import com.hanmo.calligraphy.domain.mapper.CourseCategoryMapper;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "管理端-课程分类")
@RestController
@RequestMapping("/api/admin/categories")
public class CourseCategoryController extends BaseCrudController<CourseCategory> {
    public CourseCategoryController(CourseCategoryMapper mapper) {
        super(mapper, CourseCategory.class);
    }
}
