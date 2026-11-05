package com.hanmo.calligraphy.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.*;
import lombok.Data;

@Data
@TableName("trial_booking")
public class TrialBooking {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String bookingNo;
    private String studentName;
    private String age;
    private String parentName;
    private String phone;
    private Long courseId;
    private Long slotId;
    private String note;
    private String status;
    private String followUpNote;
    private LocalDate nextFollowUp;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
