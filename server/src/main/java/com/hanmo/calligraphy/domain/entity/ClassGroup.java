package com.hanmo.calligraphy.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.*;
import lombok.Data;

@Data
@TableName("class_group")
public class ClassGroup {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String name;
    private Long courseId;
    private Long teacherId;
    private Long classroomId;
    private Integer weekday;
    private LocalTime startTime;
    private LocalTime endTime;
    private Integer capacity;
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    @TableLogic
    private Integer deleted;

}
