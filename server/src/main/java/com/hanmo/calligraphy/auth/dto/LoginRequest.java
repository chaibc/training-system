package com.hanmo.calligraphy.auth.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 后台账号登录请求体（{@code POST /api/auth/login}）。
 *
 * <p>字段非空由 Bean Validation 在进入 Controller 前校验；密码为明文提交，
 * 生产环境必须走 HTTPS。</p>
 */
@Data
public class LoginRequest {
    /** 登录用户名，对应 sys_user.username */
    @NotBlank(message = "请输入用户名")
    private String username;

    /** 登录明文密码，仅用于与 BCrypt 哈希比对，不落库 */
    @NotBlank(message = "请输入密码")
    private String password;
}
