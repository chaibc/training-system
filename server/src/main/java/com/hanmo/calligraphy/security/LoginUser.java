package com.hanmo.calligraphy.security;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * 登录用户快照，作为 SecurityContext 中的 principal。
 *
 * <p>由 JwtAuthenticationFilter 在每次请求时根据令牌重新构建，不持久化；
 * 业务代码可通过 @AuthenticationPrincipal 获取。</p>
 */
@Data
@AllArgsConstructor
public class LoginUser {
    // 账号 ID（sys_user.id）
    private Long id;
    // 后台账号用户名（教师 / 家长为空）
    private String username;
    // 姓名
    private String name;
    // 账号类型：ADMIN 后台 / TEACHER 教师 / PARENT 家长
    private String userType;
    // 角色编码
    private String roleCode;
    // 角色名称
    private String roleName;
    // 数据范围标识：ALL / OWN_CLASS / OWN_CHILDREN / SELF
    private String dataScope;
}
