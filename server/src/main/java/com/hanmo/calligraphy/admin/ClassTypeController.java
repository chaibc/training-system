package com.hanmo.calligraphy.admin;

import com.hanmo.calligraphy.common.BaseCrudController;
import com.hanmo.calligraphy.domain.entity.ClassType;
import com.hanmo.calligraphy.domain.mapper.ClassTypeMapper;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "管理端-班型")
@RestController
@RequestMapping("/api/admin/class-types")
public class ClassTypeController extends BaseCrudController<ClassType> {
    public ClassTypeController(ClassTypeMapper mapper) {
        super(mapper, ClassType.class);
    }
}
