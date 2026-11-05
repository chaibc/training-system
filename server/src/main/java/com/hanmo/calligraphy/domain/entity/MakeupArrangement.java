package com.hanmo.calligraphy.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.*;
import lombok.Data;

/**
 * 补课安排，对应表 makeup_arrangement。
 *
 * <p>把已批准的请假学员安排到另一课次补课。</p>
 */
@Data
@TableName("makeup_arrangement")
public class MakeupArrangement {

    @TableId(type = IdType.AUTO)
    private Long id;

    // 对应请假申请（leave_request.id）
    private Long leaveRequestId;
    // 补课学员（student.id）
    private Long studentId;
    // 补课插入的课次（lesson.id）
    private Long lessonId;
    // 安排状态（默认 ARRANGED）
    private String status;
    // 安排人（sys_user.id）
    private Long arrangedBy;
    // 安排时间
    private LocalDateTime arrangedAt;
    private String note;
    private LocalDateTime createdAt;
}
