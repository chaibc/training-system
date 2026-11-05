package com.hanmo.calligraphy.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.*;
import lombok.Data;

/**
 * 作品评论，对应表 work_comment。
 *
 * <p>家长对作品的评论需审核后展示。</p>
 */
@Data
@TableName("work_comment")
public class WorkComment {

    @TableId(type = IdType.AUTO)
    private Long id;

    // 所属作品（work.id）
    private Long workId;
    // 评论家长账号（sys_user.id）
    private Long parentUserId;
    // 评论内容
    private String content;
    // 状态：PENDING 待审核 / APPROVED 已通过 / HIDDEN 已隐藏 / REJECTED 已驳回
    private String status;
    // 审核人（sys_user.id）
    private Long reviewedBy;
    // 审核时间
    private LocalDateTime reviewedAt;
    private LocalDateTime createdAt;
}
