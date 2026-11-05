package com.hanmo.calligraphy.admin;

import com.hanmo.calligraphy.common.BaseCrudController;
import com.hanmo.calligraphy.domain.entity.TrialBooking;
import com.hanmo.calligraphy.domain.mapper.TrialBookingMapper;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "管理端-试听预约")
@RestController
@RequestMapping("/api/admin/trials")
public class TrialBookingController extends BaseCrudController<TrialBooking> {
    public TrialBookingController(TrialBookingMapper mapper) {
        super(mapper, TrialBooking.class);
    }
}
