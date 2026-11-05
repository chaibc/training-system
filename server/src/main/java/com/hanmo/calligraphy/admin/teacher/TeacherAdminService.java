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

/**
 * 教师管理服务：教师档案分页/列表、建档（同时建登录账号）、编辑、启停用、解绑微信。
 *
 * <p>教师没有用户名/密码，小程序端按「手机号匹配 sys_user + 微信 openid」登录，因此手机号变更
 * 必须清空微信绑定；档案状态（teacher_profile.status）与账号状态（sys_user.status）需要成对维护。</p>
 *
 * <p>依赖表：teacher_profile、sys_user、sys_role（取内置 TEACHER 角色）、sys_audit_log。</p>
 */
@Service
@RequiredArgsConstructor
public class TeacherAdminService {

    private final TeacherProfileMapper teacherMapper;
    private final SysUserMapper userMapper;
    private final SysRoleMapper roleMapper;
    private final SysAuditLogMapper auditLogMapper;

    /**
     * 教师分页。
     *
     * <p>纯查询。keyword 匹配姓名或书体（styles）；按 sort 升序、id 倒序；每页上限 200。
     * 每行会回查 sys_user 补手机号/账号状态/微信绑定标记。</p>
     *
     * @param current 页码（最小 1）
     * @param size    每页条数（1~200）
     * @param keyword 姓名/书体，可为空
     * @param status  ACTIVE/DISABLED，可为空
     * @return 教师视图分页
     */
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

    /**
     * 教师全量列表（不分页），排序同 page。
     *
     * <p>纯查询；供下拉与小程序教师团队页使用。</p>
     *
     * @return 教师视图列表
     */
    public List<TeacherView> list() {
        return teacherMapper.selectList(new LambdaQueryWrapper<TeacherProfile>()
                        .orderByAsc(TeacherProfile::getSort).orderByDesc(TeacherProfile::getId))
                .stream().map(this::toView).toList();
    }

    /**
     * 教师建档：同时创建登录账号与教师档案。
     *
     * <p>副作用（@Transactional 内）：① sys_user 插入教师账号（user_type=TEACHER、绑定内置 TEACHER
     * 角色、无用户名/密码、状态随请求）；② teacher_profile 插入档案并生成教师编号；③ 写审计日志。
     * 不涉及课时与班级。</p>
     *
     * @param request  姓名、手机号（必填且为 11 位）、职称、教龄、书体、简介、标签、状态、排序
     * @param operator 操作人，用于审计
     * @return 新建教师视图
     * @throws BusinessException 400 手机号格式不对；500 系统缺少 TEACHER 角色或编号生成失败
     */
    @Transactional
    public TeacherView create(TeacherRequest request, LoginUser operator) {
        // 教师以手机号作为登录匹配依据，格式不对会直接导致无法登录，先做前置校验
        if (!request.phone().matches("^1\\d{10}$")) throw new BusinessException(400, "请填写正确的 11 位手机号");
        // 取内置角色 code=TEACHER；角色缺失说明初始化数据有问题，按 500 处理
        SysRole role = roleMapper.selectOne(new LambdaQueryWrapper<SysRole>()
                .eq(SysRole::getCode, "TEACHER").last("limit 1"));
        if (role == null) throw new BusinessException(500, "系统缺少教师角色");

        // 教师账号不写 username/password：小程序用手机号匹配 + 微信 openid 登录
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
        // 教师编号由 T001 起顺序补位，最多尝试 999 个
        teacher.setTeacherNo(generateNo());
        apply(teacher, request);
        teacherMapper.insert(teacher);
        audit(operator, "教师建档", teacher.getId(), "创建教师账号 " + teacher.getName());
        return toView(teacher);
    }

