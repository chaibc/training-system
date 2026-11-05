package com.hanmo.calligraphy.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.*;
import com.baomidou.mybatisplus.annotation.TableLogic;
import lombok.Data;

/**
 * 机构资质，对应表 qualification。
 *
 * <p>保存资质名称、证书编号、有效期与核验 / 展示状态。</p>
 */
@Data
@TableName("qualification")
public class Qualification {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String name;
    // 证书编号
    private String code;
    // 有效期（按原文以字符串存储）
    private java.time.LocalDate validUntil;
    // 核验状态：VERIFIED 已核验 / PENDING 待核验
    private String verifyStatus;
    // 是否前台展示：1=展示 / 0=隐藏
    private Integer visible;
    private Integer sort;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // 逻辑删除标记：0 正常 / 1 已删除（查询由 @TableLogic 自动过滤）
    @TableLogic
    private Integer deleted;
}
