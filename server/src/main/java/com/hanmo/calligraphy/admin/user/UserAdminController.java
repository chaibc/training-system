package com.hanmo.calligraphy.admin.user;

import com.hanmo.calligraphy.admin.user.UserAdminDtos.AddBindingRequest;
import com.hanmo.calligraphy.admin.user.UserAdminDtos.AuditRow;
import com.hanmo.calligraphy.admin.user.UserAdminDtos.BindingView;
import com.hanmo.calligraphy.admin.user.UserAdminDtos.CreateUserRequest;
import com.hanmo.calligraphy.admin.user.UserAdminDtos.ParentOption;
import com.hanmo.calligraphy.admin.user.UserAdminDtos.ResetPasswordResponse;
import com.hanmo.calligraphy.admin.user.UserAdminDtos.RolePermissionUpdateRequest;
import com.hanmo.calligraphy.admin.user.UserAdminDtos.RoleView;
import com.hanmo.calligraphy.admin.user.UserAdminDtos.StudentOption;
import com.hanmo.calligraphy.admin.user.UserAdminDtos.UpdateUserRequest;
import com.hanmo.calligraphy.admin.user.UserAdminDtos.UserRow;
import com.hanmo.calligraphy.common.ApiResponse;
import com.hanmo.calligraphy.common.PageResult;
import com.hanmo.calligraphy.security.LoginUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "管理端-用户与权限")
@RestController
@RequestMapping("/api/admin/users")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class UserAdminController {

    private final UserAdminService service;

    @Operation(summary = "用户分页")
    @GetMapping("/page")
    public ApiResponse<PageResult<UserRow>> page(
            @RequestParam(defaultValue = "1") long current,
            @RequestParam(defaultValue = "10") long size,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String keyword) {
        return ApiResponse.ok(service.pageUsers(current, size, type, status, keyword));
    }

    @Operation(summary = "新建教务账号")
    @PostMapping
    public ApiResponse<UserRow> create(@Valid @RequestBody CreateUserRequest request,
                                       @AuthenticationPrincipal LoginUser operator) {
        return ApiResponse.ok(service.createUser(request, operator));
    }

    @Operation(summary = "编辑账号")
    @PutMapping("/{id}")
    public ApiResponse<UserRow> update(@PathVariable Long id,
                                       @RequestBody UpdateUserRequest request,
                                       @AuthenticationPrincipal LoginUser operator) {
        return ApiResponse.ok(service.updateUser(id, request, operator));
    }

    @Operation(summary = "重置后台账号密码")
    @PostMapping("/{id}/reset-password")
    public ApiResponse<ResetPasswordResponse> resetPassword(@PathVariable Long id,
                                                             @AuthenticationPrincipal LoginUser operator) {
        return ApiResponse.ok(service.resetPassword(id, operator));
    }

    @Operation(summary = "启用/停用账号")
    @PostMapping("/{id}/toggle-status")
    public ApiResponse<UserRow> toggleStatus(@PathVariable Long id,
                                             @AuthenticationPrincipal LoginUser operator) {
        return ApiResponse.ok(service.toggleStatus(id, operator));
    }

    @Operation(summary = "解绑微信")
    @PostMapping("/{id}/unbind-wechat")
    public ApiResponse<UserRow> unbindWechat(@PathVariable Long id,
                                             @AuthenticationPrincipal LoginUser operator) {
        return ApiResponse.ok(service.unbindWechat(id, operator));
    }

    @Operation(summary = "角色列表与权限")
    @GetMapping("/roles")
    public ApiResponse<List<RoleView>> roles() {
        return ApiResponse.ok(service.roles());
    }

    @Operation(summary = "更新角色权限")
    @PutMapping("/roles/{roleId}/permissions")
    public ApiResponse<List<RoleView>> updateRolePermissions(@PathVariable Long roleId,
                                                             @RequestBody RolePermissionUpdateRequest request,
                                                             @AuthenticationPrincipal LoginUser operator) {
        return ApiResponse.ok(service.updateRolePermissions(roleId, request, operator));
    }

    @Operation(summary = "待审核绑定")
    @GetMapping("/bindings/pending")
    public ApiResponse<List<BindingView>> pendingBindings() {
        return ApiResponse.ok(service.bindings("PENDING"));
    }

    @Operation(summary = "已绑定关系")
    @GetMapping("/bindings/active")
    public ApiResponse<List<BindingView>> activeBindings() {
        return ApiResponse.ok(service.bindings("ACTIVE"));
    }

    @Operation(summary = "通过家长绑定")
    @PostMapping("/bindings/{id}/approve")
    public ApiResponse<BindingView> approveBinding(@PathVariable Long id,
                                                   @AuthenticationPrincipal LoginUser operator) {
        return ApiResponse.ok(service.approveBinding(id, operator));
    }

    @Operation(summary = "驳回家长绑定")
    @PostMapping("/bindings/{id}/reject")
    public ApiResponse<BindingView> rejectBinding(@PathVariable Long id,
                                                  @AuthenticationPrincipal LoginUser operator) {
        return ApiResponse.ok(service.rejectBinding(id, operator));
    }

    @Operation(summary = "新增家长绑定")
    @PostMapping("/bindings")
    public ApiResponse<BindingView> addBinding(@Valid @RequestBody AddBindingRequest request,
                                               @AuthenticationPrincipal LoginUser operator) {
        return ApiResponse.ok(service.addBinding(request, operator));
    }

    @Operation(summary = "解除家长绑定")
    @PostMapping("/bindings/{id}/unbind")
    public ApiResponse<BindingView> unbindBinding(@PathVariable Long id,
                                                  @AuthenticationPrincipal LoginUser operator) {
        return ApiResponse.ok(service.unbindBinding(id, operator));
    }

    @Operation(summary = "登录与变更记录")
    @GetMapping("/audit-logs")
    public ApiResponse<PageResult<AuditRow>> auditLogs(
            @RequestParam(defaultValue = "1") long current,
            @RequestParam(defaultValue = "20") long size,
            @RequestParam(required = false) String actionType,
            @RequestParam(required = false) String keyword) {
        return ApiResponse.ok(service.auditLogs(current, size, actionType, keyword));
    }

    @Operation(summary = "家长下拉（教务在作品授权中使用）")
    @PreAuthorize("hasAnyRole('ADMIN','ACADEMIC')")
    @GetMapping("/parent-options")
    public ApiResponse<List<ParentOption>> parentOptions() {
        return ApiResponse.ok(service.parentOptions());
    }

    @Operation(summary = "学员下拉")
    @PreAuthorize("hasAnyRole('ADMIN','ACADEMIC')")
    @GetMapping("/student-options")
    public ApiResponse<List<StudentOption>> studentOptions() {
        return ApiResponse.ok(service.studentOptions());
    }
}
