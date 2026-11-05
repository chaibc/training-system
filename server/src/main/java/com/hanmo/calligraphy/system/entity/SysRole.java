package com.hanmo.calligraphy.system.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("sys_role")
public class SysRole {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String code;
    private String name;
    private String description;
    private String dataScope;
    private Integer builtIn;
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
