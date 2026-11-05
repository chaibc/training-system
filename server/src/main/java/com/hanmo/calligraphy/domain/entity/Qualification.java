package com.hanmo.calligraphy.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.*;
import lombok.Data;

@Data
@TableName("qualification")
public class Qualification {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String name;
    private String code;
    private String validUntil;
    private String verifyStatus;
    private Integer visible;
    private Integer sort;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
