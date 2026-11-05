package com.hanmo.calligraphy.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.*;
import lombok.Data;

@Data
@TableName("course_outline")
public class CourseOutline {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long courseId;
    private String title;
    private String lessonRange;
    private String description;
    private Integer visible;
    private Integer sort;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
