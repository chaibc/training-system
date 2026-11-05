package com.hanmo.calligraphy.admin.teacher;

import jakarta.validation.constraints.NotBlank;

public class TeacherAdminDtos {

    public record TeacherView(
            Long id,
            Long userId,
            String teacherNo,
            String name,
            String phone,
            String title,
            Integer years,
            String styles,
            String coursesText,
            String bio,
            String quote,
            String tags,
            String avatar,
            String status,
            boolean wechatBound,
            String accountStatus,
            Integer sort
    ) {}

    public record TeacherRequest(
            @NotBlank(message = "请输入教师姓名") String name,
            @NotBlank(message = "请输入手机号") String phone,
            String title,
            Integer years,
            String styles,
            String coursesText,
            String bio,
            String quote,
            String tags,
            String status,
            Integer sort
    ) {}
}
