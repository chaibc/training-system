package com.hanmo.calligraphy.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.*;
import com.baomidou.mybatisplus.annotation.TableLogic;
import lombok.Data;

/**
 * 课程阶段，对应表 course_stage。
 *
 * <p>课程（course.stage_id）可归入某个学习阶段。</p>
 */
@Data
@TableName("course_stage")
public class CourseStage {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String name;
    private String description;
    private Integer sort;
    // 状态：ENABLED 启用 / DISABLED 停用
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // 逻辑删除标记：0 正常 / 1 已删除（查询由 @TableLogic 自动过滤）
    @TableLogic
    private Integer deleted;
}
