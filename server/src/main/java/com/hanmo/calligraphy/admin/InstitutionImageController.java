package com.hanmo.calligraphy.admin;

import com.hanmo.calligraphy.common.BaseCrudController;
import com.hanmo.calligraphy.domain.entity.InstitutionImage;
import com.hanmo.calligraphy.domain.mapper.InstitutionImageMapper;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "管理端-机构图片")
@RestController
@RequestMapping("/api/admin/institution-images")
public class InstitutionImageController extends BaseCrudController<InstitutionImage> {
    public InstitutionImageController(InstitutionImageMapper mapper) {
        super(mapper, InstitutionImage.class);
    }
}
