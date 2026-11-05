package com.hanmo.calligraphy.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.math.BigDecimal;
import java.time.*;
import lombok.Data;

@Data
@TableName("enrollment")
public class Enrollment {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String enrollmentNo;
    private Long studentId;
    private Long courseId;
    private Long classId;
    private Long parentUserId;
    private String source;
    private String status;
    private BigDecimal amount;
    private LocalDateTime submittedAt;
    private Long confirmedBy;
    private LocalDateTime confirmedAt;
    private String remark;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
