package com.hanmo.calligraphy.admin;

import com.hanmo.calligraphy.common.BaseCrudController;
import com.hanmo.calligraphy.domain.entity.Qualification;
import com.hanmo.calligraphy.domain.mapper.QualificationMapper;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "管理端-资质")
@RestController
@RequestMapping("/api/admin/qualifications")
public class QualificationController extends BaseCrudController<Qualification> {
    public QualificationController(QualificationMapper mapper) {
        super(mapper, Qualification.class);
    }
}
