package com.hanmo.calligraphy.system.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("sys_role_permission")
public class SysRolePermission {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long roleId;
    private String moduleKey;
    private Integer canView;
    private Integer canCreate;
    private Integer canEdit;
    private Integer canDelete;
    private Integer canSpecial;
    private LocalDateTime updatedAt;
}
