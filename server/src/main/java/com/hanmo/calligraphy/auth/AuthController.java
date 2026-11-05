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

@Tag(name = "认证")
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @Operation(summary = "后台账号登录")
    @PostMapping("/login")
    public ApiResponse<LoginResponse> login(@Valid @RequestBody LoginRequest request,
                                            HttpServletRequest http) {
        return ApiResponse.ok(authService.login(request, http));
    }

    @Operation(summary = "小程序微信登录（家长自动建号 / 教师手机号匹配）")
    @PostMapping("/wx-login")
    public ApiResponse<LoginResponse> wxLogin(@Valid @RequestBody WxLoginRequest request,
                                              HttpServletRequest http) {
        return ApiResponse.ok(authService.wxLogin(request, http));
    }

    @Operation(summary = "当前登录用户与权限")
    @GetMapping("/me")
    public ApiResponse<LoginResponse> me(@AuthenticationPrincipal LoginUser loginUser) {
        return ApiResponse.ok(authService.me(loginUser));
    }

    @Operation(summary = "修改密码（后台账号）")
    @PostMapping("/change-password")
    public ApiResponse<Void> changePassword(@AuthenticationPrincipal LoginUser loginUser,
                                            @Valid @RequestBody ChangePasswordRequest request,
                                            HttpServletRequest http) {
        authService.changePassword(loginUser, request, http);
        return ApiResponse.ok();
    }

    @Operation(summary = "退出登录（客户端丢弃 Token）")
    @PostMapping("/logout")
    public ApiResponse<Void> logout() {
        return ApiResponse.ok();
    }
}
