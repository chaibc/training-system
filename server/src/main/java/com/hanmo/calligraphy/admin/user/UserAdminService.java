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
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

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
        Map<Long, SysRole> roles = roleMapper.selectList(null).stream()
                .collect(Collectors.toMap(SysRole::getId, r -> r, (a, b) -> a));
        List<UserRow> rows = page.getRecords().stream()
                .map(u -> toRow(u, roles.get(u.getRoleId()), null))
                .toList();
        return PageResult.of(rows, page.getTotal(), page.getCurrent(), page.getSize());
    }

    @Transactional
    public UserRow createUser(CreateUserRequest request, LoginUser operator) {
        if (userMapper.selectCount(new LambdaQueryWrapper<SysUser>().eq(SysUser::getUsername, request.username())) > 0) {
            throw new BusinessException(400, "用户名已存在");
        }
        if (!request.phone().matches("^1\\d{10}$")) {
            throw new BusinessException(400, "请填写正确的 11 位手机号");
        }
        SysRole role = roleMapper.selectOne(new LambdaQueryWrapper<SysRole>()
                .eq(SysRole::getCode, "ACADEMIC").last("limit 1"));
        if (role == null) throw new BusinessException(500, "系统缺少教务角色");
        String password = StringUtils.hasText(request.password())
                ? request.password() : "Hm@" + UUID.randomUUID().toString().replace("-", "").substring(0, 6);
        SysUser user = new SysUser();
        user.setUsername(request.username().trim());
        user.setPasswordHash(passwordEncoder.encode(password));
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
        if (!"ADMIN".equals(user.getUserType()) && StringUtils.hasText(request.phone())
                && !request.phone().equals(oldPhone)) {
            userMapper.update(null, new LambdaUpdateWrapper<SysUser>()
                    .eq(SysUser::getId, id)
                    .set(SysUser::getWechatOpenid, null)
                    .set(SysUser::getWechatUnionid, null));
            audit(operator, "账号变更", "账号", id, "手机号变更，已解除微信绑定");
        }
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

    @Transactional
    public ResetPasswordResponse resetPassword(Long id, LoginUser operator) {
        SysUser user = requireUser(id);
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

    @Transactional
    public UserRow toggleStatus(Long id, LoginUser operator) {
        SysUser user = requireUser(id);
        SysRole role = roleMapper.selectById(user.getRoleId());
        if (role != null && "ADMIN".equals(role.getCode())) {
            throw new BusinessException(400, "管理者账号唯一，不可停用");
        }
        user.setStatus("ENABLED".equals(user.getStatus()) ? "DISABLED" : "ENABLED");
        userMapper.updateById(user);
        audit(operator, "账号变更", "账号", id, user.getStatus().equals("DISABLED") ? "停用账号" : "启用账号");
        return toRow(user, role, null);
    }

    @Transactional
    public UserRow unbindWechat(Long id, LoginUser operator) {
        SysUser user = requireUser(id);
        if ("ADMIN".equals(user.getUserType())) throw new BusinessException(400, "后台账号不使用微信登录");
        userMapper.update(null, new LambdaUpdateWrapper<SysUser>()
                .eq(SysUser::getId, id)
                .set(SysUser::getWechatOpenid, null)
                .set(SysUser::getWechatUnionid, null));
        audit(operator, "账号变更", "账号", id, "解绑微信");
        return toRow(requireUser(id), roleMapper.selectById(user.getRoleId()), null);
    }

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

    @Transactional
    public List<RoleView> updateRolePermissions(Long roleId, RolePermissionUpdateRequest request, LoginUser operator) {
        SysRole role = roleMapper.selectById(roleId);
        if (role == null) throw new BusinessException(404, "角色不存在");
        if ("ADMIN".equals(role.getCode())) throw new BusinessException(400, "管理者权限不可修改");
        if ("TEACHER".equals(role.getCode()) || "PARENT".equals(role.getCode())) {
            throw new BusinessException(400, "教师/家长为小程序身份，权限不可在后台修改");
        }
        if (StringUtils.hasText(request.dataScope())) role.setDataScope(request.dataScope());
        roleMapper.updateById(role);
        permissionMapper.delete(new LambdaQueryWrapper<SysRolePermission>().eq(SysRolePermission::getRoleId, roleId));
        if (request.permissions() != null) {
            for (PermissionView p : request.permissions()) {
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

    public List<BindingView> bindings(String status) {
        List<ParentStudent> list;
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

    @Transactional
    public BindingView approveBinding(Long id, LoginUser operator) {
        ParentStudent binding = parentStudentMapper.selectById(id);
        if (binding == null) throw new BusinessException(404, "绑定申请不存在");
        boolean unbind = "UNBIND".equals(binding.getSource());
        if (!unbind) {
            long activeGuardians = parentStudentMapper.selectCount(new LambdaQueryWrapper<ParentStudent>()
                    .eq(ParentStudent::getStudentId, binding.getStudentId())
                    .eq(ParentStudent::getStatus, "ACTIVE")
                    .ne(ParentStudent::getId, binding.getId()));
            if (activeGuardians >= 2) {
                throw new BusinessException(400, "该学员已有 2 位有效监护人，不能继续通过绑定");
            }
        }
        if (!unbind && binding.getIsPrimary() != null && binding.getIsPrimary() == 1) {
            parentStudentMapper.update(null, new LambdaUpdateWrapper<ParentStudent>()
                    .eq(ParentStudent::getStudentId, binding.getStudentId())
                    .set(ParentStudent::getIsPrimary, 0));
        }
        if (unbind) {
            binding.setStatus("REVOKED");
            binding.setIsPrimary(0);
        } else {
            binding.setStatus("ACTIVE");
        }
        binding.setVerifiedBy(operator.getId());
        binding.setVerifiedAt(LocalDateTime.now());
        parentStudentMapper.updateById(binding);
        audit(operator, "绑定审核", "家长-学员", binding.getId(), unbind ? "通过家长解绑申请" : "通过家长绑定");
        return toBinding(binding);
    }

    @Transactional
    public BindingView rejectBinding(Long id, LoginUser operator) {
        ParentStudent binding = parentStudentMapper.selectById(id);
        if (binding == null) throw new BusinessException(404, "绑定申请不存在");
        boolean unbind = "UNBIND".equals(binding.getSource());
        binding.setStatus(unbind ? "ACTIVE" : "REJECTED");
        if (unbind) binding.setSource("SELF");
        binding.setVerifiedBy(operator.getId());
        binding.setVerifiedAt(LocalDateTime.now());
        parentStudentMapper.updateById(binding);
        audit(operator, "绑定审核", "家长-学员", binding.getId(), unbind ? "驳回家长解绑申请" : "驳回家长绑定");
        return toBinding(binding);
    }

    @Transactional
    public BindingView addBinding(AddBindingRequest request, LoginUser operator) {
        SysUser parent = requireUser(request.parentUserId());
        if (!"PARENT".equals(parent.getUserType())) throw new BusinessException(400, "请选择家长账号");
        Student student = studentMapper.selectById(request.studentId());
        if (student == null) throw new BusinessException(404, "学员不存在");
        ParentStudent exists = parentStudentMapper.selectOne(new LambdaQueryWrapper<ParentStudent>()
                .eq(ParentStudent::getParentUserId, parent.getId())
                .eq(ParentStudent::getStudentId, student.getId()).last("limit 1"));
        if (exists != null && List.of("ACTIVE", "PENDING").contains(exists.getStatus())) {
            throw new BusinessException(400, "该家长与学员已存在有效或待审核绑定，请直接审核或解绑");
        }
        long active = parentStudentMapper.selectCount(new LambdaQueryWrapper<ParentStudent>()
                .eq(ParentStudent::getStudentId, student.getId())
                .eq(ParentStudent::getStatus, "ACTIVE"));
        if (active >= 2) throw new BusinessException(400, "一个学员最多 2 个有效监护人");
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
        if (binding.getId() == null) parentStudentMapper.insert(binding);
        else parentStudentMapper.updateById(binding);
        audit(operator, "绑定审核", "家长-学员", binding.getId(), exists == null ? "新增家长绑定" : "恢复家长绑定");
        return toBinding(binding);
    }

    @Transactional
    public BindingView unbindBinding(Long id, LoginUser operator) {
        ParentStudent binding = parentStudentMapper.selectById(id);
        if (binding == null) throw new BusinessException(404, "绑定关系不存在");
        binding.setStatus("REVOKED");
        parentStudentMapper.updateById(binding);
        audit(operator, "绑定审核", "家长-学员", binding.getId(), "解除家长绑定");
        return toBinding(binding);
    }

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

    public List<ParentOption> parentOptions() {
        return userMapper.selectList(new LambdaQueryWrapper<SysUser>()
                        .eq(SysUser::getUserType, "PARENT")
                        .eq(SysUser::getStatus, "ENABLED")
                        .orderByDesc(SysUser::getId))
                .stream().map(u -> new ParentOption(u.getId(), u.getName(), u.getPhone())).toList();
    }

    public List<StudentOption> studentOptions() {
        return studentMapper.selectList(new LambdaQueryWrapper<Student>()
                        .eq(Student::getStatus, "ACTIVE")
                        .orderByDesc(Student::getId))
                .stream().map(s -> new StudentOption(s.getId(), s.getName(), s.getStudentNo())).toList();
    }

    private UserRow toRow(SysUser user, SysRole role, String initialPassword) {
        String roleCode = role == null ? null : role.getCode();
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
            related = manager ? "全机构" : "全机构";
        }
        return new UserRow(user.getId(), user.getName(), user.getUsername(), user.getUserType(),
                roleCode, role == null ? null : role.getName(), maskPhone(user.getPhone()),
                StringUtils.hasText(user.getWechatOpenid()), user.getStatus(), user.getLastLoginAt(),
                related, !manager, manager, initialPassword);
    }

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

    private SysUser requireUser(Long id) {
        SysUser user = userMapper.selectById(id);
        if (user == null) throw new BusinessException(404, "账号不存在");
        return user;
    }

    private boolean isOne(Integer v) {
        return v != null && v == 1;
    }

    private String maskPhone(String phone) {
        if (phone == null || phone.length() < 7) return phone;
        return phone.substring(0, 3) + "****" + phone.substring(phone.length() - 4);
    }

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
