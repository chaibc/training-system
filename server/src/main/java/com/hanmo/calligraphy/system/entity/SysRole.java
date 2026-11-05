package com.hanmo.calligraphy.system.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 内置角色，对应表 sys_role。
 *
 * <p>角色编码唯一（uk_role_code），决定账号的权限集合与数据范围。</p>
 */
@Data
@TableName("sys_role")
public class SysRole {

    @TableId(type = IdType.AUTO)
    private Long id;

    // 角色编码：ADMIN 管理员 / ACADEMIC 教务 / TEACHER 教师 / PARENT 家长
    private String code;
    private String name;
    private String description;
    // 数据范围：ALL 全部 / OWN_CLASS 本班 / OWN_CHILDREN 本人子女 / SELF 仅本人
    private String dataScope;
    // 是否内置角色：1=内置 / 0=自定义
    private Integer builtIn;
    // 状态（默认 ENABLED）
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
