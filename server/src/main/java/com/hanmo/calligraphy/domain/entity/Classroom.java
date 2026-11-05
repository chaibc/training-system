package com.hanmo.calligraphy.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.*;
import lombok.Data;

@Data
@TableName("classroom")
public class Classroom {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String name;
    private Integer capacity;
    private String description;
    private Integer sort;
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
