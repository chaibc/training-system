package com.hanmo.calligraphy.admin;

import com.hanmo.calligraphy.common.BaseCrudController;
import com.hanmo.calligraphy.domain.entity.LeaveReason;
import com.hanmo.calligraphy.domain.mapper.LeaveReasonMapper;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "管理端-请假原因")
@RestController
@RequestMapping("/api/admin/leave-reasons")
public class LeaveReasonController extends BaseCrudController<LeaveReason> {
    public LeaveReasonController(LeaveReasonMapper mapper) {
        super(mapper, LeaveReason.class);
    }
}
