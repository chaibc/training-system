package com.hanmo.calligraphy.admin;

import com.hanmo.calligraphy.common.BaseCrudController;
import com.hanmo.calligraphy.domain.entity.WorkAuth;
import com.hanmo.calligraphy.domain.mapper.WorkAuthMapper;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "管理端-作品授权")
@RestController
@RequestMapping("/api/admin/work-auths")
public class WorkAuthController extends BaseCrudController<WorkAuth> {
    public WorkAuthController(WorkAuthMapper mapper) {
        super(mapper, WorkAuth.class);
    }
}
