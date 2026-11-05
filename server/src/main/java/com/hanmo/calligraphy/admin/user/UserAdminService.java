package com.hanmo.calligraphy.admin.user;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.hanmo.calligraphy.admin.user.UserAdminDtos.AddBindingRequest;
import com.hanmo.calligraphy.admin.user.UserAdminDtos.AuditRow;
import com.hanmo.calligraphy.admin.user.UserAdminDtos.BindingView;
import com.hanmo.calligraphy.admin.user.UserAdminDtos.CreateUserRequest;
import com.hanmo.calligraphy.admin.user.UserAdminDtos.ParentOption;
import com.hanmo.calligraphy.admin.user.UserAdminDtos.PermissionView;
import com.hanmo.calligraphy.admin.user.UserAdminDtos.ResetPasswordResponse;
import com.hanmo.calligraphy.admin.user.UserAdminDtos.RolePermissionUpdateRequest;
import com.hanmo.calligraphy.admin.user.UserAdminDtos.RoleView;
import com.hanmo.calligraphy.admin.user.UserAdminDtos.StudentOption;
import com.hanmo.calligraphy.admin.user.UserAdminDtos.UpdateUserRequest;
import com.hanmo.calligraphy.admin.user.UserAdminDtos.UserRow;
import com.hanmo.calligraphy.common.BusinessException;
import com.hanmo.calligraphy.common.PageResult;
import com.hanmo.calligraphy.domain.entity.ClassGroup;
import com.hanmo.calligraphy.domain.entity.ClassStudent;
import com.hanmo.calligraphy.domain.entity.ParentStudent;
import com.hanmo.calligraphy.domain.entity.Student;
import com.hanmo.calligraphy.domain.entity.TeacherProfile;
import com.hanmo.calligraphy.domain.mapper.ClassGroupMapper;
import com.hanmo.calligraphy.domain.mapper.ClassStudentMapper;
import com.hanmo.calligraphy.domain.mapper.ParentStudentMapper;
import com.hanmo.calligraphy.domain.mapper.StudentMapper;
import com.hanmo.calligraphy.domain.mapper.TeacherProfileMapper;
import com.hanmo.calligraphy.security.LoginUser;
import com.hanmo.calligraphy.system.entity.ParentProfile;
import com.hanmo.calligraphy.system.entity.SysAuditLog;
import com.hanmo.calligraphy.system.entity.SysRole;
import com.hanmo.calligraphy.system.entity.SysRolePermission;
import com.hanmo.calligraphy.system.entity.SysUser;
import com.hanmo.calligraphy.system.mapper.ParentProfileMapper;
import com.hanmo.calligraphy.system.mapper.SysAuditLogMapper;
import com.hanmo.calligraphy.system.mapper.SysRoleMapper;
import com.hanmo.calligraphy.system.mapper.SysRolePermissionMapper;
import com.hanmo.calligraphy.system.mapper.SysUserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * 用户与权限服务：后台账号管理、角色权限维护、家长-学员绑定审核、审计日志与下拉选项。
 *
 * <p>家长绑定状态机（parent_student）：
 * 新绑定 PENDING →（通过）ACTIVE /（驳回）REJECTED；
 * 解绑申请不新增行，而是把 ACTIVE 行的 source 改为 UNBIND 进入待审核，
 * 通过后 status=REVOKED 且 is_primary=0，驳回则恢复 ACTIVE 并把 source 改回 SELF。
 * 约束：一个学员最多 2 位 ACTIVE 监护人、最多 1 位主联系人。</p>
 *
 * <p>权限数据范围（sys_role.data_scope：ALL/OWN_CLASS/OWN_CHILDREN/SELF）只在本服务落库，
 * 并随登录响应下发；后台各业务查询目前没有按它过滤，实际可见范围由角色模块权限 + 前端控制。</p>
 *
 * <p>依赖表：sys_user、sys_role、sys_role_permission、sys_audit_log、parent_profile、
 * parent_student、student、teacher_profile、class_group、class_student。</p>
 */
@Service
@RequiredArgsConstructor
public class UserAdminService {

    private static final List<String> MODULES = List.of(
            "dashboard", "reports", "students", "enrollments", "courses", "classes", "attendance",
            "leave", "works", "org", "basic", "settings", "users");

    private final SysUserMapper userMapper;
    private final SysRoleMapper roleMapper;
    private final SysRolePermissionMapper permissionMapper;
    private final SysAuditLogMapper auditLogMapper;
    private final ParentProfileMapper parentProfileMapper;
    private final ParentStudentMapper parentStudentMapper;
    private final StudentMapper studentMapper;
    private final TeacherProfileMapper teacherProfileMapper;
    private final ClassGroupMapper classGroupMapper;
    private final ClassStudentMapper classStudentMapper;
    private final PasswordEncoder passwordEncoder;

