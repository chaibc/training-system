package com.hanmo.calligraphy.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.*;
import lombok.Data;

/**
 * 请假申请，对应表 leave_request。
 *
 * <p>记录学员对某课次的请假、审批结果与补课状态；leave_no 唯一。</p>
 */
@Data
@TableName("leave_request")
public class LeaveRequest {

    @TableId(type = IdType.AUTO)
    private Long id;

    // 请假单号（唯一）
    private String leaveNo;
    // 请假学员（student.id）
    private Long studentId;
    // 请假课次（lesson.id）
    private Long lessonId;
    // 请假原因（leave_reason.id）
    private Long reasonId;
    // 原因补充说明
    private String reasonText;
    // 状态：PENDING 待审批 / APPROVED 已通过 / REJECTED 已驳回 / CANCELLED 已撤销
    private String status;
    // 是否扣减课时：1=扣 1 课时 / 0=不扣课时
    private Integer deductHours;
    // 提交时间
    private LocalDateTime submittedAt;
    // 审批人（sys_user.id）
    private Long approverId;
    // 审批时间
    private LocalDateTime approvedAt;
    // 驳回原因
    private String rejectReason;
    // 补课状态：NONE 无需补课 / PENDING 待安排 / ARRANGED 已安排
    private String makeupStatus;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
