package com.hanmo.calligraphy.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.*;
import lombok.Data;

@Data
@TableName("business_rule")
public class BusinessRule {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String ruleKey;
    private String ruleValue;
    private String description;
    private LocalDateTime updatedAt;
}
