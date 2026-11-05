package com.hanmo.calligraphy.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.*;
import lombok.Data;

/**
 * 家长-学员绑定，对应表 parent_student。
 *
 * <p>记录家长账号与学员的亲属关系、绑定状态与审核信息；
 * 唯一键 (parent_user_id, student_id) 防止重复绑定。</p>
 */
@Data
@TableName("parent_student")
public class ParentStudent {

    @TableId(type = IdType.AUTO)
    private Long id;

    // 家长账号（sys_user.id）
    private Long parentUserId;
    // 学员 ID（student.id）
    private Long studentId;
    // 亲属关系：MOTHER 母亲 / FATHER 父亲 / GRANDPARENT 祖辈 / OTHER 其他
    private String relation;
    // 是否主绑定家长：1=是 / 0=否
    private Integer isPrimary;
    // 绑定状态：PENDING 待审核 / ACTIVE 已生效 / REJECTED 已驳回 / REVOKED 已解除
    private String status;
    // 绑定来源：SELF 家长自助 / ADMIN 后台 / UNBIND 解绑
    private String source;
    // 审核人（sys_user.id）
    private Long verifiedBy;
    // 审核时间
    private LocalDateTime verifiedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
