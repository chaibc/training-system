package com.hanmo.calligraphy.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.*;
import lombok.Data;

/**
 * 课程分类，对应表 course_category。
 *
 * <p>课程（course.category_id）按分类归档。</p>
 */
@Data
@TableName("course_category")
public class CourseCategory {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String name;
    // 分类编码
    private String code;
    private String description;
    private Integer sort;
    // 状态：ENABLED 启用 / DISABLED 停用
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
