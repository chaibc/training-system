package com.hanmo.calligraphy.admin;

import com.hanmo.calligraphy.common.BaseCrudController;
import com.hanmo.calligraphy.domain.entity.ClassGroup;
import com.hanmo.calligraphy.domain.mapper.ClassGroupMapper;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "管理端-班级")
@RestController
@RequestMapping("/api/admin/classes")
public class ClassGroupController extends BaseCrudController<ClassGroup> {
    public ClassGroupController(ClassGroupMapper mapper) {
        super(mapper, ClassGroup.class);
    }
}
