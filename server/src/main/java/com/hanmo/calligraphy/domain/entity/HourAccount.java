package com.hanmo.calligraphy.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.math.BigDecimal;
import java.time.*;
import lombok.Data;

@Data
@TableName("hour_account")
public class HourAccount {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long studentId;
    private BigDecimal totalHours;
    private BigDecimal usedHours;
    private BigDecimal remainingHours;
    private LocalDate validUntil;
    private LocalDateTime updatedAt;
}
