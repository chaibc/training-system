package com.hanmo.calligraphy.auth.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class LoginResponse {
    private String token;
    private Long userId;
    private String username;
    private String name;
    private String userType;
    private String roleCode;
    private String roleName;
    private String dataScope;
    private Boolean forceChangePassword;
    private List<PermissionDto> permissions;
}
