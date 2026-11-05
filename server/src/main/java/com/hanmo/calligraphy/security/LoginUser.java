package com.hanmo.calligraphy.security;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class LoginUser {
    private Long id;
    private String username;
    private String name;
    private String userType;
    private String roleCode;
    private String roleName;
    private String dataScope;
}
