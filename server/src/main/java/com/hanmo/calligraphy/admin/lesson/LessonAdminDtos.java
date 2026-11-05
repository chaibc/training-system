package com.hanmo.calligraphy.admin.lesson;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.time.LocalTime;

/**
 * 课次与班级学员模块的请求 DTO。
 *
 * <p>字段校验用 Jakarta Validation 注解，由 controller 的 @Valid 触发；
 * 业务级校验（时间先后、冲突、容量）在 LessonAdminService 内完成。</p>
 */
public class LessonAdminDtos {

    /**
     * 课次新增/修改请求。
     *
     * <p>classId/lessonNo/日期/起止时间必填；teacherId、classroomId 为空时继承班级配置；
     * status 为空按 SCHEDULED；originalLessonId 仅补课课次使用，指向被补的原课次。</p>
     */
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

    /**
     * 加入班级学员请求：只带学员 ID，班级 ID 在路径参数里。
     */
    public record AddClassStudentRequest(@NotNull(message = "请选择学员") Long studentId) {}
}