    /**
     * 账号分页。
     *
     * <p>纯查询。keyword 匹配姓名/用户名/手机号；type 过滤 ADMIN/TEACHER/PARENT；
     * 按 role_id、id 升序，保证管理者排在前面。每页上限 200。</p>
     *
     * @param current 页码（最小 1）
     * @param size    每页条数（1~200）
     * @param type    账号类型，可为空
     * @param status  ENABLED/DISABLED/LOCKED，可为空
     * @param keyword 姓名/用户名/手机号，可为空
     * @return 账号行分页（手机号脱敏，含角色与关联信息）
     */
    public PageResult<UserRow> pageUsers(long current, long size, String type, String status, String keyword) {
        Page<SysUser> page = new Page<>(Math.max(1, current), Math.min(Math.max(1, size), 200));
        LambdaQueryWrapper<SysUser> qw = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(type)) qw.eq(SysUser::getUserType, type);
        if (StringUtils.hasText(status)) qw.eq(SysUser::getStatus, status);
        if (StringUtils.hasText(keyword)) {
            qw.and(w -> w.like(SysUser::getName, keyword)
                    .or().like(SysUser::getUsername, keyword)
                    .or().like(SysUser::getPhone, keyword));
        }
        qw.orderByAsc(SysUser::getRoleId).orderByAsc(SysUser::getId);
        userMapper.selectPage(page, qw);
        // 先一次性取角色字典再逐行映射，避免每条账号都查一次 sys_role
        Map<Long, SysRole> roles = roleMapper.selectList(null).stream()
                .collect(Collectors.toMap(SysRole::getId, r -> r, (a, b) -> a));
        List<UserRow> rows = page.getRecords().stream()
                .map(u -> toRow(u, roles.get(u.getRoleId()), null))
                .toList();
        return PageResult.of(rows, page.getTotal(), page.getCurrent(), page.getSize());
    }

    /**
     * 新建后台账号（教务）。
     *
     * <p>校验：用户名唯一、手机号 11 位；角色固定取内置 ACADEMIC（管理者唯一，不在本接口创建）。
     * 密码未填时随机生成「Hm@ + 6 位」，并置 force_change_password=1 强制首登修改。</p>
     *
     * <p>副作用：写 sys_user 一行、写审计日志；返回体里带明文初始密码（仅此一次）。
     * 事务边界：@Transactional 覆盖。</p>
     *
     * @param request  姓名、用户名、手机号（均必填）、可选密码
     * @param operator 操作人，用于审计
     * @return 新建账号行，initialPassword 为初始密码
     * @throws BusinessException 400 用户名已存在或手机号格式错误；500 系统缺少 ACADEMIC 角色
     */
    @Transactional
    public UserRow createUser(CreateUserRequest request, LoginUser operator) {
        // 用户名是登录凭据，数据库有唯一键；先查一次给出友好提示，避免直接抛 DuplicateKey
        if (userMapper.selectCount(new LambdaQueryWrapper<SysUser>().eq(SysUser::getUsername, request.username())) > 0) {
            throw new BusinessException(400, "用户名已存在");
        }
        if (!request.phone().matches("^1\\d{10}$")) {
            throw new BusinessException(400, "请填写正确的 11 位手机号");
        }
        SysRole role = roleMapper.selectOne(new LambdaQueryWrapper<SysRole>()
                .eq(SysRole::getCode, "ACADEMIC").last("limit 1"));
        if (role == null) throw new BusinessException(500, "系统缺少教务角色");
        // 未填密码时生成随机初始密码；force_change_password=1 要求首登修改
        String password = StringUtils.hasText(request.password())
                ? request.password() : "Hm@" + UUID.randomUUID().toString().replace("-", "").substring(0, 6);
        SysUser user = new SysUser();
        user.setUsername(request.username().trim());
        user.setPasswordHash(passwordEncoder.encode(password));
        // 后台账号统一 user_type=ADMIN、角色教务；管理者唯一，不通过本接口创建
        user.setUserType("ADMIN");
        user.setRoleId(role.getId());
        user.setName(request.name().trim());
        user.setPhone(request.phone().trim());
        user.setStatus("ENABLED");
        user.setFailedAttempts(0);
        user.setForceChangePassword(1);
        userMapper.insert(user);
        audit(operator, "账号变更", "后台账号", user.getId(), "创建教务账号 " + user.getUsername());
        return toRow(user, role, password);
    }

    /**
     * 编辑账号资料（姓名/用户名/手机号）。
     *
     * <p>副作用：更新 sys_user；非 ADMIN 账号换手机号时清空 wechat_openid/unionid（旧微信绑定作废）；
     * 若该账号是家长，同步 parent_profile 的姓名与手机号；写审计日志。@Transactional 覆盖。</p>
     *
     * @param id      账号 ID
     * @param request 待改字段，null/空串表示不改
     * @param operator 操作人，用于审计
     * @return 更新后的账号行
     * @throws BusinessException 404 账号不存在；400 用户名重复或手机号格式错误
     */
    @Transactional
    public UserRow updateUser(Long id, UpdateUserRequest request, LoginUser operator) {
        SysUser user = requireUser(id);
        String oldPhone = user.getPhone();
        if (StringUtils.hasText(request.name())) user.setName(request.name().trim());
        if (StringUtils.hasText(request.username()) && !request.username().equals(user.getUsername())) {
            if (userMapper.selectCount(new LambdaQueryWrapper<SysUser>()
                    .eq(SysUser::getUsername, request.username()).ne(SysUser::getId, id)) > 0) {
                throw new BusinessException(400, "用户名已存在");
            }
            user.setUsername(request.username().trim());
        }
        if (StringUtils.hasText(request.phone())) {
            if (!request.phone().matches("^1\\d{10}$")) throw new BusinessException(400, "请填写正确的 11 位手机号");
            user.setPhone(request.phone().trim());
        }
        userMapper.updateById(user);
        // 教师/家长用手机号+微信登录，换手机号必须解绑旧微信，防止串号
        if (!"ADMIN".equals(user.getUserType()) && StringUtils.hasText(request.phone())
                && !request.phone().equals(oldPhone)) {
            userMapper.update(null, new LambdaUpdateWrapper<SysUser>()
                    .eq(SysUser::getId, id)
                    .set(SysUser::getWechatOpenid, null)
                    .set(SysUser::getWechatUnionid, null));
            audit(operator, "账号变更", "账号", id, "手机号变更，已解除微信绑定");
        }
        // 家长档案与账号冗余了姓名/手机号，这里同步，避免两处不一致
        if ("PARENT".equals(user.getUserType())) {
            ParentProfile profile = parentProfileMapper.selectOne(new LambdaQueryWrapper<ParentProfile>()
                    .eq(ParentProfile::getUserId, id).last("limit 1"));
            if (profile != null) {
                profile.setName(user.getName());
                profile.setPhone(user.getPhone());
                parentProfileMapper.updateById(profile);
            }
        }
        audit(operator, "账号变更", "账号", id, "编辑账号资料");
        return toRow(requireUser(id), roleMapper.selectById(user.getRoleId()), null);
    }

    /**
     * 重置后台账号密码。
     *
     * <p>只允许 ADMIN 类型账号；随机生成「Hm@ + 6 位」并 BCrypt 加密，同时清空失败次数与锁定时间、
     * 置强制改密。副作用：更新 sys_user、写审计日志；返回明文新密码。@Transactional 覆盖。</p>
     *
     * @param id       账号 ID
     * @param operator 操作人，用于审计
     * @return 新密码（仅本次返回）
     * @throws BusinessException 404 账号不存在；400 教师/家长账号使用微信登录，无需重置密码
     */
    @Transactional
    public ResetPasswordResponse resetPassword(Long id, LoginUser operator) {
        SysUser user = requireUser(id);
        // 只有后台账号有密码；教师/家长走微信登录，无密码可重置
        if (!"ADMIN".equals(user.getUserType())) {
            throw new BusinessException(400, "教师/家长账号使用微信登录，无需重置密码");
        }
        String password = "Hm@" + UUID.randomUUID().toString().replace("-", "").substring(0, 6);
        user.setPasswordHash(passwordEncoder.encode(password));
        user.setForceChangePassword(1);
        user.setFailedAttempts(0);
        user.setLockedUntil(null);
        userMapper.updateById(user);
        audit(operator, "账号变更", "密码", id, "重置密码，首次登录强制修改");
        return new ResetPasswordResponse(password);
    }

    /**
     * 启用/停用账号。
     *
     * <p>管理者角色（role.code=ADMIN）唯一，禁止停用。副作用：切换 sys_user.status 并写审计；
     * 事务边界：@Transactional 覆盖。</p>
     *
     * @param id       账号 ID
     * @param operator 操作人，用于审计
     * @return 切换后的账号行
     * @throws BusinessException 404 账号不存在；400 管理者账号不可停用
     */
    @Transactional
    public UserRow toggleStatus(Long id, LoginUser operator) {
        SysUser user = requireUser(id);
        SysRole role = roleMapper.selectById(user.getRoleId());
        // 内置管理者角色唯一：一旦停用将无人可管理系统，直接禁止
        if (role != null && "ADMIN".equals(role.getCode())) {
            throw new BusinessException(400, "管理者账号唯一，不可停用");
        }
        user.setStatus("ENABLED".equals(user.getStatus()) ? "DISABLED" : "ENABLED");
        userMapper.updateById(user);
        audit(operator, "账号变更", "账号", id, user.getStatus().equals("DISABLED") ? "停用账号" : "启用账号");
        return toRow(user, role, null);
    }

    /**
     * 解绑账号微信。
     *
     * <p>副作用：清空 sys_user.wechat_openid/unionid 并写审计；不改账号状态。
     * 后台账号不使用微信登录，直接拒绝。@Transactional 覆盖。</p>
     *
     * @param id       账号 ID
     * @param operator 操作人，用于审计
     * @return 解绑后的账号行
     * @throws BusinessException 404 账号不存在；400 后台账号不使用微信登录
     */
    @Transactional
    public UserRow unbindWechat(Long id, LoginUser operator) {
        SysUser user = requireUser(id);
        // 后台账号没有 openid，解绑操作没有意义
        if ("ADMIN".equals(user.getUserType())) throw new BusinessException(400, "后台账号不使用微信登录");
        userMapper.update(null, new LambdaUpdateWrapper<SysUser>()
                .eq(SysUser::getId, id)
                .set(SysUser::getWechatOpenid, null)
                .set(SysUser::getWechatUnionid, null));
        audit(operator, "账号变更", "账号", id, "解绑微信");
        return toRow(requireUser(id), roleMapper.selectById(user.getRoleId()), null);
    }

    /**
     * 角色列表（含每个角色的模块权限与账号数）。
     *
     * <p>纯查询；权限位从 sys_role_permission 读出后转 boolean。供角色权限配置页初始化。</p>
     *
     * @return 角色视图列表，按 id 升序
     */
    public List<RoleView> roles() {
        List<SysRole> roles = roleMapper.selectList(new LambdaQueryWrapper<SysRole>().orderByAsc(SysRole::getId));
        List<RoleView> result = new ArrayList<>();
        for (SysRole role : roles) {
            long count = userMapper.selectCount(new LambdaQueryWrapper<SysUser>().eq(SysUser::getRoleId, role.getId()));
            List<PermissionView> permissions = permissionMapper.selectList(
                            new LambdaQueryWrapper<SysRolePermission>().eq(SysRolePermission::getRoleId, role.getId()))
                    .stream().map(p -> new PermissionView(p.getModuleKey(),
                            isOne(p.getCanView()), isOne(p.getCanCreate()), isOne(p.getCanEdit()),
                            isOne(p.getCanDelete()), isOne(p.getCanSpecial()))).toList();
            result.add(new RoleView(role.getId(), role.getCode(), role.getName(), role.getDescription(),
                    role.getDataScope(), count, permissions));
        }
        return result;
    }

    /**
     * 更新角色的数据范围与模块权限。
     *
     * <p>管理者/教师/家长角色不可改。副作用（@Transactional 内）：更新 sys_role.data_scope；
     * 删除该角色全部 sys_role_permission 后按请求重建（moduleKey 不在白名单的直接丢弃）；
     * 写审计日志。更新完成后返回最新角色列表。</p>
     *
     * @param roleId  角色 ID
     * @param request dataScope（可空，空则不改）与权限项列表
     * @param operator 操作人，用于审计
     * @return 更新后的角色列表
     * @throws BusinessException 404 角色不存在；400 内置管理者/教师/家长角色不可修改
     */
    @Transactional
    public List<RoleView> updateRolePermissions(Long roleId, RolePermissionUpdateRequest request, LoginUser operator) {
        SysRole role = roleMapper.selectById(roleId);
        if (role == null) throw new BusinessException(404, "角色不存在");
        if ("ADMIN".equals(role.getCode())) throw new BusinessException(400, "管理者权限不可修改");
        // 教师/家长是小程序身份，权限不来自 sys_role_permission，禁止在后台改
        if ("TEACHER".equals(role.getCode()) || "PARENT".equals(role.getCode())) {
            throw new BusinessException(400, "教师/家长为小程序身份，权限不可在后台修改");
        }
        // 数据范围只落库并随登录下发，服务端查询并未据此过滤（前端按它做展示控制）
        if (StringUtils.hasText(request.dataScope())) role.setDataScope(request.dataScope());
        roleMapper.updateById(role);
        permissionMapper.delete(new LambdaQueryWrapper<SysRolePermission>().eq(SysRolePermission::getRoleId, roleId));
        if (request.permissions() != null) {
            for (PermissionView p : request.permissions()) {
                // 模块 key 白名单过滤：防止前端传入不存在的模块写入脏数据
                if (!MODULES.contains(p.moduleKey())) continue;
                SysRolePermission entity = new SysRolePermission();
                entity.setRoleId(roleId);
                entity.setModuleKey(p.moduleKey());
                entity.setCanView(p.canView() ? 1 : 0);
                entity.setCanCreate(p.canCreate() ? 1 : 0);
                entity.setCanEdit(p.canEdit() ? 1 : 0);
                entity.setCanDelete(p.canDelete() ? 1 : 0);
                entity.setCanSpecial(p.canSpecial() ? 1 : 0);
                permissionMapper.insert(entity);
            }
        }
        audit(operator, "账号变更", "角色", roleId, "更新角色权限 " + role.getName());
        return roles();
    }

    /**
     * 家长-学员绑定列表。
     *
     * <p>纯查询，但「待审核」是两类记录的并集：status=PENDING 的新绑定申请 +
     * status=ACTIVE 且 source=UNBIND 的解绑申请；「已绑定」则排除 source=UNBIND，
     * 避免正在审核解绑的关系还显示在有效列表里。其它状态按 status 精确过滤。</p>
     *
     * @param status PENDING（待审核）/ACTIVE（已绑定）/REJECTED/REVOKED
     * @return 绑定视图列表，按 id 倒序
     */
    public List<BindingView> bindings(String status) {
        List<ParentStudent> list;
        // 待审核列表 = 新绑定申请（status=PENDING）+ 解绑申请（status=ACTIVE 且 source=UNBIND）
        // 用 LinkedHashMap 按 id 合并去重，再统一倒序，避免两类记录交错
        if ("PENDING".equals(status)) {
            // 待审核 = 新绑定申请 + 家长解绑申请（解绑审核期间绑定仍为 ACTIVE）
            Map<Long, ParentStudent> merged = new LinkedHashMap<>();
            parentStudentMapper.selectList(new LambdaQueryWrapper<ParentStudent>()
                            .eq(ParentStudent::getStatus, "PENDING").orderByDesc(ParentStudent::getId))
                    .forEach(b -> merged.put(b.getId(), b));
            parentStudentMapper.selectList(new LambdaQueryWrapper<ParentStudent>()
                            .eq(ParentStudent::getStatus, "ACTIVE")
                            .eq(ParentStudent::getSource, "UNBIND").orderByDesc(ParentStudent::getId))
                    .forEach(b -> merged.put(b.getId(), b));
            list = new ArrayList<>(merged.values());
            // 合并后重新按 id 倒序：最新的申请排最前
            list.sort((a, b) -> Long.compare(b.getId(), a.getId()));
        } else if ("ACTIVE".equals(status)) {
            list = parentStudentMapper.selectList(new LambdaQueryWrapper<ParentStudent>()
                    .eq(ParentStudent::getStatus, "ACTIVE")
                    .ne(ParentStudent::getSource, "UNBIND")
                    .orderByDesc(ParentStudent::getId));
        } else {
            list = parentStudentMapper.selectList(new LambdaQueryWrapper<ParentStudent>()
                    .eq(ParentStudent::getStatus, status)
                    .orderByDesc(ParentStudent::getId));
        }
        return list.stream().map(this::toBinding).toList();
    }

    /**
     * 通过绑定/解绑申请。
     *
     * <p>分支：source=UNBIND 视为解绑申请 → status=REVOKED、is_primary=0；
     * 否则视为新绑定 → 先校验该学员 ACTIVE 监护人是否已满 2 人，通过后 status=ACTIVE；
     * 若申请为主联系人，先把该学员其它绑定的 is_primary 全部清 0。</p>
     *
     * <p>副作用：更新 parent_student（status/is_primary/verified_by/verified_at）并写审计日志；
     * 事务边界：@Transactional 覆盖。不检查当前状态，重复调用会重复写审计。</p>
     *
     * @param id       绑定 ID
     * @param operator 审核人，写入 verified_by
     * @return 审核后的绑定视图
     * @throws BusinessException 404 绑定不存在；400 学员已有 2 位有效监护人
     */
    @Transactional
    public BindingView approveBinding(Long id, LoginUser operator) {
        ParentStudent binding = parentStudentMapper.selectById(id);
        if (binding == null) throw new BusinessException(404, "绑定申请不存在");
        boolean unbind = "UNBIND".equals(binding.getSource());
        // 状态机前置校验：新绑定只有 PENDING 可审，解绑申请只有 ACTIVE 可审；
        // 否则重复点击会把已生效/已驳回的单子再次改写，并重复写审计
        if (unbind ? !"ACTIVE".equals(binding.getStatus()) : !"PENDING".equals(binding.getStatus())) {
            throw new BusinessException(400, "该申请已处理，请刷新列表");
        }
        if (!unbind) {
            long activeGuardians = parentStudentMapper.selectCount(new LambdaQueryWrapper<ParentStudent>()
                    .eq(ParentStudent::getStudentId, binding.getStudentId())
                    .eq(ParentStudent::getStatus, "ACTIVE")
                    .ne(ParentStudent::getId, binding.getId()));
            // 一学员最多 2 位 ACTIVE 监护人：统计时排除当前这条（它此时仍是 PENDING）
            if (activeGuardians >= 2) {
                throw new BusinessException(400, "该学员已有 2 位有效监护人，不能继续通过绑定");
            }
        }
        // 主联系人唯一：先清空该学员所有绑定的 is_primary，下面 updateById 再把本条写回 1
        if (!unbind && binding.getIsPrimary() != null && binding.getIsPrimary() == 1) {
            parentStudentMapper.update(null, new LambdaUpdateWrapper<ParentStudent>()
                    .eq(ParentStudent::getStudentId, binding.getStudentId())
                    .set(ParentStudent::getIsPrimary, 0));
        }
        // 解绑通过：关系置 REVOKED 且取消主联系人
        if (unbind) {
            binding.setStatus("REVOKED");
            binding.setIsPrimary(0);
        } else {
            binding.setStatus("ACTIVE");
        }
        binding.setVerifiedBy(operator.getId());
        binding.setVerifiedAt(LocalDateTime.now());
        parentStudentMapper.updateById(binding);
        // 主联系人对齐：解绑可能把主联系人置空，这里补选一位有效监护人并回写学员档案
        syncPrimaryParent(binding.getStudentId());
        audit(operator, "绑定审核", "家长-学员", binding.getId(), unbind ? "通过家长解绑申请" : "通过家长绑定");
        return toBinding(binding);
    }

    /**
     * 驳回绑定/解绑申请。
     *
     * <p>分支：解绑申请 → 恢复 ACTIVE 且 source 改回 SELF（关系继续有效）；
     * 新绑定申请 → status=REJECTED。副作用：更新 parent_student 与审核人/时间，写审计日志。
     * 事务边界：@Transactional 覆盖。</p>
     *
     * @param id       绑定 ID
     * @param operator 审核人
     * @return 驳回后的绑定视图
     * @throws BusinessException 404 绑定不存在
     */
    @Transactional
    public BindingView rejectBinding(Long id, LoginUser operator) {
        ParentStudent binding = parentStudentMapper.selectById(id);
        if (binding == null) throw new BusinessException(404, "绑定申请不存在");
        boolean unbind = "UNBIND".equals(binding.getSource());
        // 与通过接口同一套状态机校验：已处理的单子不允许再驳回/恢复
        if (unbind ? !"ACTIVE".equals(binding.getStatus()) : !"PENDING".equals(binding.getStatus())) {
            throw new BusinessException(400, "该申请已处理，请刷新列表");
        }
        binding.setStatus(unbind ? "ACTIVE" : "REJECTED");
        // 驳回解绑：关系恢复 ACTIVE，source 改回 SELF，否则会一直留在待审核列表
        if (unbind) binding.setSource("SELF");
        binding.setVerifiedBy(operator.getId());
        binding.setVerifiedAt(LocalDateTime.now());
        parentStudentMapper.updateById(binding);
        // 驳回解绑会恢复 ACTIVE，可能产生「没有主联系人」的状态，这里统一对齐一次
        syncPrimaryParent(binding.getStudentId());
        audit(operator, "绑定审核", "家长-学员", binding.getId(), unbind ? "驳回家长解绑申请" : "驳回家长绑定");
        return toBinding(binding);
    }

    /**
     * 管理员直接新增绑定（跳过家长申请）。
     *
     * <p>校验：家长账号 user_type=PARENT、学员存在、同一对家长-学员不能已有 ACTIVE/PENDING 绑定、
     * 学员 ACTIVE 监护人不足 2 人。若存在历史 REJECTED/REVOKED 行则复用该行，避免触发唯一键冲突。</p>
     *
     * <p>副作用：insert 或 update parent_student（status=ACTIVE、source=ADMIN、记录审核人时间）；
     * primary=true 时先清空该学员其它绑定的 is_primary；写审计日志。@Transactional 覆盖。</p>
     *
     * @param request 家长 ID、学员 ID（必填）、关系、是否主联系人
     * @param operator 操作人，写入 verified_by
     * @return 新增后的绑定视图
     * @throws BusinessException 404 账号/学员不存在；400 非家长账号、已有有效绑定、监护人数已满
     */
    @Transactional
    public BindingView addBinding(AddBindingRequest request, LoginUser operator) {
        SysUser parent = requireUser(request.parentUserId());
        if (!"PARENT".equals(parent.getUserType())) throw new BusinessException(400, "请选择家长账号");
        Student student = studentMapper.selectById(request.studentId());
        if (student == null) throw new BusinessException(404, "学员不存在");
        ParentStudent exists = parentStudentMapper.selectOne(new LambdaQueryWrapper<ParentStudent>()
                .eq(ParentStudent::getParentUserId, parent.getId())
                .eq(ParentStudent::getStudentId, student.getId()).last("limit 1"));
        // 已有有效或待审核绑定则不再新增；表上有 (parent_user_id, student_id) 唯一键
        if (exists != null && List.of("ACTIVE", "PENDING").contains(exists.getStatus())) {
            throw new BusinessException(400, "该家长与学员已存在有效或待审核绑定，请直接审核或解绑");
        }
        long active = parentStudentMapper.selectCount(new LambdaQueryWrapper<ParentStudent>()
                .eq(ParentStudent::getStudentId, student.getId())
                .eq(ParentStudent::getStatus, "ACTIVE"));
        // 与审核入口一致的上限：一个学员最多 2 位 ACTIVE 监护人
        if (active >= 2) throw new BusinessException(400, "一个学员最多 2 个有效监护人");
        // 主联系人唯一：设置为 true 时先清空同学生其它关系的主联系人标记
        boolean primary = Boolean.TRUE.equals(request.primary());
        if (primary) {
            parentStudentMapper.update(null, new LambdaUpdateWrapper<ParentStudent>()
                    .eq(ParentStudent::getStudentId, student.getId())
                    .set(ParentStudent::getIsPrimary, 0));
        }
        ParentStudent binding = exists == null ? new ParentStudent() : exists;
        binding.setParentUserId(parent.getId());
        binding.setStudentId(student.getId());
        binding.setRelation(StringUtils.hasText(request.relation()) ? request.relation() : "OTHER");
        binding.setIsPrimary(primary ? 1 : 0);
        binding.setStatus("ACTIVE");
        binding.setSource("ADMIN");
        binding.setVerifiedBy(operator.getId());
        binding.setVerifiedAt(LocalDateTime.now());
        // 复用历史 REJECTED/REVOKED 行（唯一键限制不能新插），没有历史才 insert
        if (binding.getId() == null) parentStudentMapper.insert(binding);
        else parentStudentMapper.updateById(binding);
        // 新增绑定后对齐主联系人：学员原来没有主家长时，本条会成为主联系人
        syncPrimaryParent(student.getId());
        audit(operator, "绑定审核", "家长-学员", binding.getId(), exists == null ? "新增家长绑定" : "恢复家长绑定");
        return toBinding(binding);
    }

    /**
     * 管理员直接解除绑定。
     *
     * <p>副作用：parent_student.status 置 REVOKED 并写审计；不改学员档案、不影响课时与报名，
     * 也不清理 student.primary_parent_user_id（见报告中的疑点）。@Transactional 覆盖。</p>
     *
     * @param id       绑定 ID
     * @param operator 操作人
     * @return 解除后的绑定视图
     * @throws BusinessException 404 绑定关系不存在
     */
    @Transactional
    public BindingView unbindBinding(Long id, LoginUser operator) {
        ParentStudent binding = parentStudentMapper.selectById(id);
        if (binding == null) throw new BusinessException(404, "绑定关系不存在");
        // 只有生效中的关系才能解除；待审核/已驳回/已解除的单子直接拒绝，避免误操作
        if (!"ACTIVE".equals(binding.getStatus())) {
            throw new BusinessException(400, "只有生效中的绑定才能解除");
        }
        // 管理员直接解除：置 REVOKED；source 保留原值，便于追溯是自绑还是管理员加的
        binding.setStatus("REVOKED");
        binding.setIsPrimary(0);
        parentStudentMapper.updateById(binding);
        // 解除的若是主联系人，自动把另一位有效监护人设为主联系人（没有则清空学员上的主家长）
        syncPrimaryParent(binding.getStudentId());
        audit(operator, "绑定审核", "家长-学员", binding.getId(), "解除家长绑定");
        return toBinding(binding);
    }

    /**
     * 主联系人对齐：保证 parent_student.is_primary 与 student.primary_parent_user_id 指向同一位家长。
     *
     * <p>规则：若该学员仍有 ACTIVE 监护人，取「已是主联系人的那条」，否则取 id 最小的一条设为主联系人，
     * 并把它回写到 student.primary_parent_user_id；若已无有效监护人，则清空学员上的主家长字段。</p>
     *
     * <p>副作用：可能更新 parent_student.is_primary 与 student.primary_parent_user_id，无审计。</p>
     *
     * @param studentId 学员 ID
     */
    private void syncPrimaryParent(Long studentId) {
        List<ParentStudent> actives = parentStudentMapper.selectList(new LambdaQueryWrapper<ParentStudent>()
                .eq(ParentStudent::getStudentId, studentId)
                .eq(ParentStudent::getStatus, "ACTIVE")
                .orderByAsc(ParentStudent::getId));
        com.hanmo.calligraphy.domain.entity.Student student = studentMapper.selectById(studentId);
        if (actives.isEmpty()) {
            // 没有有效监护人：清空学员档案上的主家长，避免报名单继续指向已解绑家长。
            // 注意必须用 UpdateWrapper 显式 set null：updateById 默认忽略 null 字段，写了也不会生效。
            if (student != null && student.getPrimaryParentUserId() != null) {
                studentMapper.update(null, new LambdaUpdateWrapper<com.hanmo.calligraphy.domain.entity.Student>()
                        .eq(com.hanmo.calligraphy.domain.entity.Student::getId, studentId)
                        .set(com.hanmo.calligraphy.domain.entity.Student::getPrimaryParentUserId, null));
            }
            return;
        }
        ParentStudent primary = actives.stream()
                .filter(b -> Integer.valueOf(1).equals(b.getIsPrimary()))
                .findFirst().orElse(actives.get(0));
        if (!Integer.valueOf(1).equals(primary.getIsPrimary())) {
            // 主联系人唯一：先清空该学员所有关系，再写回选中的这条
            parentStudentMapper.update(null, new LambdaUpdateWrapper<ParentStudent>()
                    .eq(ParentStudent::getStudentId, studentId)
                    .set(ParentStudent::getIsPrimary, 0));
            primary.setIsPrimary(1);
            parentStudentMapper.updateById(primary);
        }
        if (student != null && !primary.getParentUserId().equals(student.getPrimaryParentUserId())) {
            student.setPrimaryParentUserId(primary.getParentUserId());
            studentMapper.updateById(student);
        }
    }

    /**
     * 审计日志分页。
     *
     * <p>纯查询；actionType 精确过滤，keyword 匹配内容或操作人姓名；按 id 倒序。
     * 没有时间范围参数，只能翻页。每页上限 200。</p>
     *
     * @param current    页码（最小 1）
     * @param size       每页条数（1~200）
     * @param actionType 动作类型（库里存的是中文动作名），可为空
     * @param keyword    内容/操作人关键字，可为空
     * @return 审计行分页
     */
    public PageResult<AuditRow> auditLogs(long current, long size, String actionType, String keyword) {
        Page<SysAuditLog> page = new Page<>(Math.max(1, current), Math.min(Math.max(1, size), 200));
        LambdaQueryWrapper<SysAuditLog> qw = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(actionType)) qw.eq(SysAuditLog::getActionType, actionType);
        if (StringUtils.hasText(keyword)) {
            qw.and(w -> w.like(SysAuditLog::getContent, keyword)
                    .or().like(SysAuditLog::getOperatorName, keyword));
        }
        qw.orderByDesc(SysAuditLog::getId);
        auditLogMapper.selectPage(page, qw);
        List<AuditRow> rows = page.getRecords().stream()
                .map(a -> new AuditRow(a.getId(), a.getOperatorName(), a.getActionType(),
                        a.getTargetType(), a.getContent(), a.getIp(), a.getResult(), a.getCreatedAt()))
                .toList();
        return PageResult.of(rows, page.getTotal(), page.getCurrent(), page.getSize());
    }

    /**
     * 家长下拉（作品授权、绑定表单用）。
     *
     * <p>纯查询；只取 user_type=PARENT 且状态 ENABLED 的账号，按 id 倒序。
     * 返回的 phone 未脱敏，供教务核对身份。</p>
     *
     * @return 家长选项列表
     */
    public List<ParentOption> parentOptions() {
        return userMapper.selectList(new LambdaQueryWrapper<SysUser>()
                        .eq(SysUser::getUserType, "PARENT")
                        .eq(SysUser::getStatus, "ENABLED")
                        .orderByDesc(SysUser::getId))
                .stream().map(u -> new ParentOption(u.getId(), u.getName(), u.getPhone())).toList();
    }

    /**
     * 学员下拉。
     *
     * <p>纯查询；只取 ACTIVE 学员，按 id 倒序。</p>
     *
     * @return 学员选项列表
     */
    public List<StudentOption> studentOptions() {
        return studentMapper.selectList(new LambdaQueryWrapper<Student>()
                        .eq(Student::getStatus, "ACTIVE")
                        .orderByDesc(Student::getId))
                .stream().map(s -> new StudentOption(s.getId(), s.getName(), s.getStudentNo())).toList();
    }

    private UserRow toRow(SysUser user, SysRole role, String initialPassword) {
        String roleCode = role == null ? null : role.getCode();
        // 管理者标记：用于前端禁用停用/改权限等操作，也决定 canDisable
        boolean manager = "ADMIN".equals(roleCode);
        String related = "";
        if ("TEACHER".equals(user.getUserType())) {
            TeacherProfile teacher = teacherProfileMapper.selectOne(new LambdaQueryWrapper<TeacherProfile>()
                    .eq(TeacherProfile::getUserId, user.getId()).last("limit 1"));
            if (teacher != null) {
                List<ClassGroup> classes = classGroupMapper.selectList(new LambdaQueryWrapper<ClassGroup>()
                        .eq(ClassGroup::getTeacherId, teacher.getId()));
                long students = 0;
                if (!classes.isEmpty()) {
                    List<Long> ids = classes.stream().map(ClassGroup::getId).toList();
                    students = classStudentMapper.selectCount(new LambdaQueryWrapper<ClassStudent>()
                            .in(ClassStudent::getClassId, ids));
                }
                related = "带班 " + classes.size() + " · 学员 " + students;
            }
        } else if ("PARENT".equals(user.getUserType())) {
            List<ParentStudent> bindings = parentStudentMapper.selectList(new LambdaQueryWrapper<ParentStudent>()
                    .eq(ParentStudent::getParentUserId, user.getId())
                    .eq(ParentStudent::getStatus, "ACTIVE"));
            List<Long> studentIds = bindings.stream().map(ParentStudent::getStudentId).toList();
            String names = "";
            if (!studentIds.isEmpty()) {
                names = studentMapper.selectBatchIds(studentIds).stream().map(Student::getName)
                        .collect(Collectors.joining(" / "));
            }
            long pending = parentStudentMapper.selectCount(new LambdaQueryWrapper<ParentStudent>()
                    .eq(ParentStudent::getParentUserId, user.getId())
                    .eq(ParentStudent::getStatus, "PENDING"));
            related = "子女 " + bindings.size() + (StringUtils.hasText(names) ? "：" + names : "")
                    + (pending > 0 ? " · 待审核 " + pending : "");
        } else {
            // 两个分支当前文案相同（管理者也显示「全机构」），保留三元只是占位，后续可按范围细分
            related = manager ? "全机构" : "全机构";
        }
        return new UserRow(user.getId(), user.getName(), user.getUsername(), user.getUserType(),
                roleCode, role == null ? null : role.getName(), maskPhone(user.getPhone()),
                StringUtils.hasText(user.getWechatOpenid()), user.getStatus(), user.getLastLoginAt(),
                related, !manager, manager, initialPassword);
    }

    // 绑定视图：家长/学员可能已被删除，用「未知家长/未知学员」兜底；手机号脱敏
    private BindingView toBinding(ParentStudent binding) {
        SysUser parent = userMapper.selectById(binding.getParentUserId());
        Student student = studentMapper.selectById(binding.getStudentId());
        String relation = binding.getRelation();
        String relationText = switch (relation == null ? "" : relation) {
            case "MOTHER" -> "母亲";
            case "FATHER" -> "父亲";
            case "GRANDPARENT" -> "祖父母";
            default -> "其他监护人";
        };
        // 解绑申请与普通绑定展示不同的核对文案
        String verify = "UNBIND".equals(binding.getSource())
                ? "家长申请解绑，通过后将解除关系"
                : student == null ? "" : "学员编号 " + student.getStudentNo()
                + (parent != null && parent.getPhone() != null && parent.getPhone().length() >= 4
                ? " / 手机尾号 " + parent.getPhone().substring(parent.getPhone().length() - 4) : "");
        return new BindingView(binding.getId(), binding.getParentUserId(),
                parent == null ? "未知家长" : parent.getName(),
                parent == null ? "" : maskPhone(parent.getPhone()),
                binding.getStudentId(), student == null ? "未知学员" : student.getName(),
                student == null ? "" : student.getStudentNo(), relation, relationText,
                binding.getIsPrimary() != null && binding.getIsPrimary() == 1,
                binding.getStatus(), binding.getSource(), binding.getCreatedAt(), verify);
    }

    // 账号不存在统一抛 404
    private SysUser requireUser(Long id) {
        SysUser user = userMapper.selectById(id);
        if (user == null) throw new BusinessException(404, "账号不存在");
        return user;
    }

    // 权限位是 TINYINT，转 boolean 时容忍 null（按无权限处理）
    private boolean isOne(Integer v) {
        return v != null && v == 1;
    }

    // 手机号脱敏：保留前 3 位与后 4 位
    private String maskPhone(String phone) {
        if (phone == null || phone.length() < 7) return phone;
        return phone.substring(0, 3) + "****" + phone.substring(phone.length() - 4);
    }

    // 统一审计：targetType 有「后台账号」「账号」「密码」「角色」「家长-学员」等
    private void audit(LoginUser operator, String action, String targetType, Long targetId, String content) {
        SysAuditLog log = new SysAuditLog();
        log.setUserId(operator == null ? null : operator.getId());
        log.setOperatorName(operator == null ? "系统" : operator.getName());
        log.setActionType(action);
        log.setTargetType(targetType);
        log.setTargetId(targetId);
        log.setContent(content);
        log.setResult("SUCCESS");
        auditLogMapper.insert(log);
    }
}
