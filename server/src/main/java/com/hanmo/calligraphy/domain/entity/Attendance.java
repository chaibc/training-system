package com.hanmo.calligraphy.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.math.BigDecimal;
import java.time.*;
import lombok.Data;

@Data
@TableName("attendance")
public class Attendance {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long lessonId;
    private Long studentId;
    private String status;
    private BigDecimal consumedHours;
    private String note;
    private Long recordedBy;
    private LocalDateTime recordedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
