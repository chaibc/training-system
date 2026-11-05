package com.hanmo.calligraphy.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import java.math.BigDecimal;
import java.time.*;
import lombok.Data;

@Data
@TableName("course_price")
public class CoursePrice {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long courseId;
    private Long classTypeId;
    private Integer lessons;
    private BigDecimal price;
    private String unit;
    private String status;
    private Integer sort;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    @TableLogic
    private Integer deleted;

}
