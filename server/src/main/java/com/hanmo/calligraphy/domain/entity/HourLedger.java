package com.hanmo.calligraphy.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.math.BigDecimal;
import java.time.*;
import lombok.Data;

/**
 * 课时流水，对应表 hour_ledger。
 *
 * <p>每次购买 / 消课 / 更正都会追加一条，balance_after 记录变动后的剩余课时，用于对账；
 * 流水只追加，不做逻辑删除。</p>
 */
@Data
@TableName("hour_ledger")
public class HourLedger {

    @TableId(type = IdType.AUTO)
    private Long id;

    // 学员 ID（student.id）
    private Long studentId;
    // 关联课次（lesson.id），非消课流水可为空
    private Long lessonId;
    // 流水类型：PURCHASE 购买 / CONSUME 消课 / CORRECT 更正
    private String type;
    // 课时变动值（正数增加、负数扣减，单位：课时，DECIMAL(6,1)）
    private BigDecimal changeHours;
    // 变动后剩余课时
    private BigDecimal balanceAfter;
    // 操作人（sys_user.id）
    private Long operatorId;
    // 变动原因
    private String reason;
    private LocalDateTime createdAt;
}
