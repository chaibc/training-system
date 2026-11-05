package com.hanmo.calligraphy.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.*;
import lombok.Data;

/**
 * 教师档案，对应表 teacher_profile。
 *
 * <p>与账号（sys_user）一对一（唯一键 uk_teacher_user），保存教龄、书体、简介等展示信息。</p>
 */
@Data
@TableName("teacher_profile")
public class TeacherProfile {

    @TableId(type = IdType.AUTO)
    private Long id;

    // 关联账号（sys_user.id，唯一）
    private Long userId;
    // 教师工号（唯一）
    private String teacherNo;
    private String name;
    // 职称 / 头衔
    private String title;
    // 教龄（年）
    private Integer years;
    // 擅长书体
    private String styles;
    // 主讲课程（展示文本）
    private String coursesText;
    // 个人简介
    private String bio;
    // 教学格言
    private String quote;
    // 教师标签
    private String tags;
    private String avatar;
    // 状态：ACTIVE 在职 / DISABLED 停用
    private String status;
    private Integer sort;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    // 逻辑删除标记：0 正常 / 1 已删除（查询由 @TableLogic 自动过滤）
    @TableLogic
    private Integer deleted;

}
