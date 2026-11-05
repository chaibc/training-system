package com.hanmo.calligraphy.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.math.BigDecimal;
import java.time.*;
import lombok.Data;

/**
 * 课时账户，对应表 hour_account。
 *
 * <p>每个学员一条（唯一键 uk_hour_student），保存累计购买、已消耗与剩余课时；
 * 课时数量为 DECIMAL(8,1)，可精确到 0.1 课时。</p>
 */
@Data
@TableName("hour_account")
public class HourAccount {

    @TableId(type = IdType.AUTO)
    private Long id;

    // 学员 ID（student.id）
    private Long studentId;
    // 累计购买课时（单位：课时，DECIMAL(8,1)）
    private BigDecimal totalHours;
    // 已消耗课时（单位：课时）
    private BigDecimal usedHours;
    // 剩余课时（单位：课时，与 total/used 同步维护）
    private BigDecimal remainingHours;
    // 课时有效期截止日
    private LocalDate validUntil;
    private LocalDateTime updatedAt;
}
