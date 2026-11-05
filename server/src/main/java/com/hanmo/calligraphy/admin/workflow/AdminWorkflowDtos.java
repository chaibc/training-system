package com.hanmo.calligraphy.admin.workflow;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.List;

/**
 * 点名与请假联动模块的请求 DTO。
 *
 * <p>出勤状态取值 PRESENT/LEAVE/ABSENT；请假审批默认通过，只有显式 approve=false 才是驳回。
 * 这些校验由 service 二次确认，注解只保证非空。</p>
 */
public class AdminWorkflowDtos {

    /**
     * 单个学员的点名项：studentId 与 status 必填，note 为空时服务端按状态生成默认说明。
     */
    public record AttendanceItem(
            @NotNull(message = "缺少学员") Long studentId,
            @NotBlank(message = "缺少出勤状态") String status,
            String note
    ) {}

    /**
     * 点名请求：items 至少一条，且同一学员不能重复（service 校验）。
     */
    public record RollcallRequest(List<AttendanceItem> items) {}

    /**
     * 请假审批请求：approve 为 null（不传）按通过处理；驳回时 rejectReason 必填。
     */
    public record LeaveActionRequest(Boolean approve, String rejectReason) {}

    /**
     * 补课安排请求：lessonId 必填，note 可选。
     */
    public record MakeupRequest(@NotNull(message = "请选择补课课次") Long lessonId, String note) {}
}
