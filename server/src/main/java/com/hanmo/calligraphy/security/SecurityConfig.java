package com.hanmo.calligraphy.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hanmo.calligraphy.common.ApiResponse;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;

/**
 * Spring Security 配置：无状态 JWT 鉴权、URL 级权限规则与 BCrypt 密码编码。
 *
 * <p>安全策略：关闭 CSRF 与 Session（纯 Bearer Token）；除登录、公开接口、静态资源、
 * 接口文档、健康检查与 H2 控制台外一律要求认证；/api/admin/** 按角色与 HTTP 方法细分权限。
 * 认证失败返回 401、权限不足返回 403，均为统一 JSON 结构。后台密码使用 BCrypt 校验（见 AuthService），
 * 连续 5 次密码错误会锁定账号 5 分钟（见 sys_user.failed_attempts / locked_until）。</p>
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final ObjectMapper objectMapper;

    @Value("${app.cors.allowed-origins}")
    private String allowedOrigins;

    /**
     * 是否放行 H2 控制台：只有显式开启 H2 控制台（dev/本地）时才注册这条放行规则。
     *
     * <p>生产 profile 下 {@code spring.h2.console.enabled=false}，因此 /h2-console/** 不会放行，
     * 即使有人误开了控制台也不会变成匿名可访问。</p>
     */
    @Value("${spring.h2.console.enabled:false}")
    private boolean h2ConsoleEnabled;

    /**
     * 密码编码器：BCrypt。
     *
     * <p>BCrypt 每次编码使用随机盐，密文自带盐值，校验用 matches 而非明文比较；
     * 由 AuthService 在登录与改密时使用。</p>
     *
     * @return BCryptPasswordEncoder 实例
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * 构建安全过滤器链。
     *
     * <p>规则顺序敏感：先放行公开路径、再收紧具体写操作，最后的 anyRequest 兜底要求认证；
     * JWT 过滤器在 UsernamePasswordAuthenticationFilter 之前执行。</p>
     *
     * @param http HttpSecurity 构建器
     * @return 安全过滤器链
     * @throws Exception 构建失败
     */
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        // 纯 Token 认证、不使用 Cookie 会话，关闭 CSRF；会话策略设为 STATELESS
        http.csrf(csrf -> csrf.disable())
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .headers(headers -> headers.frameOptions(frame -> frame.sameOrigin()))
                .authorizeHttpRequests(auth -> auth
                        // 登录、公开数据、静态资源、接口文档与健康检查无需令牌
                        .requestMatchers("/api/auth/login", "/api/auth/wx-login").permitAll()
                        .requestMatchers("/api/public/**").permitAll()
                        .requestMatchers("/uploads/**").permitAll()
                        .requestMatchers("/swagger-ui/**", "/swagger-ui.html", "/v3/api-docs/**").permitAll()
                        // H2 控制台只在开发环境开启时放行；生产（h2.console.enabled=false）换成永不匹配的占位路径
                        .requestMatchers(h2ConsoleEnabled ? "/h2-console/**" : "/__h2_console_disabled__/**").permitAll()
                        .requestMatchers("/actuator/health", "/actuator/info").permitAll()
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        // 教务在作品授权/展示中需要家长下拉；其余用户管理接口仍仅管理者
                        .requestMatchers(HttpMethod.GET,
                                "/api/admin/users/parent-options",
                                "/api/admin/users/student-options").hasAnyRole("ADMIN", "ACADEMIC")
                        .requestMatchers("/api/admin/users/**").hasRole("ADMIN")
                        // 教务对课程、基础字典、机构内容只读；所有写操作仅管理者
                        .requestMatchers(HttpMethod.POST,
                                "/api/admin/courses/**", "/api/admin/course-prices/**", "/api/admin/course-outlines/**",
                                "/api/admin/categories/**", "/api/admin/stages/**", "/api/admin/class-types/**",
                                "/api/admin/classrooms/**", "/api/admin/leave-reasons/**", "/api/admin/payment-methods/**",
                                "/api/admin/trial-slots/**", "/api/admin/org/**", "/api/admin/institution-images/**",
                                "/api/admin/qualifications/**", "/api/admin/notices/**", "/api/admin/rules/**",
                                "/api/admin/teachers/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PUT,
                                "/api/admin/courses/**", "/api/admin/course-prices/**", "/api/admin/course-outlines/**",
                                "/api/admin/categories/**", "/api/admin/stages/**", "/api/admin/class-types/**",
                                "/api/admin/classrooms/**", "/api/admin/leave-reasons/**", "/api/admin/payment-methods/**",
                                "/api/admin/trial-slots/**", "/api/admin/org/**", "/api/admin/institution-images/**",
                                "/api/admin/qualifications/**", "/api/admin/notices/**", "/api/admin/rules/**",
                                "/api/admin/teachers/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.DELETE,
                                "/api/admin/courses/**", "/api/admin/course-prices/**", "/api/admin/course-outlines/**",
                                "/api/admin/categories/**", "/api/admin/stages/**", "/api/admin/class-types/**",
                                "/api/admin/classrooms/**", "/api/admin/leave-reasons/**", "/api/admin/payment-methods/**",
                                "/api/admin/trial-slots/**", "/api/admin/org/**", "/api/admin/institution-images/**",
                                "/api/admin/qualifications/**", "/api/admin/notices/**", "/api/admin/rules/**",
                                "/api/admin/teachers/**").hasRole("ADMIN")
                        // 其余后台接口管理员与教务均可访问；写操作已在上方单独收紧为仅管理员
                        .requestMatchers("/api/admin/**").hasAnyRole("ADMIN", "ACADEMIC")
                        .requestMatchers("/api/mini/**").authenticated()
                        .requestMatchers("/api/auth/**").authenticated()
                        .anyRequest().authenticated()
                )
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint((request, response, e) -> writeJson(response, 401, "未登录或登录已失效"))
                        .accessDeniedHandler((request, response, e) -> writeJson(response, 403, "没有权限执行该操作"))
                )
                // 先执行 JWT 过滤器恢复登录态，再交给用户名密码过滤器链
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    // 以统一 ApiResponse JSON 结构输出认证 / 授权失败，避免 Spring 默认的 HTML 错误页
    private void writeJson(HttpServletResponse response, int code, String message) throws java.io.IOException {
        response.setStatus(code);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.getWriter().write(objectMapper.writeValueAsString(ApiResponse.fail(code, message)));
    }

    /**
     * CORS 配置：允许管理端等来源跨域访问，并暴露 Content-Disposition 供下载场景读取文件名。
     *
     * @return 作用于 /** 的 CORS 配置源
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        // 允许来源来自 app.cors.allowed-origins（逗号分隔），忽略空白项
        config.setAllowedOrigins(Arrays.stream(allowedOrigins.split(",")).map(String::trim).filter(s -> !s.isEmpty()).toList());
        config.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(Arrays.asList("Authorization", "Content-Type", "X-Requested-With"));
        config.setExposedHeaders(Arrays.asList("Content-Disposition"));
        // 允许携带凭证，因此允许来源不能配置为通配符 *
        config.setAllowCredentials(true);
        config.setMaxAge(3600L);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}
