package com.hanmo.calligraphy.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.*;
import lombok.Data;

/**
 * 教室，对应表 classroom。
 *
 * <p>用于排课时选择上课场地，capacity 为可容纳人数。</p>
 */
@Data
@TableName("classroom")
public class Classroom {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String name;
    // 可容纳人数
    private Integer capacity;
    private String description;
    private Integer sort;
    // 状态：ENABLED 启用 / DISABLED 停用
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
