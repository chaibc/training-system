package com.hanmo.calligraphy.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.*;
import lombok.Data;

/**
 * 课程内容大纲，对应表 course_outline。
 *
 * <p>按课程维护大纲条目及其展示顺序。</p>
 */
@Data
@TableName("course_outline")
public class CourseOutline {

    @TableId(type = IdType.AUTO)
    private Long id;

    // 所属课程（course.id）
    private Long courseId;
    private String title;
    // 适用课次范围（展示文本）
    private String lessonRange;
    private String description;
    // 是否可见：1=可见 / 0=隐藏
    private Integer visible;
    private Integer sort;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
