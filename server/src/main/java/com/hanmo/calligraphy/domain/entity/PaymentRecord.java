package com.hanmo.calligraphy.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.math.BigDecimal;
import java.time.*;
import lombok.Data;

/**
 * 线下缴费记录，对应表 payment_record。
 *
 * <p>记录学员的缴费金额、方式与登记人；payment_no 唯一。</p>
 */
@Data
@TableName("payment_record")
public class PaymentRecord {

    @TableId(type = IdType.AUTO)
    private Long id;

    // 缴费单号（唯一）
    private String paymentNo;
    // 关联报名单（enrollment.id），可为空
    private Long enrollmentId;
    // 缴费学员（student.id）
    private Long studentId;
    // 缴费类型：NEW 新报 / RENEW 续费 / MAKEUP 补课
    private String type;
    // 缴费项目说明
    private String item;
    // 缴费金额（单位：元，DECIMAL(10,2)）
    private BigDecimal amount;
    // 缴费方式（payment_method.id）
    private Long paymentMethodId;
    // 缴费时间
    private LocalDateTime paidAt;
    // 登记人（sys_user.id）
    private Long registeredBy;
    private String note;
    private LocalDateTime createdAt;
}
