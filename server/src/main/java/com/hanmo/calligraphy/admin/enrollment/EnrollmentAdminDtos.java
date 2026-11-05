package com.hanmo.calligraphy.admin.enrollment;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public class EnrollmentAdminDtos {

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

    public record CreateEnrollmentRequest(
            Long studentId,
            Long courseId,
            Long classId,
            BigDecimal amount,
            String remark
    ) {}

    public record ConfirmRequest(Long classId, BigDecimal amount, String note) {}

    public record PayRequest(Long paymentMethodId, BigDecimal amount, String note) {}

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

    public record FollowUpRequest(String status, String note, LocalDate nextFollowUp) {}

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
