package com.hanmo.calligraphy.system.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 账号，对应表 sys_user。
 *
 * <p>后台、教师、家长共用一张账号表，用 user_type 区分；密码以 BCrypt 哈希保存，
 * 教师 / 家长可凭微信 openid 登录。逻辑删除由 @TableLogic 处理。</p>
 */
@Data
@TableName("sys_user")
public class SysUser {

    @TableId(type = IdType.AUTO)
    private Long id;

    // 后台账号用户名（教师 / 家长为空）
    private String username;
    // BCrypt 密码哈希（教师 / 家长为空，不支持密码登录）
    private String passwordHash;
    // 账号类型：ADMIN 后台 / TEACHER 教师 / PARENT 家长
    private String userType;
    // 角色 ID（sys_role.id）
    private Long roleId;
    private String name;
    // 手机号（教师微信登录按手机号匹配账号）
    private String phone;
    private String avatar;
    // 微信登录 openid（唯一）
    private String wechatOpenid;
    // 微信 unionid（同一开放平台下的统一标识）
    private String wechatUnionid;
    // 状态：ENABLED 正常 / DISABLED 停用 / LOCKED 锁定（当前登录锁定用 locked_until 表达）
    private String status;
    // 连续登录失败次数（后台密码登录累计 5 次触发锁定）
    private Integer failedAttempts;
    // 锁定截止时间（为空或已过期表示未锁定）
    private LocalDateTime lockedUntil;
    // 最近登录时间
    private LocalDateTime lastLoginAt;
    // 最近登录 IP
    private String lastLoginIp;
    // 是否要求下次登录修改密码：1=是 / 0=否
    private Integer forceChangePassword;
    // 备注
    private String remark;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // 逻辑删除标记：0 正常 / 1 已删除（查询由 @TableLogic 自动过滤）
    @TableLogic
    private Integer deleted;
}
