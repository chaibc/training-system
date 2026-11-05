package com.hanmo.calligraphy.admin.teacher;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.hanmo.calligraphy.admin.teacher.TeacherAdminDtos.TeacherRequest;
import com.hanmo.calligraphy.admin.teacher.TeacherAdminDtos.TeacherView;
import com.hanmo.calligraphy.common.BusinessException;
import com.hanmo.calligraphy.common.PageResult;
import com.hanmo.calligraphy.domain.entity.TeacherProfile;
import com.hanmo.calligraphy.domain.mapper.TeacherProfileMapper;
import com.hanmo.calligraphy.security.LoginUser;
import com.hanmo.calligraphy.system.entity.SysAuditLog;
import com.hanmo.calligraphy.system.entity.SysRole;
import com.hanmo.calligraphy.system.entity.SysUser;
import com.hanmo.calligraphy.system.mapper.SysAuditLogMapper;
import com.hanmo.calligraphy.system.mapper.SysRoleMapper;
import com.hanmo.calligraphy.system.mapper.SysUserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;

@Service
@RequiredArgsConstructor
public class TeacherAdminService {

    private final TeacherProfileMapper teacherMapper;
    private final SysUserMapper userMapper;
    private final SysRoleMapper roleMapper;
    private final SysAuditLogMapper auditLogMapper;

