package com.hanmo.calligraphy.admin.enrollment;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 报名与缴费模块的请求/响应 DTO 容器。
 *
 * <p>只承载字段，不含业务逻辑；状态字段取值与 enrollment.status / trial_booking.status 一致。
 * 金额统一 BigDecimal，时间沿用服务器本地时区。</p>
 */
public class EnrollmentAdminDtos {

    /**
     * 报名列表行。
     *
     * <p>由 service 拼装：班级/家长可能为空，手机号已脱敏，status 为原始英文枚举。</p>
     */
    public record EnrollmentRow(
            Long id,
            String enrollmentNo,
            Long studentId,
            String studentName,
            Long courseId,
            String courseName,
            Long classId,
            String className,
            String parentName,
            String parentPhone,
            BigDecimal amount,
            String status,
            LocalDateTime submittedAt,
            LocalDateTime confirmedAt
    ) {}

    /**
     * 后台新建报名请求。
     *
     * <p>classId 可空（先报名后分班）；amount 为应缴金额，缴费时仍可调整。</p>
     */
    public record CreateEnrollmentRequest(
            Long studentId,
            Long courseId,
            Long classId,
            BigDecimal amount,
            String remark
    ) {}

    /**
     * 确认报名请求：字段均可空，非空时覆盖原报名班级/金额；note 当前未被 service 使用。
     */
    public record ConfirmRequest(Long classId, BigDecimal amount, String note) {}

    /**
     * 登记缴费请求：paymentMethodId 必填；amount 为空时取报名应缴金额。
     */
    public record PayRequest(Long paymentMethodId, BigDecimal amount, String note) {}

    /**
     * 试听预约列表行：手机号脱敏，课程/时段名称缺失时为空串。
     */
    public record TrialRow(
            Long id,
            String bookingNo,
            String studentName,
            String age,
            String parentName,
            String phone,
            String courseName,
            String slotName,
            String status,
            String followUpNote,
            LocalDate nextFollowUp,
            LocalDateTime createdAt
    ) {}

    /**
     * 试听跟进请求：status/note/nextFollowUp 为 null 表示不修改对应字段。
     */
    public record FollowUpRequest(String status, String note, LocalDate nextFollowUp) {}

    /**
     * 缴费记录行：status 固定由 service 填「已到账」（本期无线上支付）。
     */
    public record PaymentRow(
            Long id,
            String paymentNo,
            String studentName,
            String item,
            BigDecimal amount,
            String methodName,
            String registeredBy,
            LocalDateTime paidAt,
            String status
    ) {}
}
