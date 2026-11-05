package com.hanmo.calligraphy.system.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 登录与账号变更审计，对应表 sys_audit_log。
 *
 * <p>记录登录、改密、绑定等操作，供事后追溯；日志只追加，不提供修改 / 删除。</p>
 */
@Data
@TableName("sys_audit_log")
public class SysAuditLog {

    @TableId(type = IdType.AUTO)
    private Long id;

    // 操作人账号（sys_user.id），登录失败等场景可为空
    private Long userId;
    // 操作人姓名快照（避免账号改名后追溯失真）
    private String operatorName;
    // 操作类型：LOGIN 登录 / ACCOUNT 账号 / BINDING 绑定 / PASSWORD 密码 / WORK 作品 / PAYMENT 缴费（代码实际写入中文动作名）
    private String actionType;
    // 操作对象类型
    private String targetType;
    // 操作对象 ID
    private Long targetId;
    // 操作内容
    private String content;
    // 来源 IP
    private String ip;
    // 操作结果：SUCCESS 成功 / FAIL 失败
    private String result;
    private LocalDateTime createdAt;
}
