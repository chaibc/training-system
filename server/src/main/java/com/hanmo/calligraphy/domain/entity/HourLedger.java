package com.hanmo.calligraphy.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.math.BigDecimal;
import java.time.*;
import lombok.Data;

@Data
@TableName("hour_ledger")
public class HourLedger {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long studentId;
    private Long lessonId;
    private String type;
    private BigDecimal changeHours;
    private BigDecimal balanceAfter;
    private Long operatorId;
    private String reason;
    private LocalDateTime createdAt;
}
