package com.hanmo.calligraphy.auth.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;

/**
 * 登录与“当前用户”响应体，{@code /api/auth/login}、{@code /wx-login}、{@code /me} 共用。
 *
 * <p>字段名是小程序与管理后台的既有契约（前端直接读取），改名会破坏两端；
 * 如需调整应同步前端并保留旧字段过渡。</p>
 */
@Data
@Builder
public class LoginResponse {
    /** JWT，后续请求放在 Authorization: Bearer &lt;token&gt;；/me 不签发新令牌，此字段为 null */
    private String token;
    /** 账号主键 sys_user.id */
    private Long userId;
    /** 后台登录用户名；小程序自动建号的家长/教师可能为 null */
    private String username;
    /** 显示名：账号名称（后台账号姓名、家长昵称或教师姓名） */
    private String name;
    /** 账号类型：ADMIN / ACADEMIC / TEACHER / PARENT，前端据此决定进入哪个端 */
    private String userType;
    /** 角色编码，如 ADMIN / TEACHER / PARENT，用于前端路由与按钮判断 */
    private String roleCode;
    /** 角色中文名，如“管理者”“教师”“家长” */
    private String roleName;
    /** 数据范围标识（来自 sys_role.data_scope）：ALL / OWN_CLASS / OWN_CHILDREN / SELF */
    private String dataScope;
    /** 是否强制修改密码；管理员新建或重置密码后为 true，前端应先跳改密页 */
    private Boolean forceChangePassword;
    /** 模块权限点列表，管理端导航与按钮权限来源；小程序账号通常为空数组 */
    private List<PermissionDto> permissions;
}
