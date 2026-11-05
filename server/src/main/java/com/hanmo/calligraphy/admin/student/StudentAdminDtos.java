package com.hanmo.calligraphy.admin.student;

import jakarta.validation.constraints.NotBlank;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public class StudentAdminDtos {

    public record StudentRow(
            Long id,
            String studentNo,
            String name,
            String gender,
            Integer age,
            String school,
            String grade,
            String status,
            String className,
            String parentName,
            String parentPhone,
            BigDecimal remainingHours,
            BigDecimal totalHours,
            LocalDate validUntil,
            LocalDateTime lastLessonAt
    ) {}

    public record ParentView(Long userId, String name, String phone, String relation, boolean primary) {}

    public record ClassView(Long classId, String className, String courseName, String teacherName) {}

    public record LedgerView(Long id, String type, BigDecimal changeHours, BigDecimal balanceAfter,
                             String reason, LocalDateTime createdAt) {}

    public record AttendanceView(Long id, String lessonTitle, LocalDateTime recordedAt,
                                 String status, BigDecimal consumedHours, String note) {}

    public record StudentDetail(
            Long id,
            String studentNo,
            String name,
            String gender,
            LocalDate birthDate,
            String school,
            String grade,
            String status,
            String remark,
            BigDecimal totalHours,
            BigDecimal usedHours,
            BigDecimal remainingHours,
            LocalDate validUntil,
            List<ParentView> parents,
            List<ClassView> classes,
            List<LedgerView> ledger,
            List<AttendanceView> attendance
    ) {}

    public record CreateStudentRequest(
            @NotBlank(message = "请输入学员姓名") String name,
            String gender,
            LocalDate birthDate,
            String school,
            String grade,
            String remark
    ) {}

    public record UpdateStudentRequest(
            String name,
            String gender,
            LocalDate birthDate,
            String school,
            String grade,
            String status,
            String remark
    ) {}

    public record AdjustHoursRequest(
            @NotBlank(message = "请选择调整类型") String type,
            BigDecimal hours,
            @NotBlank(message = "请填写调整原因") String reason
    ) {}

    public record StudentOption(Long id, String name, String studentNo) {}
}
