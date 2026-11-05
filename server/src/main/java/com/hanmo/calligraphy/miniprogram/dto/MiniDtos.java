package com.hanmo.calligraphy.miniprogram.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public class MiniDtos {

    public record TrialRequest(
            @NotBlank(message = "请输入学员姓名") String studentName,
            String age,
            String parentName,
            @NotBlank(message = "请输入手机号") String phone,
            Long courseId,
            Long slotId,
            String note
    ) {}

    public record BindingRequest(
            Long studentId,
            String studentName,
            String studentNo,
            String phoneTail,
            String relation,
            Boolean newStudent,
            String gender,
            LocalDate birthDate,
            String school,
            String grade
    ) {}

    public record ChildProfileRequest(
            String gender,
            LocalDate birthDate,
            String school,
            String grade
    ) {}

    public record LeaveCreateRequest(
            @NotNull(message = "请选择学员") Long studentId,
            @NotNull(message = "请选择课次") Long lessonId,
            Long reasonId,
            String reasonText
    ) {}

    public record EnrollmentCreateRequest(
            @NotNull(message = "请选择学员") Long studentId,
            @NotNull(message = "请选择课程") Long courseId,
            Long classId
    ) {}

    public record AttendanceItem(Long studentId, String status, String note) {}

    public record RollcallRequest(List<AttendanceItem> items) {}

    public record ApproveLeaveRequest(Boolean approve, String rejectReason) {}

    public record WorkCreateRequest(
            @NotNull(message = "请选择学员") Long studentId,
            Long classId,
            @NotBlank(message = "请输入作品标题") String title,
            String workType,
            String contentChar,
            String imageUrl,
            String comment,
            Boolean authorized
    ) {}

    public record CommentRequest(@NotBlank(message = "请输入留言内容") String content) {}

    public record AmountRequest(BigDecimal amount) {}
}
