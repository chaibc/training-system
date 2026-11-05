package com.hanmo.calligraphy.admin;

import com.hanmo.calligraphy.common.BaseCrudController;
import com.hanmo.calligraphy.domain.entity.Work;
import com.hanmo.calligraphy.domain.mapper.WorkMapper;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "管理端-作品")
@RestController
@RequestMapping("/api/admin/works")
public class WorkController extends BaseCrudController<Work> {
    public WorkController(WorkMapper mapper) {
        super(mapper, Work.class);
    }
}
