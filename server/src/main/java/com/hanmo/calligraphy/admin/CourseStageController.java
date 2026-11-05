package com.hanmo.calligraphy.admin;

import com.hanmo.calligraphy.common.BaseCrudController;
import com.hanmo.calligraphy.domain.entity.CourseStage;
import com.hanmo.calligraphy.domain.mapper.CourseStageMapper;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 课程阶段管理：course_stage 表（学习阶段名称、说明、排序、启停）的增删改查。
 *
 * <p>业务模块：基础数据字典；接口前缀 {@code /api/admin/stages}。</p>
 * <p>后台页面：管理端「基础数据」(/basic，BasicData.vue) 的课程阶段页签。</p>
 * <p>权限：GET 允许 ADMIN 与 ACADEMIC；POST/PUT/DELETE 由 SecurityConfig 限定仅 ADMIN。</p>
 * <p>通道：继承 {@link BaseCrudController}，六个通道全部开放；分页 keyword 命中 name 列；删除为物理删除，
 * 不校验 course.stage_id 的引用。</p>
 */
@Tag(name = "管理端-课程阶段")
@RestController
@RequestMapping("/api/admin/stages")
public class CourseStageController extends BaseCrudController<CourseStage> {
    public CourseStageController(CourseStageMapper mapper) {
        super(mapper, CourseStage.class);
    }
}
