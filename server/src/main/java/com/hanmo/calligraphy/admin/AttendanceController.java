package com.hanmo.calligraphy.admin;

import com.hanmo.calligraphy.common.BaseCrudController;
import com.hanmo.calligraphy.domain.entity.Attendance;
import com.hanmo.calligraphy.domain.mapper.AttendanceMapper;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "管理端-上课记录")
@RestController
@RequestMapping("/api/admin/attendance-records")
public class AttendanceController extends BaseCrudController<Attendance> {
    public AttendanceController(AttendanceMapper mapper) {
        super(mapper, Attendance.class);
    }
}
