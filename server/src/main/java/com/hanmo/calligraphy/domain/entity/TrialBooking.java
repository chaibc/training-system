package com.hanmo.calligraphy.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.*;
import lombok.Data;

/**
 * 试听预约，对应表 trial_booking。
 *
 * <p>记录潜客的试听预约、跟进状态与下次跟进日期；booking_no 唯一。</p>
 */
@Data
@TableName("trial_booking")
public class TrialBooking {

    @TableId(type = IdType.AUTO)
    private Long id;

    // 预约单号（唯一）
    private String bookingNo;
    // 学员姓名
    private String studentName;
    // 年龄（按文本填写）
    private String age;
    // 家长姓名
    private String parentName;
    // 联系电话
    private String phone;
    // 意向课程（course.id）
    private Long courseId;
    // 试听时段（trial_slot.id）
    private Long slotId;
    private String note;
    // 状态：NEW 新预约 / CONTACTED 已联系 / VISITED 已到访 / CONVERTED 已转化 / CANCELLED 已取消
    private String status;
    // 跟进记录
    private String followUpNote;
    // 下次跟进日期
    private LocalDate nextFollowUp;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