    /**
     * 编辑教师档案（局部更新）。
     *
     * <p>副作用（@Transactional 内）：更新 teacher_profile；若手机号变化，先清空 sys_user 的
     * wechat_openid/wechat_unionid（旧微信绑定作废），再更新账号姓名/手机号/状态；写审计日志。</p>
     *
     * @param id      教师档案 ID
     * @param request 待更新字段；name/phone 为空表示不改；title/styles 等为 null 不改；
     *                years/sort 为 null 会被置 0，status 非 DISABLED 一律写 ACTIVE
     * @param operator 操作人，用于审计
     * @return 更新后的教师视图
     * @throws BusinessException 404 教师不存在；400 手机号格式不对
     */
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
                // 换手机号必须解绑旧微信：旧 openid 可能属于原手机号，保留会导致新号登录串号
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

    /**
     * 启用/停用教师（在 ACTIVE 与 DISABLED 之间切换）。
     *
     * <p>副作用：同步更新 teacher_profile.status 与 sys_user.status（停用时账号也禁用，
     * 防止「档案停用但还能登录」），并写审计日志；@Transactional 覆盖两次更新。</p>
     *
     * @param id       教师档案 ID
     * @param operator 操作人，用于审计
     * @return 切换后的教师视图
     * @throws BusinessException 404 教师不存在
     */
    @Transactional
    public TeacherView toggle(Long id, LoginUser operator) {
        TeacherProfile teacher = requireTeacher(id);
        // 当前不是 DISABLED 就视为启用中，本次动作是停用
        boolean disabling = !"DISABLED".equals(teacher.getStatus());
        teacher.setStatus(disabling ? "DISABLED" : "ACTIVE");
        teacherMapper.updateById(teacher);
        SysUser user = userMapper.selectById(teacher.getUserId());
        if (user != null) {
            // 档案与账号状态必须成对切换，否则会出现已停用教师仍可登录
            user.setStatus(disabling ? "DISABLED" : "ENABLED");
            userMapper.updateById(user);
        }
        audit(operator, "教师建档", id, disabling ? "停用教师" : "启用教师");
        return toView(teacher);
    }

    /**
     * 解绑教师微信。
     *
     * <p>副作用：只把 sys_user.wechat_openid/wechat_unionid 置空并写审计；不改档案与账号状态，
     * 下次微信登录会重新按手机号匹配。@Transactional 覆盖。</p>
     *
     * @param id       教师档案 ID
     * @param operator 操作人，用于审计
     * @return 教师视图（wechatBound 变为 false）
     * @throws BusinessException 404 教师不存在
     */
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

    // 把请求字段合并进档案：null 表示不修改；years/sort 缺省归零，status 非 DISABLED 一律按 ACTIVE
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

    // 视图补齐 sys_user：手机号、账号状态、wechatBound（是否已绑微信）
    private TeacherView toView(TeacherProfile t) {
        SysUser user = t.getUserId() == null ? null : userMapper.selectById(t.getUserId());
        return new TeacherView(t.getId(), t.getUserId(), t.getTeacherNo(), t.getName(),
                user == null ? null : user.getPhone(), t.getTitle(), t.getYears(), t.getStyles(),
                t.getCoursesText(), t.getBio(), t.getQuote(), t.getTags(), t.getAvatar(),
                t.getStatus(), user != null && StringUtils.hasText(user.getWechatOpenid()),
                user == null ? null : user.getStatus(), t.getSort());
    }

    // 教师档案不存在统一抛 404
    private TeacherProfile requireTeacher(Long id) {
        TeacherProfile teacher = teacherMapper.selectById(id);
        if (teacher == null) throw new BusinessException(404, "教师不存在");
        return teacher;
    }

    // 教师编号 = T + 3 位序号（T001 起），逐个探测直到不冲突
    private String generateNo() {
        for (int i = 1; i < 1000; i++) {
            String no = "T" + String.format("%03d", i);
            if (teacherMapper.selectCount(new LambdaQueryWrapper<TeacherProfile>()
                    .eq(TeacherProfile::getTeacherNo, no)) == 0) return no;
        }
        throw new BusinessException(500, "生成教师编号失败");
    }

    // 教师模块审计：target_type 固定「教师」，action 统一为「教师建档」
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
