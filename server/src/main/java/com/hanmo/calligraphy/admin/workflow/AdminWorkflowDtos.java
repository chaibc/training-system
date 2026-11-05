package com.hanmo.calligraphy.admin.workflow;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public class AdminWorkflowDtos {

    public record AttendanceItem(
            @NotNull(message = "缺少学员") Long studentId,
            @NotBlank(message = "缺少出勤状态") String status,
            String note
    ) {}

    public record RollcallRequest(List<AttendanceItem> items) {}

    public record LeaveActionRequest(Boolean approve, String rejectReason) {}

    public record MakeupRequest(@NotNull(message = "请选择补课课次") Long lessonId, String note) {}
}
