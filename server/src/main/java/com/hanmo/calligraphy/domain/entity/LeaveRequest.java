package com.hanmo.calligraphy.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.*;
import lombok.Data;

@Data
@TableName("leave_request")
public class LeaveRequest {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String leaveNo;
    private Long studentId;
    private Long lessonId;
    private Long reasonId;
    private String reasonText;
    private String status;
    private Integer deductHours;
    private LocalDateTime submittedAt;
    private Long approverId;
    private LocalDateTime approvedAt;
    private String rejectReason;
    private String makeupStatus;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