    public PageResult<TeacherView> page(long current, long size, String keyword, String status) {
        Page<TeacherProfile> page = new Page<>(Math.max(1, current), Math.min(Math.max(1, size), 200));
        LambdaQueryWrapper<TeacherProfile> qw = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(keyword)) {
            qw.and(w -> w.like(TeacherProfile::getName, keyword).or().like(TeacherProfile::getStyles, keyword));
        }
        if (StringUtils.hasText(status)) qw.eq(TeacherProfile::getStatus, status);
        qw.orderByAsc(TeacherProfile::getSort).orderByDesc(TeacherProfile::getId);
        teacherMapper.selectPage(page, qw);
        List<TeacherView> rows = page.getRecords().stream().map(this::toView).toList();
        return PageResult.of(rows, page.getTotal(), page.getCurrent(), page.getSize());
    }

    public List<TeacherView> list() {
        return teacherMapper.selectList(new LambdaQueryWrapper<TeacherProfile>()
                        .orderByAsc(TeacherProfile::getSort).orderByDesc(TeacherProfile::getId))
                .stream().map(this::toView).toList();
    }

    @Transactional
    public TeacherView create(TeacherRequest request, LoginUser operator) {
        if (!request.phone().matches("^1\\d{10}$")) throw new BusinessException(400, "请填写正确的 11 位手机号");
        SysRole role = roleMapper.selectOne(new LambdaQueryWrapper<SysRole>()
                .eq(SysRole::getCode, "TEACHER").last("limit 1"));
        if (role == null) throw new BusinessException(500, "系统缺少教师角色");

        SysUser user = new SysUser();
        user.setUserType("TEACHER");
        user.setRoleId(role.getId());
        user.setName(request.name().trim());
        user.setPhone(request.phone().trim());
        user.setStatus("DISABLED".equals(request.status()) ? "DISABLED" : "ENABLED");
        user.setFailedAttempts(0);
        user.setForceChangePassword(0);
        userMapper.insert(user);

        TeacherProfile teacher = new TeacherProfile();
        teacher.setUserId(user.getId());
        teacher.setTeacherNo(generateNo());
        apply(teacher, request);
        teacherMapper.insert(teacher);
        audit(operator, "教师建档", teacher.getId(), "创建教师账号 " + teacher.getName());
        return toView(teacher);
    }

    @Transactional
    public TeacherView update(Long id, TeacherRequest request, LoginUser operator) {
        TeacherProfile teacher = requireTeacher(id);
        if (StringUtils.hasText(request.phone()) && !request.phone().matches("^1\\d{10}$")) {
            throw new BusinessException(400, "请填写正确的 11 位手机号");
        }
        SysUser user = userMapper.selectById(teacher.getUserId());
        if (user != null) {
            if (StringUtils.hasText(request.name())) user.setName(request.name().trim());
            if (StringUtils.hasText(request.phone())) {
                if (!request.phone().equals(user.getPhone())) {
                    user.setPhone(request.phone().trim());
                    userMapper.update(null, new LambdaUpdateWrapper<SysUser>()
                            .eq(SysUser::getId, user.getId())
                            .set(SysUser::getWechatOpenid, null)
                            .set(SysUser::getWechatUnionid, null));
                }
            }
            user.setStatus("DISABLED".equals(request.status()) ? "DISABLED" : "ENABLED");
            userMapper.updateById(user);
        }
        apply(teacher, request);
        teacherMapper.updateById(teacher);
        audit(operator, "教师建档", id, "编辑教师资料 " + teacher.getName());
        return toView(teacher);
    }

    @Transactional
    public TeacherView toggle(Long id, LoginUser operator) {
        TeacherProfile teacher = requireTeacher(id);
        boolean disabling = !"DISABLED".equals(teacher.getStatus());
        teacher.setStatus(disabling ? "DISABLED" : "ACTIVE");
        teacherMapper.updateById(teacher);
        SysUser user = userMapper.selectById(teacher.getUserId());
        if (user != null) {
            user.setStatus(disabling ? "DISABLED" : "ENABLED");
            userMapper.updateById(user);
        }
        audit(operator, "教师建档", id, disabling ? "停用教师" : "启用教师");
        return toView(teacher);
    }

    @Transactional
    public TeacherView unbind(Long id, LoginUser operator) {
        TeacherProfile teacher = requireTeacher(id);
        userMapper.update(null, new LambdaUpdateWrapper<SysUser>()
                .eq(SysUser::getId, teacher.getUserId())
                .set(SysUser::getWechatOpenid, null)
                .set(SysUser::getWechatUnionid, null));
        audit(operator, "教师建档", id, "解绑教师微信");
        return toView(teacher);
    }

    private void apply(TeacherProfile teacher, TeacherRequest request) {
        if (StringUtils.hasText(request.name())) teacher.setName(request.name().trim());
        if (request.title() != null) teacher.setTitle(request.title());
        teacher.setYears(request.years() == null ? 0 : request.years());
        if (request.styles() != null) teacher.setStyles(request.styles());
        if (request.coursesText() != null) teacher.setCoursesText(request.coursesText());
        if (request.bio() != null) teacher.setBio(request.bio());
        if (request.quote() != null) teacher.setQuote(request.quote());
        if (request.tags() != null) teacher.setTags(request.tags());
        teacher.setStatus("DISABLED".equals(request.status()) ? "DISABLED" : "ACTIVE");
        teacher.setSort(request.sort() == null ? 0 : request.sort());
    }

    private TeacherView toView(TeacherProfile t) {
        SysUser user = t.getUserId() == null ? null : userMapper.selectById(t.getUserId());
        return new TeacherView(t.getId(), t.getUserId(), t.getTeacherNo(), t.getName(),
                user == null ? null : user.getPhone(), t.getTitle(), t.getYears(), t.getStyles(),
                t.getCoursesText(), t.getBio(), t.getQuote(), t.getTags(), t.getAvatar(),
                t.getStatus(), user != null && StringUtils.hasText(user.getWechatOpenid()),
                user == null ? null : user.getStatus(), t.getSort());
    }

    private TeacherProfile requireTeacher(Long id) {
        TeacherProfile teacher = teacherMapper.selectById(id);
        if (teacher == null) throw new BusinessException(404, "教师不存在");
        return teacher;
    }

    private String generateNo() {
        for (int i = 1; i < 1000; i++) {
            String no = "T" + String.format("%03d", i);
            if (teacherMapper.selectCount(new LambdaQueryWrapper<TeacherProfile>()
                    .eq(TeacherProfile::getTeacherNo, no)) == 0) return no;
        }
        throw new BusinessException(500, "生成教师编号失败");
    }

    private void audit(LoginUser operator, String action, Long targetId, String content) {
        SysAuditLog log = new SysAuditLog();
        log.setUserId(operator == null ? null : operator.getId());
        log.setOperatorName(operator == null ? "系统" : operator.getName());
        log.setActionType(action);
        log.setTargetType("教师");
        log.setTargetId(targetId);
        log.setContent(content);
        log.setResult("SUCCESS");
        auditLogMapper.insert(log);
    }
}
