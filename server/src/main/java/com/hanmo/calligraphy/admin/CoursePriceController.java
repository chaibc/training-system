package com.hanmo.calligraphy.admin;

import com.hanmo.calligraphy.common.BaseCrudController;
import com.hanmo.calligraphy.domain.entity.CoursePrice;
import com.hanmo.calligraphy.domain.mapper.CoursePriceMapper;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 班型价格管理：course_price 表（课程 × 班型的课时数与价格）的增删改查。
 *
 * <p>业务模块：课程与定价；接口前缀 {@code /api/admin/course-prices}。</p>
 * <p>后台页面：管理端「课程与定价」(/courses，Courses.vue) 的班型价格页签。</p>
 * <p>权限：GET 允许 ADMIN 与 ACADEMIC；POST/PUT/DELETE 由 SecurityConfig 限定仅 ADMIN。</p>
 * <p>通道：继承 {@link BaseCrudController}，六个通道全部开放；该表没有 name/title/phone 等候选列，
 * 分页 keyword 会被忽略（始终按 id 倒序）。删除为逻辑删除（course_price.deleted）。</p>
 */
@Tag(name = "管理端-班型价格")
@RestController
@RequestMapping("/api/admin/course-prices")
public class CoursePriceController extends BaseCrudController<CoursePrice> {
    public CoursePriceController(CoursePriceMapper mapper) {
        super(mapper, CoursePrice.class);
    }
}
