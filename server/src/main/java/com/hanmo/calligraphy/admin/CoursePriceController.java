package com.hanmo.calligraphy.admin;

import com.hanmo.calligraphy.common.BaseCrudController;
import com.hanmo.calligraphy.domain.entity.CoursePrice;
import com.hanmo.calligraphy.domain.mapper.CoursePriceMapper;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "管理端-班型价格")
@RestController
@RequestMapping("/api/admin/course-prices")
public class CoursePriceController extends BaseCrudController<CoursePrice> {
    public CoursePriceController(CoursePriceMapper mapper) {
        super(mapper, CoursePrice.class);
    }
}
