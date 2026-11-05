package com.hanmo.calligraphy.admin.student;

import jakarta.validation.constraints.NotBlank;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 学员管理模块的请求/响应 DTO 容器。
 *
 * <p>响应对象只给展示字段：手机号已脱敏、年龄由生日换算；请求对象不含学员编号与状态流转字段
 * （状态在编辑接口里改）。</p>
 */
public class StudentAdminDtos {

    /**
     * 学员列表行：班级取第一个 ACTIVE 班级，主监护人缺失时显示「未绑定」，无班级显示「未分班」。
     */
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

    /**
     * 档案中的监护人视图：phone 已脱敏，relation 已转中文，primary 表示是否主联系人。
     */
    public record ParentView(Long userId, String name, String phone, String relation, boolean primary) {}

    /**
     * 档案中的班级视图：课程或教师被删除时对应名称为空串。
     */
    public record ClassView(Long classId, String className, String courseName, String teacherName) {}

    /**
     * 课时流水视图：type 为 PURCHASE/CONSUME/CORRECT，changeHours 带符号，balanceAfter 为变动后余额。
     */
    public record LedgerView(Long id, String type, BigDecimal changeHours, BigDecimal balanceAfter,
                             String reason, LocalDateTime createdAt) {}

    /**
     * 上课记录视图：lessonTitle 形如「第 N 课」，课次删除后显示「课次已删除」。
     */
    public record AttendanceView(Long id, String lessonTitle, LocalDateTime recordedAt,
                                 String status, BigDecimal consumedHours, String note) {}

    /**
     * 学员档案详情：课时账户三值 + 有效期，附带家长、班级、流水与上课记录。
     */
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

    /**
     * 新建学员请求：姓名必填，其余可空；不接收学员编号与状态（由 service 生成/置默认）。
     */
    public record CreateStudentRequest(
            @NotBlank(message = "请输入学员姓名") String name,
            String gender,
            LocalDate birthDate,
            String school,
            String grade,
            String remark
    ) {}

    /**
     * 编辑学员请求：字段为 null 表示不修改；status 可改 ACTIVE/PAUSED/GRADUATED/LEFT。
     */
    public record UpdateStudentRequest(
            String name,
            String gender,
            LocalDate birthDate,
            String school,
            String grade,
            String status,
            String remark
    ) {}

    /**
     * 调整课时请求：type 为 ADD/DEDUCT/CORRECT，hours 带符号语义由 type 决定，reason 必填。
     */
    public record AdjustHoursRequest(
            @NotBlank(message = "请选择调整类型") String type,
            BigDecimal hours,
            @NotBlank(message = "请填写调整原因") String reason
    ) {}

    /**
     * 学员下拉选项。
     */
    public record StudentOption(Long id, String name, String studentNo) {}
}
