package com.hanmo.calligraphy.admin.user;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;
import java.util.List;

public class UserAdminDtos {

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

    public record CreateUserRequest(
            @NotBlank(message = "请输入姓名") String name,
            @NotBlank(message = "请输入用户名") String username,
            @NotBlank(message = "请输入手机号") String phone,
            String password
    ) {}

    public record UpdateUserRequest(String name, String username, String phone) {}

    public record ResetPasswordResponse(String password) {}

    public record PermissionView(
            String moduleKey,
            boolean canView,
            boolean canCreate,
            boolean canEdit,
            boolean canDelete,
            boolean canSpecial
    ) {}

    public record RoleView(
            Long id,
            String code,
            String name,
            String description,
            String dataScope,
            long userCount,
            List<PermissionView> permissions
    ) {}

    public record RolePermissionUpdateRequest(String dataScope, List<PermissionView> permissions) {}

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

    public record AddBindingRequest(
            @NotNull(message = "请选择家长") Long parentUserId,
            @NotNull(message = "请选择学员") Long studentId,
            String relation,
            Boolean primary
    ) {}

    public record ParentOption(Long id, String name, String phone) {}

    public record StudentOption(Long id, String name, String studentNo) {}

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
