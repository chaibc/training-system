package com.hanmo.calligraphy.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 修改密码请求体（{@code POST /api/auth/change-password}）。
 *
 * <p>仅后台账号（有密码的 ADMIN/ACADEMIC）可用；教师、家长是微信登录，没有密码可改。</p>
 */
@Data
public class ChangePasswordRequest {
    /** 原密码，用于二次确认身份 */
    @NotBlank(message = "请输入原密码")
    private String oldPassword;

    /** 新密码，长度 8–32；Service 还会校验其不能与原密码相同 */
    @NotBlank(message = "请输入新密码")
    @Size(min = 8, max = 32, message = "新密码长度需为 8–32 位")
    private String newPassword;
}
