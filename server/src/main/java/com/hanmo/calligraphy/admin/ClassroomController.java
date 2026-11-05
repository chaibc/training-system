package com.hanmo.calligraphy.admin;

import com.hanmo.calligraphy.common.BaseCrudController;
import com.hanmo.calligraphy.domain.entity.Classroom;
import com.hanmo.calligraphy.domain.mapper.ClassroomMapper;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "管理端-教室")
@RestController
@RequestMapping("/api/admin/classrooms")
public class ClassroomController extends BaseCrudController<Classroom> {
    public ClassroomController(ClassroomMapper mapper) {
        super(mapper, Classroom.class);
    }
}
