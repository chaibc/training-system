package com.hanmo.calligraphy.admin;

import com.hanmo.calligraphy.admin.teacher.TeacherAdminDtos.TeacherRequest;
import com.hanmo.calligraphy.admin.teacher.TeacherAdminDtos.TeacherView;
import com.hanmo.calligraphy.admin.teacher.TeacherAdminService;
import com.hanmo.calligraphy.common.ApiResponse;
import com.hanmo.calligraphy.common.PageResult;
import com.hanmo.calligraphy.security.LoginUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
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
 * 师资团队管理：教师档案 teacher_profile 与教师登录账号 sys_user 的联动维护。
 *
 * <p>业务模块：机构内容（师资）；接口前缀 {@code /api/admin/teachers}。</p>
 * <p>后台页面：管理端「机构内容」(/org，Org.vue) 的师资区块。</p>
 * <p>权限：该前缀在 SecurityConfig 的「写操作仅 ADMIN」清单内 —— GET 允许 ADMIN 与 ACADEMIC，
 * POST/PUT 仅 ADMIN。</p>
 * <p>通道：不比照 BaseCrudController，只有 page/list/create/update/toggle-status/unbind-wechat，
 * 不提供 DELETE。写操作全部由 TeacherAdminService 在同一事务里同时改档案与账号，
 * 并写 sys_audit_log（actionType = 教师建档）。</p>
 * <p>注意：admin-web 的 teacherApi.remove 指向 DELETE /api/admin/teachers/{id}，但本控制器没有该映射，
 * 前端若调用会得到 405（目前页面未使用）。</p>
 */
@Tag(name = "管理端-师资团队")
@RestController
@RequestMapping("/api/admin/teachers")
@RequiredArgsConstructor
public class TeacherProfileController {

    private final TeacherAdminService service;

    /**
     * 教师分页。
     *
     * <p>keyword 同时模糊匹配姓名与擅长书体（styles），status 精确过滤（档案状态 ACTIVE / DISABLED）；
     * 排序为 sort 升序、id 倒序；size 上限 200。返回的 TeacherView 会补出账号手机号、微信绑定状态与账号状态。</p>
     *
     * @param current 页码，从 1 开始
     * @param size    每页条数，缺省 10
     * @param keyword 可选，姓名或书体关键字
     * @param status  可选，档案状态
     */
    @Operation(summary = "教师分页")
    @GetMapping("/page")
    public ApiResponse<PageResult<TeacherView>> page(
            @RequestParam(defaultValue = "1") long current,
            @RequestParam(defaultValue = "10") long size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String status) {
        return ApiResponse.ok(service.page(current, size, keyword, status));
    }

    /**
     * 教师全量列表（含手机号与微信绑定状态），供班级、作品页的教师下拉使用。
     */
    @Operation(summary = "教师列表")
    @GetMapping("/list")
    public ApiResponse<List<TeacherView>> list() {
        return ApiResponse.ok(service.list());
    }

    /**
     * 新增教师：先建 sys_user（userType=TEACHER、角色取 TEACHER、失败次数归零、不写入密码），
     * 再生成 T001 形式的教师编号并插 teacher_profile，最后写审计日志。
     *
     * <p>副作用：写 sys_user、teacher_profile、sys_audit_log 三张表（同一事务）。</p>
     *
     * @param request  教师档案字段（姓名、手机号、职称、教龄、书体、简介、排序、状态等）
     * @param operator 当前登录人，仅用于审计日志；为空时记「系统」
     * @throws com.hanmo.calligraphy.common.BusinessException 400 手机号不是 11 位；500 系统缺少 TEACHER 角色
     */
    @Operation(summary = "新增教师（同时创建教师账号）")
    @PostMapping
    public ApiResponse<TeacherView> create(@Valid @RequestBody TeacherRequest request,
                                           @AuthenticationPrincipal LoginUser operator) {
        return ApiResponse.ok(service.create(request, operator));
    }

    /**
     * 编辑教师：同时更新档案字段与账号的姓名、手机号、状态。
     *
     * <p>副作用：手机号变更时会清空该账号的 wechat_openid / wechat_unionid（原微信绑定失效，需重新绑定），
     * 并写审计日志。</p>
     *
     * @param id       教师档案 ID
     * @param request  教师档案字段
     * @param operator 当前登录人，仅用于审计日志
     * @throws com.hanmo.calligraphy.common.BusinessException 404 教师不存在；400 手机号格式错误
     */
    @Operation(summary = "编辑教师")
    @PutMapping("/{id}")
    public ApiResponse<TeacherView> update(@PathVariable Long id,
                                           @Valid @RequestBody TeacherRequest request,
                                           @AuthenticationPrincipal LoginUser operator) {
        return ApiResponse.ok(service.update(id, request, operator));
    }

    /**
     * 启用 / 停用教师：把档案 status 在 ACTIVE 与 DISABLED 之间翻转，并同步账号 sys_user.status
     * （ENABLED / DISABLED），停用后该教师无法登录教师端；写审计日志。
     *
     * <p>注意判据是「当前状态是否等于 DISABLED」：除 DISABLED 外的一切值都按启用处理再翻转，
     * 因此档案状态若被写成其他字符串，首次调用会把它当成启用而改为 DISABLED。</p>
     *
     * @param id       教师档案 ID
     * @param operator 当前登录人，仅用于审计日志
     * @throws com.hanmo.calligraphy.common.BusinessException 教师不存在（404）
     */
    @Operation(summary = "启用/停用教师")
    @PostMapping("/{id}/toggle-status")
    public ApiResponse<TeacherView> toggle(@PathVariable Long id,
                                           @AuthenticationPrincipal LoginUser operator) {
        return ApiResponse.ok(service.toggle(id, operator));
    }

    /**
     * 解绑教师微信：清空该教师账号的 wechat_openid / wechat_unionid，下次扫码需重新绑定；
     * 不改档案与账号状态，写审计日志。
     *
     * @param id       教师档案 ID
     * @param operator 当前登录人，仅用于审计日志
     * @throws com.hanmo.calligraphy.common.BusinessException 教师不存在（404）
     */
    @Operation(summary = "解绑教师微信")
    @PostMapping("/{id}/unbind-wechat")
    public ApiResponse<TeacherView> unbind(@PathVariable Long id,
                                           @AuthenticationPrincipal LoginUser operator) {
        return ApiResponse.ok(service.unbind(id, operator));
    }
}
