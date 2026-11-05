package com.hanmo.calligraphy.auth;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.hanmo.calligraphy.auth.dto.ChangePasswordRequest;
import com.hanmo.calligraphy.auth.dto.LoginRequest;
import com.hanmo.calligraphy.auth.dto.LoginResponse;
import com.hanmo.calligraphy.auth.dto.PermissionDto;
import com.hanmo.calligraphy.auth.dto.WxLoginRequest;
import com.hanmo.calligraphy.common.BusinessException;
import com.hanmo.calligraphy.security.JwtService;
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
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 认证服务：后台账号密码登录、小程序微信登录、当前用户查询与修改密码。
 *
 * <p>账号口径：后台登录只认 {@code user_type=ADMIN}；小程序端统一走 {@code /wx-login}，
 * 教师按 {@code sys_user.phone} 匹配，家长首次登录自动建号。openid 与账号一对一，
 * 冲突统一返回 409。</p>
 *
 * <p>Token 由 {@link JwtService} 签发（携带 uid/name/userType/role 声明，默认 8 小时），
 * 服务端不存会话、不维护黑名单，因此改密不会让旧 Token 立即失效。</p>
 *
 * <p>登录成功与失败都写 {@code sys_audit_log}，便于排查撞库与账号锁定。</p>
 */
@Service
@RequiredArgsConstructor
public class AuthService {

    private final SysUserMapper userMapper;
    private final SysRoleMapper roleMapper;
    private final SysRolePermissionMapper permissionMapper;
    private final SysAuditLogMapper auditLogMapper;
    private final ParentProfileMapper parentProfileMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    // 微信登录开发模式：dev/local profile 显式配置为 true；默认 false，避免漏配时在生产误用演示 code
    @Value("${app.wx.dev-mode:false}")
    private boolean wxDevMode;

    // 真实模式换取 openid 所需的小程序凭证；两者任一为空都会导致登录失败
    @Value("${app.wx.app-id:}")
    private String wxAppId;

    @Value("${app.wx.app-secret:}")
    private String wxAppSecret;

