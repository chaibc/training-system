package com.hanmo.calligraphy.admin.lesson;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.time.LocalTime;

public class LessonAdminDtos {

    public record LessonRequest(
            @NotNull(message = "请选择班级") Long classId,
            @NotNull(message = "请输入课次序号") @Min(value = 1, message = "课次序号必须大于 0") Integer lessonNo,
            @NotNull(message = "请选择上课日期") LocalDate lessonDate,
            @NotNull(message = "请选择开始时间") LocalTime startTime,
            @NotNull(message = "请选择结束时间") LocalTime endTime,
            Long teacherId,
            Long classroomId,
            String status,
            Long originalLessonId,
            String note
    ) {}

    public record AddClassStudentRequest(@NotNull(message = "请选择学员") Long studentId) {}
}
