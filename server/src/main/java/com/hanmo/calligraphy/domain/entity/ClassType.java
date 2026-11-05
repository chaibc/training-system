package com.hanmo.calligraphy.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.*;
import com.baomidou.mybatisplus.annotation.TableLogic;
import lombok.Data;

/**
 * 班型，对应表 class_type。
 *
 * <p>描述班级的人数规模区间（capacity_min / capacity_max），课程价格按班型区分。</p>
 */
@Data
@TableName("class_type")
public class ClassType {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String name;
    // 班型人数下限
    private Integer capacityMin;
    // 班型人数上限
    private Integer capacityMax;
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
