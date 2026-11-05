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

    @Value("${app.wx.dev-mode:true}")
    private boolean wxDevMode;

    @Value("${app.wx.app-id:}")
    private String wxAppId;

    @Value("${app.wx.app-secret:}")
    private String wxAppSecret;

    public LoginResponse login(LoginRequest request, HttpServletRequest http) {
        SysUser user = userMapper.selectOne(new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getUsername, request.getUsername())
                .last("limit 1"));
        if (user == null || !"ADMIN".equals(user.getUserType())) {
            audit(null, "登录", "后台账号", null, "用户名不存在或非后台账号", ip(http), "FAIL");
            throw new BusinessException(401, "用户名或密码错误");
        }
        if ("DISABLED".equals(user.getStatus())) {
            throw new BusinessException(403, "账号已停用，请联系管理者");
        }
        LocalDateTime now = LocalDateTime.now();
        if (user.getLockedUntil() != null && user.getLockedUntil().isAfter(now)) {
            long minutes = Math.max(1, (long) Math.ceil(Duration.between(now, user.getLockedUntil()).toSeconds() / 60.0));
            throw new BusinessException(423, "账号已锁定，请 " + minutes + " 分钟后再试");
        }
        if (!StringUtils.hasText(user.getPasswordHash())
                || !passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            int attempts = (user.getFailedAttempts() == null ? 0 : user.getFailedAttempts()) + 1;
            user.setFailedAttempts(attempts);
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
        user.setFailedAttempts(0);
        user.setLockedUntil(null);
        user.setLastLoginAt(now);
        user.setLastLoginIp(ip(http));
        userMapper.updateById(user);
        SysRole role = roleMapper.selectById(user.getRoleId());
        audit(user, "登录", "后台账号", user.getId(), "用户名密码登录", ip(http), "SUCCESS");
        return buildResponse(user, role, true);
    }

    @Transactional
    public LoginResponse wxLogin(WxLoginRequest request, HttpServletRequest http) {
        String openid = resolveOpenid(request.getCode());
        SysUser existing = findByOpenid(openid);

        if (StringUtils.hasText(request.getPhone())) {
            SysUser teacher = userMapper.selectOne(new LambdaQueryWrapper<SysUser>()
                    .eq(SysUser::getPhone, request.getPhone())
                    .eq(SysUser::getUserType, "TEACHER")
                    .eq(SysUser::getStatus, "ENABLED")
                    .last("limit 1"));
            if (teacher != null) {
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

        if (existing != null) {
            if ("ADMIN".equals(existing.getUserType())) {
                throw new BusinessException(403, "后台账号不能使用小程序登录");
            }
            if ("DISABLED".equals(existing.getStatus())) {
                throw new BusinessException(403, "账号已停用，请联系机构");
            }
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

        SysRole parentRole = roleMapper.selectOne(new LambdaQueryWrapper<SysRole>()
                .eq(SysRole::getCode, "PARENT").last("limit 1"));
        if (parentRole == null) {
            throw new BusinessException(500, "系统缺少家长角色，请联系管理员");
        }
        SysUser parent = new SysUser();
        parent.setUserType("PARENT");
        parent.setRoleId(parentRole.getId());
        parent.setName(StringUtils.hasText(request.getName()) ? request.getName().trim() : "微信用户");
        parent.setPhone(request.getPhone());
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

    public LoginResponse me(LoginUser loginUser) {
        SysUser user = userMapper.selectById(loginUser.getId());
        if (user == null || !"ENABLED".equals(user.getStatus())) {
            throw new BusinessException(401, "账号不存在或已停用");
        }
        return buildResponse(user, roleMapper.selectById(user.getRoleId()), false);
    }

    @Transactional
    public void changePassword(LoginUser loginUser, ChangePasswordRequest request, HttpServletRequest http) {
        SysUser user = userMapper.selectById(loginUser.getId());
        if (user == null || !StringUtils.hasText(user.getPasswordHash())) {
            throw new BusinessException(400, "该账号不支持密码登录");
        }
        if (!passwordEncoder.matches(request.getOldPassword(), user.getPasswordHash())) {
            throw new BusinessException(400, "原密码错误");
        }
        if (request.getOldPassword().equals(request.getNewPassword())) {
            throw new BusinessException(400, "新密码不能与原密码相同");
        }
        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        user.setForceChangePassword(0);
        userMapper.updateById(user);
        audit(user, "账号变更", "密码", user.getId(), "修改登录密码", ip(http), "SUCCESS");
    }

    private SysUser findByOpenid(String openid) {
        return userMapper.selectOne(new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getWechatOpenid, openid).last("limit 1"));
    }

    private String resolveOpenid(String code) {
        if (wxDevMode) {
            // 本地联调：seed_* 直接对应演示数据的 openid，其余 code 自动加 dev_ 前缀
            return code.startsWith("seed_") || code.startsWith("dev_") ? code : "dev_" + code;
        }
        if (!StringUtils.hasText(wxAppId) || !StringUtils.hasText(wxAppSecret)) {
            throw new BusinessException(500, "微信登录未配置 AppID/AppSecret");
        }
        String url = "https://api.weixin.qq.com/sns/jscode2session"
                + "?appid={appid}&secret={secret}&js_code={code}&grant_type=authorization_code";
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

    private LoginResponse buildResponse(SysUser user, SysRole role, boolean withToken) {
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

    private Boolean toBool(Integer value) {
        return value != null && value == 1;
    }

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
