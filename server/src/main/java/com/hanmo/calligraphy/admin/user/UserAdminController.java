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

/**
 * 用户与权限接口：后台账号、角色权限、家长绑定审核、审计日志、下拉选项。
 *
 * <p>路由前缀 /api/admin/users。类级 @PreAuthorize("hasRole('ADMIN')") 限制为管理者；
 * 两个下拉接口额外放开给教务（作品授权、绑定表单要用）。角色是唯一的后台准入边界，
 * 数据范围（sys_role.data_scope）不在服务端参与过滤。</p>
 *
 * <p>逻辑全部委托 UserAdminService，本类只做参数绑定与 ApiResponse 包装。</p>
 * 落库表：sys_user、sys_role、sys_role_permission、parent_student、sys_audit_log（经 service 落库）
 */
@Tag(name = "管理端-用户与权限")
@RestController
@RequestMapping("/api/admin/users")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class UserAdminController {

    private final UserAdminService service;

    /**
     * 账号分页（管理者）。
     *
     * <p>只读；type/status/keyword 透传给 service，手机号返回时已脱敏。</p>
     */
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

    /**
     * 新建教务账号。
     *
     * <p>副作用：写 sys_user 与审计日志；返回体带一次性的明文初始密码。</p>
     *
     * @throws BusinessException 用户名重复或手机号格式错误（400）
     */
    @Operation(summary = "新建教务账号")
    @PostMapping
    public ApiResponse<UserRow> create(@Valid @RequestBody CreateUserRequest request,
                                       @AuthenticationPrincipal LoginUser operator) {
        return ApiResponse.ok(service.createUser(request, operator));
    }

    /**
     * 编辑账号资料。
     *
     * <p>副作用：更新 sys_user；非后台账号换手机号会解绑微信，家长账号还会同步 parent_profile。</p>
     *
     * @throws BusinessException 404 账号不存在；400 用户名重复/手机号格式错误
     */
    @Operation(summary = "编辑账号")
    @PutMapping("/{id}")
    public ApiResponse<UserRow> update(@PathVariable Long id,
                                       @RequestBody UpdateUserRequest request,
                                       @AuthenticationPrincipal LoginUser operator) {
        return ApiResponse.ok(service.updateUser(id, request, operator));
    }

    /**
     * 重置后台账号密码。
     *
     * <p>副作用：更新密码哈希、清空失败次数与锁定时间、强制首登改密，并写审计。</p>
     *
     * @throws BusinessException 400 教师/家长账号无需重置密码
     */
    @Operation(summary = "重置后台账号密码")
    @PostMapping("/{id}/reset-password")
    public ApiResponse<ResetPasswordResponse> resetPassword(@PathVariable Long id,
                                                             @AuthenticationPrincipal LoginUser operator) {
        return ApiResponse.ok(service.resetPassword(id, operator));
    }

    /**
     * 启用/停用账号。
     *
     * <p>副作用：切换 sys_user.status 并写审计；管理者账号禁止停用。</p>
     *
     * @throws BusinessException 400 管理者账号唯一，不可停用
     */
    @Operation(summary = "启用/停用账号")
    @PostMapping("/{id}/toggle-status")
    public ApiResponse<UserRow> toggleStatus(@PathVariable Long id,
                                             @AuthenticationPrincipal LoginUser operator) {
        return ApiResponse.ok(service.toggleStatus(id, operator));
    }

    /**
     * 解绑账号微信。
     *
     * <p>副作用：清空 openid/unionid 并写审计；后台账号直接拒绝。</p>
     *
     * @throws BusinessException 400 后台账号不使用微信登录
     */
    @Operation(summary = "解绑微信")
    @PostMapping("/{id}/unbind-wechat")
    public ApiResponse<UserRow> unbindWechat(@PathVariable Long id,
                                             @AuthenticationPrincipal LoginUser operator) {
        return ApiResponse.ok(service.unbindWechat(id, operator));
    }

    /**
     * 角色列表与模块权限。
     *
     * <p>只读。</p>
     * @return 角色视图列表（含权限与账号数）
     */
    @Operation(summary = "角色列表与权限")
    @GetMapping("/roles")
    public ApiResponse<List<RoleView>> roles() {
        return ApiResponse.ok(service.roles());
    }

    /**
     * 更新角色数据范围与模块权限。
     *
     * <p>副作用：更新 sys_role.data_scope、整表替换 sys_role_permission、写审计，同一事务。</p>
     *
     * @throws BusinessException 404 角色不存在；400 管理者/教师/家长角色不可修改
     */
    @Operation(summary = "更新角色权限")
    @PutMapping("/roles/{roleId}/permissions")
    public ApiResponse<List<RoleView>> updateRolePermissions(@PathVariable Long roleId,
                                                             @RequestBody RolePermissionUpdateRequest request,
                                                             @AuthenticationPrincipal LoginUser operator) {
        return ApiResponse.ok(service.updateRolePermissions(roleId, request, operator));
    }

    /**
     * 待审核绑定列表（新绑定 + 解绑申请）。
     *
     * <p>只读。</p>
     * @return 待审核绑定列表（新绑定 + 解绑申请）
     */
    @Operation(summary = "待审核绑定")
    @GetMapping("/bindings/pending")
    public ApiResponse<List<BindingView>> pendingBindings() {
        return ApiResponse.ok(service.bindings("PENDING"));
    }

    /**
     * 已生效绑定列表（排除审核中的解绑申请）。
     *
     * <p>只读。</p>
     * @return 已生效绑定列表
     */
    @Operation(summary = "已绑定关系")
    @GetMapping("/bindings/active")
    public ApiResponse<List<BindingView>> activeBindings() {
        return ApiResponse.ok(service.bindings("ACTIVE"));
    }

    /**
     * 通过绑定/解绑申请。
     *
     * <p>副作用：更新 parent_student 状态与审核人，写审计；学员 ACTIVE 监护人上限 2 人。</p>
     *
     * @throws BusinessException 400 学员已有 2 位有效监护人；404 绑定不存在
     */
    @Operation(summary = "通过家长绑定")
    @PostMapping("/bindings/{id}/approve")
    public ApiResponse<BindingView> approveBinding(@PathVariable Long id,
                                                   @AuthenticationPrincipal LoginUser operator) {
        return ApiResponse.ok(service.approveBinding(id, operator));
    }

    /**
     * 驳回绑定/解绑申请。
     *
     * <p>副作用：新绑定置 REJECTED；解绑申请恢复 ACTIVE 且 source 改回 SELF；写审计。</p>
     *
     * @throws BusinessException 404 绑定不存在
     */
    @Operation(summary = "驳回家长绑定")
    @PostMapping("/bindings/{id}/reject")
    public ApiResponse<BindingView> rejectBinding(@PathVariable Long id,
                                                  @AuthenticationPrincipal LoginUser operator) {
        return ApiResponse.ok(service.rejectBinding(id, operator));
    }

    /**
     * 管理员直接新增绑定。
     *
     * <p>副作用：写/复用 parent_student 行并写审计；primary=true 会清掉该学员其它主联系人标记。</p>
     *
     * @throws BusinessException 非家长账号、已有有效绑定、监护人数已满（400）
     */
    @Operation(summary = "新增家长绑定")
    @PostMapping("/bindings")
    public ApiResponse<BindingView> addBinding(@Valid @RequestBody AddBindingRequest request,
                                               @AuthenticationPrincipal LoginUser operator) {
        return ApiResponse.ok(service.addBinding(request, operator));
    }

    /**
     * 管理员直接解除绑定。
     *
     * <p>副作用：parent_student 置 REVOKED 并写审计；不影响课时与报名。</p>
     *
     * @throws BusinessException 404 绑定关系不存在
     */
    @Operation(summary = "解除家长绑定")
    @PostMapping("/bindings/{id}/unbind")
    public ApiResponse<BindingView> unbindBinding(@PathVariable Long id,
                                                  @AuthenticationPrincipal LoginUser operator) {
        return ApiResponse.ok(service.unbindBinding(id, operator));
    }

    /**
     * 审计日志分页。
     *
     * <p>只读；actionType 精确匹配，keyword 匹配内容/操作人。</p>
     */
    @Operation(summary = "登录与变更记录")
    @GetMapping("/audit-logs")
    public ApiResponse<PageResult<AuditRow>> auditLogs(
            @RequestParam(defaultValue = "1") long current,
            @RequestParam(defaultValue = "20") long size,
            @RequestParam(required = false) String actionType,
            @RequestParam(required = false) String keyword) {
        return ApiResponse.ok(service.auditLogs(current, size, actionType, keyword));
    }

    /**
     * 家长下拉（管理者与教务可用）。
     *
     * <p>只读；手机号未脱敏，供教务核对身份。</p>
     * @return 家长选项列表
     */
    @Operation(summary = "家长下拉（教务在作品授权中使用）")
    @PreAuthorize("hasAnyRole('ADMIN','ACADEMIC')")
    @GetMapping("/parent-options")
    public ApiResponse<List<ParentOption>> parentOptions() {
        return ApiResponse.ok(service.parentOptions());
    }

    /**
     * 学员下拉（管理者与教务可用）。
     *
     * <p>只读；仅 ACTIVE 学员。</p>
     * @return 学员选项列表
     */
    @Operation(summary = "学员下拉")
    @PreAuthorize("hasAnyRole('ADMIN','ACADEMIC')")
    @GetMapping("/student-options")
    public ApiResponse<List<StudentOption>> studentOptions() {
        return ApiResponse.ok(service.studentOptions());
    }
}
