package com.hanmo.calligraphy.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.*;
import lombok.Data;

@Data
@TableName("lesson")
public class Lesson {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long classId;
    private Integer lessonNo;
    private LocalDate lessonDate;
    private LocalTime startTime;
    private LocalTime endTime;
    private Long teacherId;
    private Long classroomId;
    private String status;
    private Long originalLessonId;
    private String note;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
