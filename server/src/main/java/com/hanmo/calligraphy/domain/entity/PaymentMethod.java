package com.hanmo.calligraphy.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.*;
import com.baomidou.mybatisplus.annotation.TableLogic;
import lombok.Data;

/**
 * 线下缴费方式，对应表 payment_method。
 *
 * <p>缴费记录（payment_record.payment_method_id）从这里选择方式。</p>
 */
@Data
@TableName("payment_method")
public class PaymentMethod {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String name;
    private String description;
    private Integer sort;
    // 状态：ENABLED 启用 / DISABLED 停用
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // 逻辑删除标记：0 正常 / 1 已删除（查询由 @TableLogic 自动过滤）
    @TableLogic
    private Integer deleted;
}
