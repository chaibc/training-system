package com.hanmo.calligraphy.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.math.BigDecimal;
import java.time.*;
import lombok.Data;

/**
 * 报名申请，对应表 enrollment。
 *
 * <p>记录家长在小程序或教务在后台提交的报名，经确认、缴费后转为正式报名；enrollment_no 唯一。</p>
 */
@Data
@TableName("enrollment")
public class Enrollment {

    @TableId(type = IdType.AUTO)
    private Long id;

    // 报名单号（唯一）
    private String enrollmentNo;
    // 报名学员（student.id）
    private Long studentId;
    // 报名课程（course.id）
    private Long courseId;
    // 意向 / 分配班级（class_group.id），可为空
    private Long classId;
    // 提交报名的家长账号（sys_user.id）
    private Long parentUserId;
    // 报名来源：MINI 小程序 / ADMIN 后台
    private String source;
    // 状态：PENDING_CONFIRM 待确认 / PENDING_PAYMENT 待缴费 / ENROLLED 已报名 / CANCELLED 已取消
    private String status;
    // 报名金额（单位：元，DECIMAL(10,2)）
    private BigDecimal amount;
    // 提交时间
    private LocalDateTime submittedAt;
    // 确认人（sys_user.id）
    private Long confirmedBy;
    // 确认时间
    private LocalDateTime confirmedAt;
    private String remark;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
