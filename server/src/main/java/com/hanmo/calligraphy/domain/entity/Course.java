package com.hanmo.calligraphy.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.*;
import lombok.Data;

/**
 * 课程，对应表 course。
 *
 * <p>保存课程的基础信息、展示信息与上架状态；班型价格见 course_price，内容大纲见 course_outline。</p>
 */
@Data
@TableName("course")
public class Course {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String name;
    // 课程分类（course_category.id）
    private Long categoryId;
    // 课程阶段（course_stage.id），可为空
    private Long stageId;
    // 适龄范围（展示文本）
    private String ageRange;
    // 课次总数
    private Integer lessons;
    // 上课频次说明
    private String frequency;
    // 单次课时长（分钟）
    private Integer durationMinutes;
    // 封面展示字符
    private String coverChar;
    // 封面配色
    private String coverColor;
    private String description;
    private String goal;
    // 课程标签
    private String tags;
    // 是否首页推荐：1=是 / 0=否
    private Integer featured;
    // 上架状态：ON 上架 / OFF 下架
    private String status;
    private Integer sort;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    // 逻辑删除标记：0 正常 / 1 已删除（查询由 @TableLogic 自动过滤）
    @TableLogic
    private Integer deleted;

}
