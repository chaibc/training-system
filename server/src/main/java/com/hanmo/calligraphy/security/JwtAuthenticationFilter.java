package com.hanmo.calligraphy.security;

import com.hanmo.calligraphy.system.entity.SysRole;
import com.hanmo.calligraphy.system.entity.SysUser;
import com.hanmo.calligraphy.system.mapper.SysRoleMapper;
import com.hanmo.calligraphy.system.mapper.SysUserMapper;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/**
 * JWT 认证过滤器。
 *
 * <p>每个请求读取 Authorization: Bearer 令牌，校验后按用户 ID 重新加载账号与角色，
 * 构造 LoginUser 与 ROLE_ 前缀权限放入 SecurityContext。令牌无效或账号不可用时不抛异常、
 * 保持未认证，由 SecurityConfig 的入口点统一返回 JSON 401。</p>
 */
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final SysUserMapper userMapper;
    private final SysRoleMapper roleMapper;

    /**
     * 解析请求头中的 Bearer 令牌并恢复登录态。
     *
     * <p>只做认证不做授权；账号状态、逻辑删除标记与角色每次请求都从数据库读取，
     * 因此停用账号或调整角色后无需等待令牌过期即可生效。</p>
     *
     * @param request     当前请求
     * @param response    当前响应
     * @param filterChain 过滤器链
     * @throws ServletException Servlet 异常
     * @throws IOException      IO 异常
     */
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith("Bearer ") && SecurityContextHolder.getContext().getAuthentication() == null) {
            String token = header.substring(7);
            try {
                Claims claims = jwtService.parse(token);
                Long uid = claims.get("uid", Long.class);
                SysUser user = userMapper.selectById(uid);
                // 令牌有效还不够：账号必须仍存在、状态为 ENABLED、未被逻辑删除，
                // 且密码指纹与令牌内一致（改密后旧令牌立即失效）
                if (user != null && "ENABLED".equals(user.getStatus())
                        && (user.getDeleted() == null || user.getDeleted() == 0)
                        && JwtService.passwordFingerprint(user).equals(claims.get("pwd", String.class))) {
                    SysRole role = roleMapper.selectById(user.getRoleId());
                    LoginUser principal = new LoginUser(
                            user.getId(), user.getUsername(), user.getName(), user.getUserType(),
                            role == null ? null : role.getCode(),
                            role == null ? null : role.getName(),
                            role == null ? null : role.getDataScope()
                    );
                    // hasRole/hasAnyRole 约定权限名必须带 ROLE_ 前缀
                    List<SimpleGrantedAuthority> authorities = role == null
                            ? List.of()
                            : List.of(new SimpleGrantedAuthority("ROLE_" + role.getCode()));
                    UsernamePasswordAuthenticationToken auth =
                            new UsernamePasswordAuthenticationToken(principal, null, authorities);
                    SecurityContextHolder.getContext().setAuthentication(auth);
                }
            } catch (Exception ignored) {
                // Invalid token: leave unauthenticated; entry point returns JSON 401.
            }
        }
        filterChain.doFilter(request, response);
    }
}
