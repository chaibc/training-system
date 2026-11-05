package com.hanmo.calligraphy.auth;

import com.hanmo.calligraphy.auth.dto.ChangePasswordRequest;
import com.hanmo.calligraphy.auth.dto.LoginRequest;
import com.hanmo.calligraphy.auth.dto.LoginResponse;
import com.hanmo.calligraphy.auth.dto.WxLoginRequest;
import com.hanmo.calligraphy.common.ApiResponse;
import com.hanmo.calligraphy.security.LoginUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 认证接口分组：后台账号登录、小程序微信登录、当前登录用户、修改密码与退出登录。
 *
 * <p>URL 前缀 {@code /api/auth}。登录类接口（{@code /login}、{@code /wx-login}）由
 * {@link com.hanmo.calligraphy.security.SecurityConfig} 放行，可匿名调用；其余接口必须携带
 * {@code Authorization: Bearer <token>}，由 JWT 过滤器解析出 {@link LoginUser} 后注入方法参数。</p>
 *
 * <p>JWT 无状态：服务端不保存会话、不维护黑名单，Token 默认 8 小时有效
 * （{@code app.jwt.expire-hours}）。退出登录只由客户端丢弃 Token，服务端不会主动作废已签发的令牌。</p>
 */
@Tag(name = "认证")
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    /**
     * 后台账号（用户名 + 密码）登录。
     *
     * <p>当前实现只放行 {@code user_type=ADMIN} 的账号；教师、家长请走 {@code /wx-login}。
     * 账号不存在与密码错误统一返回 401，避免暴露账号是否存在；连续输错 5 次密码锁定 5 分钟，返回 423。</p>
     *
     * <p>副作用：更新 {@code sys_user} 的失败次数、锁定截止时间、最后登录时间与 IP；
     * 成功与失败都会写 {@code sys_audit_log}；成功时签发 JWT。</p>
     *
     * @param request 用户名与密码，均不可为空
     * @param http    当前 HTTP 请求，用于解析客户端 IP 写审计
     * @return 登录响应：token、用户信息、角色与权限点
     * @throws BusinessException 401 账号不存在或密码错误；403 账号停用；423 账号锁定中
     */
    @Operation(summary = "后台账号登录")
    @PostMapping("/login")
    public ApiResponse<LoginResponse> login(@Valid @RequestBody LoginRequest request,
                                            HttpServletRequest http) {
        return ApiResponse.ok(authService.login(request, http));
    }

    /**
     * 小程序微信登录：用 {@code wx.login()} 的 code 换取 openid，再按手机号匹配教师或识别家长。
     *
     * <p>判定顺序（与代码分支一致）：</p>
     * <ol>
     *   <li>传了 {@code phone} 且命中启用状态的教师 → 把 openid 绑到该教师并登录；</li>
     *   <li>openid 已存在 → 直接登录对应家长/教师账号；后台 ADMIN 账号禁止小程序登录；</li>
     *   <li>都没有且未传 {@code phone} → 自动创建家长账号与 {@code parent_profile}；</li>
     *   <li>都没有但传了 {@code phone} → 404 提示核对手机号，<b>不会</b>回退成新建家长账号。</li>
     * </ol>
     *
     * <p>副作用：写 {@code sys_user}（openid、最后登录时间/IP，必要时新建账号）、
     * 新建 {@code parent_profile}、写 {@code sys_audit_log}，并签发 JWT。</p>
     *
     * @param request 微信登录参数，code 必填，phone/name 可选
     * @param http    当前 HTTP 请求，用于解析客户端 IP 写审计
     * @return 登录响应，{@code roleCode} 为 PARENT 或 TEACHER
     * @throws BusinessException 401 微信 code 无效；403 账号停用或为后台账号；
     *         404 教师手机号未匹配；409 微信已绑定其他账号；500 未配置 AppID/AppSecret
     */
    @Operation(summary = "小程序微信登录（家长自动建号 / 教师手机号匹配）")
    @PostMapping("/wx-login")
    public ApiResponse<LoginResponse> wxLogin(@Valid @RequestBody WxLoginRequest request,
                                              HttpServletRequest http) {
        return ApiResponse.ok(authService.wxLogin(request, http));
    }

    /**
     * 查询当前登录用户的用户信息与权限点。
     *
     * <p>不写库、不续签 Token：返回体中 {@code token} 固定为 {@code null}，客户端沿用原 Token。
     * 账号被停用或删除时返回 401（逻辑删除的账号在 JWT 过滤器中已无法通过认证）。</p>
     *
     * @param loginUser JWT 过滤器解析出的登录身份
     * @return 当前用户信息与权限点，token 为 null
     * @throws BusinessException 401 账号不存在或已停用
     */
    @Operation(summary = "当前登录用户与权限")
    @GetMapping("/me")
    public ApiResponse<LoginResponse> me(@AuthenticationPrincipal LoginUser loginUser) {
        return ApiResponse.ok(authService.me(loginUser));
    }

    /**
     * 修改后台账号密码。
     *
     * <p>教师/家长是微信登录、没有密码哈希，调用本接口返回 400。
     * 新密码长度 8–32，且不能与原密码相同。</p>
     *
     * <p>副作用：更新 {@code sys_user.password_hash}、清空强制改密标记，写 {@code sys_audit_log}。
     * <b>不会</b>作废已签发的旧 Token（无黑名单/版本号），旧 Token 在剩余有效期内仍可用。</p>
     *
     * @param loginUser 当前登录身份
     * @param request   原密码与新密码
     * @param http      当前 HTTP 请求，用于写审计 IP
     * @throws BusinessException 400 账号不支持密码登录、原密码错误或新旧密码相同
     */
    @Operation(summary = "修改密码（后台账号）")
    @PostMapping("/change-password")
    public ApiResponse<Void> changePassword(@AuthenticationPrincipal LoginUser loginUser,
                                            @Valid @RequestBody ChangePasswordRequest request,
                                            HttpServletRequest http) {
        authService.changePassword(loginUser, request, http);
        return ApiResponse.ok();
    }

    /**
     * 退出登录。
     *
     * <p>服务端无会话状态，这里只返回成功；真正的退出是客户端删除本地 Token 与用户缓存。
     * 该 Token 在有效期内仍可继续使用，服务端不做拉黑。</p>
     *
     * @return 恒为成功的空响应
     */
    @Operation(summary = "退出登录（客户端丢弃 Token）")
    @PostMapping("/logout")
    public ApiResponse<Void> logout() {
        return ApiResponse.ok();
    }
}
