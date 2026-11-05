package com.hanmo.calligraphy.admin;

import com.hanmo.calligraphy.common.BaseCrudController;
import com.hanmo.calligraphy.domain.entity.CourseStage;
import com.hanmo.calligraphy.domain.mapper.CourseStageMapper;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "管理端-课程阶段")
@RestController
@RequestMapping("/api/admin/stages")
public class CourseStageController extends BaseCrudController<CourseStage> {
    public CourseStageController(CourseStageMapper mapper) {
        super(mapper, CourseStage.class);
    }
}
