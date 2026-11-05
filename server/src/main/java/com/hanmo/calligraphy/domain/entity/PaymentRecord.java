package com.hanmo.calligraphy.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.math.BigDecimal;
import java.time.*;
import lombok.Data;

@Data
@TableName("payment_record")
public class PaymentRecord {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String paymentNo;
    private Long enrollmentId;
    private Long studentId;
    private String type;
    private String item;
    private BigDecimal amount;
    private Long paymentMethodId;
    private LocalDateTime paidAt;
    private Long registeredBy;
    private String note;
    private LocalDateTime createdAt;
}
