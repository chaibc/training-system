package com.hanmo.calligraphy.admin;

import com.hanmo.calligraphy.common.BaseCrudController;
import com.hanmo.calligraphy.domain.entity.MakeupArrangement;
import com.hanmo.calligraphy.domain.mapper.MakeupArrangementMapper;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "管理端-补课安排")
@RestController
@RequestMapping("/api/admin/makeup-arrangements")
public class MakeupArrangementController extends BaseCrudController<MakeupArrangement> {
    public MakeupArrangementController(MakeupArrangementMapper mapper) {
        super(mapper, MakeupArrangement.class);
    }
}
