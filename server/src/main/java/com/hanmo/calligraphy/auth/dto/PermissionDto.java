package com.hanmo.calligraphy.auth.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * 单个模块的权限点，登录响应中 {@code permissions} 的元素。
 *
 * <p>对应 {@code sys_role_permission} 的一行：数据库用 0/1 存放，这里已转成布尔，
 * 管理端左侧导航与操作按钮的显隐直接依赖这些值。</p>
 */
@Data
@AllArgsConstructor
public class PermissionDto {
    /** 模块标识，与前端菜单/按钮的 moduleKey 对应，如 dashboard、students、reports */
    private String moduleKey;
    /** 是否可查看该模块（对应 can_view） */
    private boolean canView;
    /** 是否可新增（对应 can_create） */
    private boolean canCreate;
    /** 是否可编辑（对应 can_edit） */
    private boolean canEdit;
    /** 是否可删除（对应 can_delete） */
    private boolean canDelete;
    /** 是否可执行该模块的特殊操作（对应 can_special），具体动作由模块自行定义 */
    private boolean canSpecial;
}
