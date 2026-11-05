package com.hanmo.calligraphy.admin;

import com.hanmo.calligraphy.common.BaseCrudController;
import com.hanmo.calligraphy.domain.entity.TrialSlot;
import com.hanmo.calligraphy.domain.mapper.TrialSlotMapper;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "管理端-试听时段")
@RestController
@RequestMapping("/api/admin/trial-slots")
public class TrialSlotController extends BaseCrudController<TrialSlot> {
    public TrialSlotController(TrialSlotMapper mapper) {
        super(mapper, TrialSlot.class);
    }
}
