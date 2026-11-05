package com.hanmo.calligraphy.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.*;
import lombok.Data;

/**
 * 业务规则配置，对应表 business_rule。
 *
 * <p>以「规则键 - 规则值」形式保存可调参数（如续费提醒阈值），业务代码按 rule_key 读取；
 * rule_key 上有唯一键 uk_rule_key。</p>
 */
@Data
@TableName("business_rule")
public class BusinessRule {

    @TableId(type = IdType.AUTO)
    private Long id;

    // 规则键（唯一）
    private String ruleKey;
    // 规则值（统一以字符串存储，使用方按需转换类型）
    private String ruleValue;
    // 规则说明
    private String description;
    // 最后更新时间（该表不记录创建时间）
    private LocalDateTime updatedAt;
}
