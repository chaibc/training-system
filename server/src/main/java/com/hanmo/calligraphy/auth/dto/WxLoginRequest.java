package com.hanmo.calligraphy.auth.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class WxLoginRequest {
    @NotBlank(message = "缺少微信登录 code")
    private String code;

    /** 教师登录时由微信手机号授权传入；家长可为空 */
    private String phone;

    private String name;
}
