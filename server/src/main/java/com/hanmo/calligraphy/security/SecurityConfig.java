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

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final ObjectMapper objectMapper;

    @Value("${app.cors.allowed-origins}")
    private String allowedOrigins;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http.csrf(csrf -> csrf.disable())
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .headers(headers -> headers.frameOptions(frame -> frame.sameOrigin()))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/auth/login", "/api/auth/wx-login").permitAll()
                        .requestMatchers("/api/public/**").permitAll()
                        .requestMatchers("/uploads/**").permitAll()
                        .requestMatchers("/swagger-ui/**", "/swagger-ui.html", "/v3/api-docs/**").permitAll()
                        .requestMatchers("/h2-console/**").permitAll()
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
                        .requestMatchers("/api/admin/**").hasAnyRole("ADMIN", "ACADEMIC")
                        .requestMatchers("/api/mini/**").authenticated()
                        .requestMatchers("/api/auth/**").authenticated()
                        .anyRequest().authenticated()
                )
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint((request, response, e) -> writeJson(response, 401, "未登录或登录已失效"))
                        .accessDeniedHandler((request, response, e) -> writeJson(response, 403, "没有权限执行该操作"))
                )
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    private void writeJson(HttpServletResponse response, int code, String message) throws java.io.IOException {
        response.setStatus(code);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.getWriter().write(objectMapper.writeValueAsString(ApiResponse.fail(code, message)));
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(Arrays.stream(allowedOrigins.split(",")).map(String::trim).filter(s -> !s.isEmpty()).toList());
        config.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(Arrays.asList("Authorization", "Content-Type", "X-Requested-With"));
        config.setExposedHeaders(Arrays.asList("Content-Disposition"));
        config.setAllowCredentials(true);
        config.setMaxAge(3600L);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}
