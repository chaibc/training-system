package com.hanmo.calligraphy.admin.user;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 用户与权限模块的请求/响应 DTO 容器。
 *
 * <p>密码只在创建/重置时以 initialPassword 明文回传一次；列表里的手机号已脱敏。
 * 绑定状态与来源取值见 parent_student.status / source。</p>
 */
public class UserAdminDtos {

    /**
     * 账号列表/详情行：canDisable/manager 供前端控制操作按钮，initialPassword 仅新建/重置时有值。
     */
    public record UserRow(
            Long id,
            String name,
            String username,
            String userType,
            String roleCode,
            String roleName,
            String phone,
            boolean wechatBound,
            String status,
            LocalDateTime lastLoginAt,
            String related,
            boolean canDisable,
            boolean manager,
            String initialPassword
    ) {}

    /**
     * 新建后台账号请求：姓名/用户名/手机号必填；password 为空时由服务端生成随机初始密码。
     */
    public record CreateUserRequest(
            @NotBlank(message = "请输入姓名") String name,
            @NotBlank(message = "请输入用户名") String username,
            @NotBlank(message = "请输入手机号") String phone,
            String password
    ) {}

    /**
     * 编辑账号请求：字段为 null/空表示不修改。
     */
    public record UpdateUserRequest(String name, String username, String phone) {}

    /**
     * 重置密码响应：只回传新密码，要求首登修改。
     */
    public record ResetPasswordResponse(String password) {}

    /**
     * 单个模块的权限项：moduleKey 取值见 UserAdminService.MODULES 白名单。
     */
    public record PermissionView(
            String moduleKey,
            boolean canView,
            boolean canCreate,
            boolean canEdit,
            boolean canDelete,
            boolean canSpecial
    ) {}

    /**
     * 角色视图：dataScope 为 ALL/OWN_CLASS/OWN_CHILDREN/SELF，permissions 为该角色的模块权限。
     */
    public record RoleView(
            Long id,
            String code,
            String name,
            String description,
            String dataScope,
            long userCount,
            List<PermissionView> permissions
    ) {}

    /**
     * 更新角色权限请求：dataScope 为空表示不改；permissions 为全量替换（先删后插）。
     */
    public record RolePermissionUpdateRequest(String dataScope, List<PermissionView> permissions) {}

    /**
     * 家长-学员绑定视图：status/source 为原始枚举，verifyText 为审核参考文案。
     */
    public record BindingView(
            Long id,
            Long parentUserId,
            String parentName,
            String parentPhone,
            Long studentId,
            String studentName,
            String studentNo,
            String relation,
            String relationText,
            boolean primary,
            String status,
            String source,
            LocalDateTime createdAt,
            String verifyText
    ) {}

    /**
     * 管理员新增绑定请求：primary=true 表示设为主联系人（会清掉该学员其它主联系人标记）。
     */
    public record AddBindingRequest(
            @NotNull(message = "请选择家长") Long parentUserId,
            @NotNull(message = "请选择学员") Long studentId,
            String relation,
            Boolean primary
    ) {}

    /**
     * 家长下拉选项：phone 未脱敏，供教务核对身份。
     */
    public record ParentOption(Long id, String name, String phone) {}

    /**
     * 学员下拉选项。
     */
    public record StudentOption(Long id, String name, String studentNo) {}

    /**
     * 审计日志行：actionType 为中文动作名，result 为 SUCCESS/FAIL。
     */
    public record AuditRow(
            Long id,
            String operatorName,
            String actionType,
            String targetType,
            String content,
            String ip,
            String result,
            LocalDateTime createdAt
    ) {}
}
