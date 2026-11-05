package com.hanmo.calligraphy.admin;

import com.hanmo.calligraphy.common.BaseCrudController;
import com.hanmo.calligraphy.domain.entity.HourLedger;
import com.hanmo.calligraphy.domain.mapper.HourLedgerMapper;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "管理端-课时流水")
@RestController
@RequestMapping("/api/admin/hour-ledger")
public class HourLedgerController extends BaseCrudController<HourLedger> {
    public HourLedgerController(HourLedgerMapper mapper) {
        super(mapper, HourLedger.class);
    }
}
