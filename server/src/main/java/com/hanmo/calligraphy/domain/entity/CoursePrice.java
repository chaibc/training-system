package com.hanmo.calligraphy.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import java.math.BigDecimal;
import java.time.*;
import lombok.Data;

/**
 * 班型与价格，对应表 course_price。
 *
 * <p>同一门课程针对不同班型（class_type）配置价格与包含课次，unit 区分按期 / 按次计价。</p>
 */
@Data
@TableName("course_price")
public class CoursePrice {

    @TableId(type = IdType.AUTO)
    private Long id;

    // 所属课程（course.id）
    private Long courseId;
    // 适用班型（class_type.id）
    private Long classTypeId;
    // 该价格包含的课次数量
    private Integer lessons;
    // 价格（单位：元，DECIMAL(10,2)）
    private BigDecimal price;
    // 计价单位：TERM 按期 / TIME 按次
    private String unit;
    // 状态（默认 ON）
    private String status;
    private Integer sort;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    // 逻辑删除标记：0 正常 / 1 已删除（查询由 @TableLogic 自动过滤）
    @TableLogic
    private Integer deleted;

}
