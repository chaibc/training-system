package com.hanmo.calligraphy.auth.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 小程序微信登录请求体（{@code POST /api/auth/wx-login}）。
 *
 * <p>{@code code} 来自 {@code wx.login()}；教师端额外传 {@code phone} 供后端匹配教师账号，
 * 家长首次登录留空即可；{@code name} 只用于家长自动建号时的默认称呼。</p>
 */
@Data
public class WxLoginRequest {
    /** wx.login() 返回的临时 code；开发模式可直接传 seed_parent_wang / seed_teacher_wang 等演示值 */
    @NotBlank(message = "缺少微信登录 code")
    private String code;

    /** 教师登录时由微信手机号授权传入；家长可为空 */
    private String phone;

    /** 家长首次登录自动建号时的称呼，为空则使用“微信用户” */
    private String name;
}
