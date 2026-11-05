package com.hanmo.calligraphy.admin.teacher;

import jakarta.validation.constraints.NotBlank;

/**
 * 教师管理模块的请求/响应 DTO 容器。
 *
 * <p>TeacherView 相比实体多带手机号、账号状态与微信绑定标记（来自 sys_user），
 * 供后台列表展示；请求体不含教师编号（由服务端生成）。</p>
 */
public class TeacherAdminDtos {

    /**
     * 教师列表/详情视图。
     *
     * <p>wechatBound 表示 sys_user.wechat_openid 是否非空；accountStatus 是登录账号状态，
     * 与档案 status 成对维护但不完全等价。</p>
     */
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

    /**
     * 教师建档/编辑请求：姓名与手机号必填（11 位），其余可空；status 为 DISABLED 表示停用，
     * 其它值一律按启用处理。
     */
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