    /**
     * 后台账号密码登录。
     *
     * <p>只允许 {@code user_type=ADMIN}；账号不存在与密码错误统一返回 401，避免暴露账号是否存在。
     * 密码用 BCrypt 比对，连续失败 5 次锁定 5 分钟（423）。</p>
     *
     * <p>副作用：更新 {@code sys_user} 的失败次数、{@code locked_until}、最后登录时间与 IP；
     * 每次成功/失败都写审计日志；成功时签发 JWT。</p>
     *
     * @param request 登录请求（用户名、密码）
     * @param http    当前请求，用于取客户端 IP
     * @return 登录响应，含新签发的 token
     * @throws BusinessException 401 账号不存在或密码错误；403 账号停用；423 账号锁定
     */
    public LoginResponse login(LoginRequest request, HttpServletRequest http) {
        SysUser user = userMapper.selectOne(new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getUsername, request.getUsername())
                .last("limit 1"));
        // 只允许后台管理者登录；教师/家长走 /wx-login，账号不存在也按“密码错误”返回
        if (user == null || !"ADMIN".equals(user.getUserType())) {
            audit(null, "登录", "后台账号", null, "用户名不存在或非后台账号", ip(http), "FAIL");
            throw new BusinessException(401, "用户名或密码错误");
        }
        assertAccountUsable(user);
        LocalDateTime now = LocalDateTime.now();
        if (!StringUtils.hasText(user.getPasswordHash())
                || !passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            int attempts = (user.getFailedAttempts() == null ? 0 : user.getFailedAttempts()) + 1;
            user.setFailedAttempts(attempts);
            // 连续第 5 次失败即锁定 5 分钟，并单独记一条审计
            if (attempts >= 5) {
                user.setLockedUntil(now.plusMinutes(5));
                userMapper.updateById(user);
                audit(user, "登录", "后台账号", user.getId(), "连续输错密码 5 次，锁定 5 分钟", ip(http), "FAIL");
                throw new BusinessException(423, "密码连续输错 5 次，账号已锁定 5 分钟");
            }
            userMapper.updateById(user);
            audit(user, "登录", "后台账号", user.getId(), "密码错误，第 " + attempts + " 次", ip(http), "FAIL");
            throw new BusinessException(401, "用户名或密码错误，还可尝试 " + (5 - attempts) + " 次");
        }
        // 登录成功：清空失败计数并解除锁定
        user.setFailedAttempts(0);
        user.setLockedUntil(null);
        user.setLastLoginAt(now);
        user.setLastLoginIp(ip(http));
        userMapper.updateById(user);
        SysRole role = roleMapper.selectById(user.getRoleId());
        audit(user, "登录", "后台账号", user.getId(), "用户名密码登录", ip(http), "SUCCESS");
        return buildResponse(user, role, true);
    }

    /**
     * 小程序微信登录：code → openid，再按手机号匹配教师或识别家长。
     *
     * <p>分支顺序：教师手机号匹配 → 已有 openid 登录 → 家长自动建号。
     * 传了手机号却没匹配到教师时直接 404，避免因手机号写错而误建家长账号。
     * 整个方法在事务内，账号与 {@code parent_profile} 要么都建成功、要么都回滚。</p>
     *
     * <p>副作用：更新或新建 {@code sys_user}、可能新建 {@code parent_profile}、
     * 写 {@code sys_audit_log}，并签发 JWT。</p>
     *
     * @param request 微信登录参数（code 必填，phone/name 可选）
     * @param http    当前请求，用于取客户端 IP
     * @return 登录响应，roleCode 为 PARENT 或 TEACHER
     * @throws BusinessException 401 code 无效；403 账号停用/后台账号；404 教师未匹配；
     *         409 微信已绑定其他账号；500 未配置微信凭证或缺少家长角色
     */
    @Transactional
    public LoginResponse wxLogin(WxLoginRequest request, HttpServletRequest http) {
        String openid = resolveOpenid(request.getCode());
        SysUser existing = findByOpenid(openid);

        if (StringUtils.hasText(request.getPhone())) {
            // 教师手机号登录：优先于按 openid 找回账号，命中即把当前 openid 绑定到教师档案
            // 这里不按 status 过滤，先取到人再统一校验，避免停用/锁定账号被报成「手机号未匹配」
            SysUser teacher = userMapper.selectOne(new LambdaQueryWrapper<SysUser>()
                    .eq(SysUser::getPhone, request.getPhone())
                    .eq(SysUser::getUserType, "TEACHER")
                    .last("limit 1"));
            if (teacher != null) {
                assertAccountUsable(teacher);
                if (existing != null && !existing.getId().equals(teacher.getId())) {
                    throw new BusinessException(409, "当前微信已绑定其他账号，不能同时绑定教师身份");
                }
                if (StringUtils.hasText(teacher.getWechatOpenid()) && !teacher.getWechatOpenid().equals(openid)) {
                    throw new BusinessException(409, "该教师账号已绑定其他微信，请联系管理者解绑");
                }
                teacher.setWechatOpenid(openid);
                teacher.setLastLoginAt(LocalDateTime.now());
                teacher.setLastLoginIp(ip(http));
                teacher.setFailedAttempts(0);
                userMapper.updateById(teacher);
                audit(teacher, "登录", "教师端", teacher.getId(), "教师微信登录（手机号匹配）", ip(http), "SUCCESS");
                return buildResponse(teacher, roleMapper.selectById(teacher.getRoleId()), true);
            }
        }

        // 已有 openid：直接登录原账号；后台账号不允许，停用/锁定账号同样拒绝
        if (existing != null) {
            if ("ADMIN".equals(existing.getUserType())) {
                throw new BusinessException(403, "后台账号不能使用小程序登录");
            }
            // 与后台登录共用同一套校验：停用、LOCKED、临时锁定都拦，避免绕过密码锁定
            assertAccountUsable(existing);
            existing.setLastLoginAt(LocalDateTime.now());
            existing.setLastLoginIp(ip(http));
            userMapper.updateById(existing);
            audit(existing, "登录", existing.getUserType().equals("TEACHER") ? "教师端" : "家长端",
                    existing.getId(), "微信授权登录", ip(http), "SUCCESS");
            return buildResponse(existing, roleMapper.selectById(existing.getRoleId()), true);
        }

        // 传入手机号表示教师登录意图；未匹配教师账号时不得自动创建家长账号
        if (StringUtils.hasText(request.getPhone())) {
            audit(null, "登录", "教师端", null, "教师微信登录未匹配手机号 " + request.getPhone(), ip(http), "FAIL");
            throw new BusinessException(404, "未匹配教师账号，请联系教务核对手机号");
        }

        // 家长首次登录：先取 PARENT 角色，再建 sys_user 与 parent_profile
        SysRole parentRole = roleMapper.selectOne(new LambdaQueryWrapper<SysRole>()
                .eq(SysRole::getCode, "PARENT").last("limit 1"));
        if (parentRole == null) {
            throw new BusinessException(500, "系统缺少家长角色，请联系管理员");
        }
        SysUser parent = new SysUser();
        parent.setUserType("PARENT");
        parent.setRoleId(parentRole.getId());
        parent.setName(StringUtils.hasText(request.getName()) ? request.getName().trim() : "微信用户");
        // 走到这里 phone 必为空（手机号非空且未匹配教师已在上方抛 404），因此不再写 phone，
        // 家长手机号后续由教务在后台补录，避免写入一个永远是 null 的字段
        parent.setWechatOpenid(openid);
        parent.setStatus("ENABLED");
        parent.setFailedAttempts(0);
        parent.setForceChangePassword(0);
        parent.setLastLoginAt(LocalDateTime.now());
        parent.setLastLoginIp(ip(http));
        userMapper.insert(parent);

        ParentProfile profile = new ParentProfile();
        profile.setUserId(parent.getId());
        profile.setName(parent.getName());
        profile.setPhone(parent.getPhone());
        parentProfileMapper.insert(profile);

        audit(parent, "登录", "家长端", parent.getId(), "首次微信登录自动创建家长账号", ip(http), "SUCCESS");
        return buildResponse(parent, parentRole, true);
    }

    /**
     * 按登录身份读取最新的用户与角色权限，供前端刷新本地缓存。
     *
     * @param loginUser JWT 过滤器解析出的身份（只用到 id）
     * @return 用户信息 + 权限点，token 为 null（不续签）
     * @throws BusinessException 401 账号不存在或已停用
     */
    public LoginResponse me(LoginUser loginUser) {
        SysUser user = userMapper.selectById(loginUser.getId());
        if (user == null || !"ENABLED".equals(user.getStatus())) {
            throw new BusinessException(401, "账号不存在或已停用");
        }
        return buildResponse(user, roleMapper.selectById(user.getRoleId()), false);
    }

    /**
     * 修改当前后台账号的登录密码。
     *
     * <p>只支持有密码哈希的账号（教师/家长走微信登录，调用会得到 400）。
     * 校验原密码、新旧密码不同，新密码长度由 DTO 限定 8–32 位。</p>
     *
     * <p>副作用：更新 {@code sys_user.password_hash} 并清空 {@code force_change_password}，
     * 写审计日志。<b>不会</b>使已签发 Token 失效：JWT 无状态且无黑名单，
     * 旧 Token 只能等 8 小时自然过期，必要时应在改密后引导用户重新登录。</p>
     *
     * @param loginUser 当前登录身份
     * @param request   原密码与新密码
     * @param http      当前请求，用于写审计 IP
     * @throws BusinessException 400 账号无密码、原密码错误或新旧密码相同
     */
    @Transactional
    public void changePassword(LoginUser loginUser, ChangePasswordRequest request, HttpServletRequest http) {
        SysUser user = userMapper.selectById(loginUser.getId());
        // 教师/家长为微信登录，没有密码哈希，直接拒绝
        if (user == null || !StringUtils.hasText(user.getPasswordHash())) {
            throw new BusinessException(400, "该账号不支持密码登录");
        }
        if (!passwordEncoder.matches(request.getOldPassword(), user.getPasswordHash())) {
            throw new BusinessException(400, "原密码错误");
        }
        if (request.getOldPassword().equals(request.getNewPassword())) {
            throw new BusinessException(400, "新密码不能与原密码相同");
        }
        // 只更新密码哈希，不触碰已签发 Token，旧 Token 仍有效
        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        user.setForceChangePassword(0);
        userMapper.updateById(user);
        audit(user, "账号变更", "密码", user.getId(), "修改登录密码", ip(http), "SUCCESS");
    }

    // openid 在库里应唯一，这里仍用 limit 1 兜底历史脏数据
    private SysUser findByOpenid(String openid) {
        return userMapper.selectOne(new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getWechatOpenid, openid).last("limit 1"));
    }

    /**
     * 把微信登录 code 换成 openid。
     *
     * <p>开发模式（{@code app.wx.dev-mode=true}）：不调用微信接口，{@code seed_*} 直接作为 openid
     * 以对应演示数据，已带 {@code dev_} 前缀的原样保留，其余 code 统一加 {@code dev_} 前缀，
     * 方便本地反复登录。</p>
     *
     * <p>真实模式：要求配置 AppID/AppSecret，调用微信 {@code jscode2session}；
     * 微信未返回 openid 时把 errmsg 包装成 401 抛出。</p>
     *
     * @param code 微信登录临时凭证
     * @return openid
     * @throws BusinessException 401 微信返回错误；500 未配置 AppID/AppSecret
     */
    private String resolveOpenid(String code) {
        if (wxDevMode) {
            // 本地联调：seed_* 直接对应演示数据的 openid，其余 code 自动加 dev_ 前缀
            return code.startsWith("seed_") || code.startsWith("dev_") ? code : "dev_" + code;
        }
        // 缺少凭证时无法调用微信接口，直接 500 提示配置问题
        if (!StringUtils.hasText(wxAppId) || !StringUtils.hasText(wxAppSecret)) {
            throw new BusinessException(500, "微信登录未配置 AppID/AppSecret");
        }
        // 真实模式：调用微信 jscode2session 换取 openid，失败时把 errmsg 透出给调用方
        String url = "https://api.weixin.qq.com/sns/jscode2session"
                + "?appid={appid}&secret={secret}&js_code={code}&grant_type=authorization_code";
        // 微信接口返回 JSON 对象，这里按 Map 读取 openid/errmsg
        @SuppressWarnings("unchecked")
        Map<String, Object> result = RestClient.create().get()
                .uri(url, wxAppId, wxAppSecret, code)
                .retrieve().body(Map.class);
        Object openid = result == null ? null : result.get("openid");
        if (openid == null) {
            Object errmsg = result == null ? "unknown" : result.get("errmsg");
            throw new BusinessException(401, "微信登录失败：" + errmsg);
        }
        return openid.toString();
    }

    /**
     * 组装登录/当前用户响应。
     *
     * @param user      账号实体
     * @param role      角色，可为 null（此时权限为空、roleCode 为 null）
     * @param withToken 是否签发新 Token；{@code /me} 传 false
     * @return 登录响应
     */
    private LoginResponse buildResponse(SysUser user, SysRole role, boolean withToken) {
        // 权限点来自角色授权表，1 视为有权限；角色为空则返回空列表
        List<PermissionDto> permissions = role == null ? List.of() :
                permissionMapper.selectList(new LambdaQueryWrapper<SysRolePermission>()
                                .eq(SysRolePermission::getRoleId, role.getId()))
                        .stream()
                        .map(p -> new PermissionDto(
                                p.getModuleKey(),
                                Boolean.TRUE.equals(toBool(p.getCanView())),
                                Boolean.TRUE.equals(toBool(p.getCanCreate())),
                                Boolean.TRUE.equals(toBool(p.getCanEdit())),
                                Boolean.TRUE.equals(toBool(p.getCanDelete())),
                                Boolean.TRUE.equals(toBool(p.getCanSpecial()))
                        )).toList();
        return LoginResponse.builder()
                .token(withToken ? jwtService.generate(user, role) : null)
                .userId(user.getId())
                .username(user.getUsername())
                .name(user.getName())
                .userType(user.getUserType())
                .roleCode(role == null ? null : role.getCode())
                .roleName(role == null ? null : role.getName())
                .dataScope(role == null ? null : role.getDataScope())
                .forceChangePassword(user.getForceChangePassword() != null && user.getForceChangePassword() == 1)
                .permissions(permissions)
                .build();
    }

    // 数据库用 0/1 表示权限，这里转成布尔
    private Boolean toBool(Integer value) {
        return value != null && value == 1;
    }

    /**
     * 校验账号当前是否允许登录：停用、锁定（含临时锁定）一律拒绝。
     *
     * <p>锁定有两层含义：① status=LOCKED 表示人工/系统永久锁定（预留给管理端，目前没有写入点）；
     * ② locked_until 表示连续输错密码后的临时锁定（5 分钟）。两者都要拦，否则后台密码锁定
     * 可以被微信登录绕过。</p>
     *
     * @param user 待校验账号
     * @throws BusinessException 403 账号已停用；423 账号锁定（临时锁定会带剩余分钟数）
     */
    private void assertAccountUsable(SysUser user) {
        if ("DISABLED".equals(user.getStatus())) {
            throw new BusinessException(403, "账号已停用，请联系管理者");
        }
        if ("LOCKED".equals(user.getStatus())) {
            throw new BusinessException(423, "账号已锁定，请联系管理者解锁");
        }
        if (user.getLockedUntil() != null && user.getLockedUntil().isAfter(LocalDateTime.now())) {
            long minutes = Math.max(1, (long) Math.ceil(
                    Duration.between(LocalDateTime.now(), user.getLockedUntil()).toSeconds() / 60.0));
            throw new BusinessException(423, "账号已锁定，请 " + minutes + " 分钟后再试");
        }
    }

    /**
     * 写一条审计日志。
     *
     * @param user       操作人，可为 null（登录失败时账号都可能不存在）
     * @param action     动作类型，如“登录”“账号变更”
     * @param targetType 目标类型，如“后台账号”“教师端”
     * @param targetId   目标 ID
     * @param content    详情
     * @param ip         客户端 IP
     * @param result     SUCCESS / FAIL
     */
    private void audit(SysUser user, String action, String targetType, Long targetId,
                       String content, String ip, String result) {
        SysAuditLog log = new SysAuditLog();
        log.setUserId(user == null ? null : user.getId());
        log.setOperatorName(user == null ? "未知" : user.getName());
        log.setActionType(action);
        log.setTargetType(targetType);
        log.setTargetId(targetId);
        log.setContent(content);
        log.setIp(ip);
        log.setResult(result);
        auditLogMapper.insert(log);
    }

    // 取真实客户端 IP：优先 X-Forwarded-For 第一段，其次 X-Real-IP，最后 remoteAddr
    private String ip(HttpServletRequest request) {
        String xff = request.getHeader("X-Forwarded-For");
        if (StringUtils.hasText(xff)) {
            return xff.split(",")[0].trim();
        }
        String realIp = request.getHeader("X-Real-IP");
        if (StringUtils.hasText(realIp)) {
            return realIp;
        }
        return request.getRemoteAddr();
    }
}
