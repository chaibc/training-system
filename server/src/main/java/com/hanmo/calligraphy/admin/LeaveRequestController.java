package com.hanmo.calligraphy.admin;

import com.hanmo.calligraphy.common.BaseCrudController;
import com.hanmo.calligraphy.domain.entity.LeaveRequest;
import com.hanmo.calligraphy.domain.mapper.LeaveRequestMapper;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "管理端-请假记录")
@RestController
@RequestMapping("/api/admin/leave")
public class LeaveRequestController extends BaseCrudController<LeaveRequest> {
    public LeaveRequestController(LeaveRequestMapper mapper) {
        super(mapper, LeaveRequest.class);
    }
}
