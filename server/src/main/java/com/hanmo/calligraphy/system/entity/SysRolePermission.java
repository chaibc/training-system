package com.hanmo.calligraphy.system.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 角色模块权限，对应表 sys_role_permission。
 *
 * <p>按 (role_id, module_key) 唯一，用权限位描述角色对某个模块可执行的操作。</p>
 */
@Data
@TableName("sys_role_permission")
public class SysRolePermission {

    @TableId(type = IdType.AUTO)
    private Long id;

    // 角色 ID（sys_role.id）
    private Long roleId;
    // 模块标识（dashboard / reports / students 等）
    private String moduleKey;
    // 是否可查看：1=是 / 0=否
    private Integer canView;
    // 是否可新增：1=是 / 0=否
    private Integer canCreate;
    // 是否可编辑：1=是 / 0=否
    private Integer canEdit;
    // 是否可删除：1=是 / 0=否
    private Integer canDelete;
    // 是否可执行特殊操作：1=是 / 0=否
    private Integer canSpecial;
    private LocalDateTime updatedAt;
}
