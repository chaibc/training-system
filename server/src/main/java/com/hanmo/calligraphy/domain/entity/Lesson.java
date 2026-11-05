package com.hanmo.calligraphy.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.*;
import lombok.Data;

/**
 * 课次，对应表 lesson。
 *
 * <p>表示某班级在具体日期时段的一次课；唯一键 (class_id, lesson_no) 保证班内课次序号不重复。</p>
 */
@Data
@TableName("lesson")
public class Lesson {

    @TableId(type = IdType.AUTO)
    private Long id;

    // 所属班级（class_group.id）
    private Long classId;
    // 班内课次序号（与 class_id 组成唯一键）
    private Integer lessonNo;
    // 上课日期
    private LocalDate lessonDate;
    // 上课开始时间
    private LocalTime startTime;
    // 上课结束时间
    private LocalTime endTime;
    // 任课教师（teacher_profile.id）
    private Long teacherId;
    // 上课教室（classroom.id）
    private Long classroomId;
    // 课次状态：SCHEDULED 已排课 / DONE 已完成 / CANCELLED 已取消
    private String status;
    // 原课次 ID（调课 / 补课场景指向被调整的课次）
    private Long originalLessonId;
    private String note;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
