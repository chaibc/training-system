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

@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final SysUserMapper userMapper;
    private final SysRoleMapper roleMapper;

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
                if (user != null && "ENABLED".equals(user.getStatus()) && (user.getDeleted() == null || user.getDeleted() == 0)) {
                    SysRole role = roleMapper.selectById(user.getRoleId());
                    LoginUser principal = new LoginUser(
                            user.getId(), user.getUsername(), user.getName(), user.getUserType(),
                            role == null ? null : role.getCode(),
                            role == null ? null : role.getName(),
                            role == null ? null : role.getDataScope()
                    );
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
