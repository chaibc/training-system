package com.hanmo.calligraphy.admin;

import com.hanmo.calligraphy.common.BaseCrudController;
import com.hanmo.calligraphy.domain.entity.CourseOutline;
import com.hanmo.calligraphy.domain.mapper.CourseOutlineMapper;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 内容大纲管理：course_outline 表（按 courseId 归属、课段 lessonRange、说明、可见性 visible）的增删改查。
 *
 * <p>业务模块：课程与定价；接口前缀 {@code /api/admin/course-outlines}。</p>
 * <p>后台页面：管理端「课程与定价」(/courses，Courses.vue) 的内容大纲页签。</p>
 * <p>权限：GET 允许 ADMIN 与 ACADEMIC；POST/PUT/DELETE 由 SecurityConfig 限定仅 ADMIN。</p>
 * <p>通道：继承 {@link BaseCrudController}，六个通道全部开放；分页 keyword 命中 title 列；删除为物理删除。</p>
 * <p>注意：基类 /list 不接收任何查询参数，admin-web 的 api 里预留了 list(courseId)，
 * 若改用它按课程过滤，courseId 会被忽略而返回全量大纲。</p>
 */
@Tag(name = "管理端-内容大纲")
@RestController
@RequestMapping("/api/admin/course-outlines")
public class CourseOutlineController extends BaseCrudController<CourseOutline> {
    public CourseOutlineController(CourseOutlineMapper mapper) {
        super(mapper, CourseOutline.class);
    }
}
