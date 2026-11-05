package com.hanmo.calligraphy.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.*;
import lombok.Data;

/**
 * 家长作品授权，对应表 work_auth。
 *
 * <p>记录家长对学员作品展示的授权方式、范围与撤销状态；
 * 唯一键 (student_id, parent_user_id) 保证每位家长对每个学员只有一条授权。</p>
 */
@Data
@TableName("work_auth")
public class WorkAuth {

    @TableId(type = IdType.AUTO)
    private Long id;

    // 学员 ID（student.id）
    private Long studentId;
    // 授权家长账号（sys_user.id）
    private Long parentUserId;
    // 授权范围说明
    private String scope;
    // 授权方式：ELECTRONIC 电子 / PAPER 纸质 / WECHAT 微信
    private String method;
    // 授权日期
    private LocalDate authorizedAt;
    // 状态：ACTIVE 生效 / REVOKED 已撤销
    private String status;
    // 授权凭证地址
    private String evidenceUrl;
    private LocalDateTime createdAt;
}
