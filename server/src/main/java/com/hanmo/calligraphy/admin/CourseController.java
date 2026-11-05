package com.hanmo.calligraphy.admin;

import com.hanmo.calligraphy.common.BaseCrudController;
import com.hanmo.calligraphy.domain.entity.Course;
import com.hanmo.calligraphy.domain.mapper.CourseMapper;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 课程管理：course 表（课程名称、分类/阶段、课时数、上课频次、封面字与配色、标签、上架状态）的增删改查。
 *
 * <p>业务模块：课程与定价；接口前缀 {@code /api/admin/courses}。</p>
 * <p>后台页面：管理端「课程与定价」(/courses，Courses.vue) 的课程页签。</p>
 * <p>权限：GET 允许 ADMIN 与 ACADEMIC（教务可查课程）；POST/PUT/DELETE 由 SecurityConfig 限定仅 ADMIN。</p>
 * <p>通道：继承 {@link BaseCrudController}，六个通道全部开放；分页 keyword 命中 name 列。
 * 删除为逻辑删除（course.deleted）：page/list 自动过滤，但班级、报名等历史外键仍指向该行。</p>
 */
@Tag(name = "管理端-课程")
@RestController
@RequestMapping("/api/admin/courses")
public class CourseController extends BaseCrudController<Course> {
    public CourseController(CourseMapper mapper) {
        super(mapper, Course.class);
    }
}
