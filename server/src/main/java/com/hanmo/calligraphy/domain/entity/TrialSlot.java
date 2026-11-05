package com.hanmo.calligraphy.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.*;
import lombok.Data;

/**
 * 试听时段，对应表 trial_slot。
 *
 * <p>试听预约（trial_booking.slot_id）从这里选择时段。</p>
 */
@Data
@TableName("trial_slot")
public class TrialSlot {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String name;
    private String description;
    private Integer sort;
    // 状态：ENABLED 启用 / DISABLED 停用
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
