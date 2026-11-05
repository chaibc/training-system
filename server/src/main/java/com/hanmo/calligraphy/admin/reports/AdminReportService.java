package com.hanmo.calligraphy.admin.reports;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.hanmo.calligraphy.domain.entity.Attendance;
import com.hanmo.calligraphy.domain.entity.BusinessRule;
import com.hanmo.calligraphy.domain.entity.ClassGroup;
import com.hanmo.calligraphy.domain.entity.ClassStudent;
import com.hanmo.calligraphy.domain.entity.Enrollment;
import com.hanmo.calligraphy.domain.entity.HourAccount;
import com.hanmo.calligraphy.domain.entity.HourLedger;
import com.hanmo.calligraphy.domain.entity.LeaveRequest;
import com.hanmo.calligraphy.domain.entity.Lesson;
import com.hanmo.calligraphy.domain.entity.PaymentMethod;
import com.hanmo.calligraphy.domain.entity.PaymentRecord;
import com.hanmo.calligraphy.domain.entity.Student;
import com.hanmo.calligraphy.domain.entity.TeacherProfile;
import com.hanmo.calligraphy.domain.entity.TrialBooking;
import com.hanmo.calligraphy.domain.mapper.AttendanceMapper;
import com.hanmo.calligraphy.domain.mapper.BusinessRuleMapper;
import com.hanmo.calligraphy.domain.mapper.ClassGroupMapper;
import com.hanmo.calligraphy.domain.mapper.ClassStudentMapper;
import com.hanmo.calligraphy.domain.mapper.EnrollmentMapper;
import com.hanmo.calligraphy.domain.mapper.HourAccountMapper;
import com.hanmo.calligraphy.domain.mapper.HourLedgerMapper;
import com.hanmo.calligraphy.domain.mapper.LeaveRequestMapper;
import com.hanmo.calligraphy.domain.mapper.LessonMapper;
import com.hanmo.calligraphy.domain.mapper.PaymentMethodMapper;
import com.hanmo.calligraphy.domain.mapper.PaymentRecordMapper;
import com.hanmo.calligraphy.domain.mapper.StudentMapper;
import com.hanmo.calligraphy.domain.mapper.TeacherProfileMapper;
import com.hanmo.calligraphy.domain.mapper.TrialBookingMapper;
import com.hanmo.calligraphy.common.BusinessException;
import com.hanmo.calligraphy.security.LoginUser;
import com.hanmo.calligraphy.system.entity.SysAuditLog;
import com.hanmo.calligraphy.system.mapper.SysAuditLogMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.ThreadLocalRandom;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AdminReportService {

    private final EnrollmentMapper enrollmentMapper;
    private final PaymentRecordMapper paymentMapper;
    private final PaymentMethodMapper paymentMethodMapper;
    private final StudentMapper studentMapper;
    private final ClassGroupMapper classGroupMapper;
    private final ClassStudentMapper classStudentMapper;
    private final LessonMapper lessonMapper;
    private final AttendanceMapper attendanceMapper;
    private final LeaveRequestMapper leaveMapper;
    private final TeacherProfileMapper teacherMapper;
    private final HourAccountMapper hourAccountMapper;
    private final TrialBookingMapper trialMapper;
    private final BusinessRuleMapper ruleMapper;
    private final HourLedgerMapper hourLedgerMapper;
    private final SysAuditLogMapper auditLogMapper;

    public Map<String, Object> overview(int months) {
        int span = months <= 1 ? 1 : months <= 6 ? 6 : 12;
        LocalDate end = LocalDate.now();
        LocalDate start = end.minusMonths(span - 1).withDayOfMonth(1);
        LocalDateTime from = start.atStartOfDay();
        LocalDateTime to = end.atTime(23, 59, 59);

        List<Enrollment> enrollments = enrollmentMapper.selectList(new LambdaQueryWrapper<Enrollment>()
                .ge(Enrollment::getSubmittedAt, from).le(Enrollment::getSubmittedAt, to));
        List<PaymentRecord> payments = paymentMapper.selectList(new LambdaQueryWrapper<PaymentRecord>()
                .ge(PaymentRecord::getPaidAt, from).le(PaymentRecord::getPaidAt, to));
        List<Lesson> lessons = lessonMapper.selectList(new LambdaQueryWrapper<Lesson>()
                .ge(Lesson::getLessonDate, start).le(Lesson::getLessonDate, end));
        Set<Long> lessonIds = lessons.stream().map(Lesson::getId).collect(Collectors.toSet());
        List<Attendance> attendance = lessonIds.isEmpty() ? List.of()
                : attendanceMapper.selectList(new LambdaQueryWrapper<Attendance>()
                .in(Attendance::getLessonId, lessonIds));
        List<LeaveRequest> leaves = leaveMapper.selectList(new LambdaQueryWrapper<LeaveRequest>()
                .ge(LeaveRequest::getSubmittedAt, from).le(LeaveRequest::getSubmittedAt, to));

        long activeStudents = studentMapper.selectCount(new LambdaQueryWrapper<Student>()
                .eq(Student::getStatus, "ACTIVE"));
        BigDecimal revenue = payments.stream().map(PaymentRecord::getAmount)
                .filter(x -> x != null).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal consumed = attendance.stream().map(Attendance::getConsumedHours)
                .filter(x -> x != null).reduce(BigDecimal.ZERO, BigDecimal::add);
        long present = attendance.stream().filter(a -> "PRESENT".equals(a.getStatus())).count();
        long leaveCount = attendance.stream().filter(a -> "LEAVE".equals(a.getStatus())).count();
        long absent = attendance.stream().filter(a -> "ABSENT".equals(a.getStatus())).count();
        long marked = present + leaveCount + absent;
        long renewPayments = payments.stream().filter(p -> "RENEW".equals(p.getType())).count();
        Set<Long> renewStudents = payments.stream().filter(p -> "RENEW".equals(p.getType()))
                .map(PaymentRecord::getStudentId).collect(Collectors.toSet());
        int threshold = ruleInt("renewal_threshold", 3);
        List<HourAccount> accounts = hourAccountMapper.selectList(new LambdaQueryWrapper<>());
        long warning = accounts.stream().filter(a -> a.getRemainingHours() != null
                && a.getRemainingHours().compareTo(BigDecimal.valueOf(threshold)) <= 0).count();

        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("enrollments", enrollments.size());
        stats.put("newEnrollments", enrollments.stream().filter(e -> "ENROLLED".equals(e.getStatus())
                || "PENDING_CONFIRM".equals(e.getStatus()) || "PENDING_PAYMENT".equals(e.getStatus())).count());
        stats.put("renewPayments", renewPayments);
        stats.put("consumedHours", consumed);
        stats.put("attendanceRate", pct(present, marked));
        stats.put("renewalRate", pct(renewStudents.size(), activeStudents));
        stats.put("activeStudents", activeStudents);
        stats.put("warningStudents", warning);
        stats.put("revenue", revenue);
        stats.put("leaveCount", leaves.size());
        stats.put("renewalThreshold", threshold);

        // 近 6 个月报名 / 试听走势
        int chartMonths = Math.max(6, span);
        LocalDate chartStart = end.minusMonths(chartMonths - 1).withDayOfMonth(1);
        List<Enrollment> chartEnrollments = enrollmentMapper.selectList(new LambdaQueryWrapper<Enrollment>()
                .ge(Enrollment::getSubmittedAt, chartStart.atStartOfDay()).le(Enrollment::getSubmittedAt, to));
        List<TrialBooking> trials = trialMapper.selectList(new LambdaQueryWrapper<TrialBooking>()
                .ge(TrialBooking::getCreatedAt, chartStart.atStartOfDay()).le(TrialBooking::getCreatedAt, to));
        Map<YearMonth, Long> enrollByMonth = chartEnrollments.stream()
                .filter(e -> e.getSubmittedAt() != null)
                .collect(Collectors.groupingBy(e -> YearMonth.from(e.getSubmittedAt()), Collectors.counting()));
        Map<YearMonth, Long> trialByMonth = trials.stream()
                .filter(t -> t.getCreatedAt() != null)
                .collect(Collectors.groupingBy(t -> YearMonth.from(t.getCreatedAt()), Collectors.counting()));
        List<Map<String, Object>> chart = new ArrayList<>();
        for (int i = chartMonths - 1; i >= 0; i--) {
            YearMonth ym = YearMonth.from(end).minusMonths(i);
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("month", ym.getMonthValue() + " 月");
            row.put("key", ym.toString());
            row.put("enrollments", enrollByMonth.getOrDefault(ym, 0L));
            row.put("trials", trialByMonth.getOrDefault(ym, 0L));
            chart.add(row);
        }

        Map<Long, String> methodNames = paymentMethodMapper.selectList(null).stream()
                .collect(Collectors.toMap(PaymentMethod::getId, PaymentMethod::getName, (a, b) -> a));
        Map<Long, BigDecimal> amountByMethod = new HashMap<>();
        for (PaymentRecord p : payments) {
            Long mid = p.getPaymentMethodId() == null ? -1L : p.getPaymentMethodId();
            amountByMethod.merge(mid, p.getAmount() == null ? BigDecimal.ZERO : p.getAmount(), BigDecimal::add);
        }
        List<Map<String, Object>> revenueByMethod = amountByMethod.entrySet().stream()
                .sorted((a, b) -> b.getValue().compareTo(a.getValue()))
                .map(e -> {
                    Map<String, Object> row = new LinkedHashMap<>();
                    row.put("name", methodNames.getOrDefault(e.getKey(), "未登记方式"));
                    row.put("amount", e.getValue());
                    row.put("rate", revenue.compareTo(BigDecimal.ZERO) == 0 ? BigDecimal.ZERO
                            : e.getValue().multiply(BigDecimal.valueOf(100))
                            .divide(revenue, 1, RoundingMode.HALF_UP));
                    return row;
                }).toList();

        // 教师统计
        List<TeacherProfile> teachers = teacherMapper.selectList(new LambdaQueryWrapper<TeacherProfile>()
                .eq(TeacherProfile::getStatus, "ACTIVE").orderByAsc(TeacherProfile::getSort));
        List<ClassGroup> classes = classGroupMapper.selectList(new LambdaQueryWrapper<>());
        Map<Long, Long> lessonTeacher = lessons.stream()
                .filter(l -> l.getTeacherId() != null)
                .collect(Collectors.groupingBy(Lesson::getTeacherId, Collectors.counting()));
        Map<Long, List<Attendance>> attendanceByTeacher = new HashMap<>();
        Map<Long, Lesson> lessonById = lessons.stream()
                .collect(Collectors.toMap(Lesson::getId, l -> l, (a, b) -> a));
        for (Attendance a : attendance) {
            Lesson lesson = lessonById.get(a.getLessonId());
            if (lesson != null && lesson.getTeacherId() != null) {
                attendanceByTeacher.computeIfAbsent(lesson.getTeacherId(), k -> new ArrayList<>()).add(a);
            }
        }
        List<Map<String, Object>> teacherStats = teachers.stream().map(t -> {
            List<Attendance> list = attendanceByTeacher.getOrDefault(t.getId(), List.of());
            long p = list.stream().filter(a -> "PRESENT".equals(a.getStatus())).count();
            long approved = leaves.stream().filter(l -> {
                Lesson lesson = lessonById.get(l.getLessonId());
                return lesson != null && t.getId().equals(lesson.getTeacherId())
                        && "APPROVED".equals(l.getStatus());
            }).count();
            long totalLeaves = leaves.stream().filter(l -> {
                Lesson lesson = lessonById.get(l.getLessonId());
                return lesson != null && t.getId().equals(lesson.getTeacherId());
            }).count();
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("teacherId", t.getId());
            row.put("teacherName", t.getName());
            row.put("classCount", classes.stream().filter(c -> t.getId().equals(c.getTeacherId())).count());
            row.put("lessonCount", lessonTeacher.getOrDefault(t.getId(), 0L));
            row.put("attendanceRate", pct(p, list.size()));
            row.put("leaveApproved", approved);
            row.put("leaveTotal", totalLeaves);
            return row;
        }).toList();

        // 班级统计
        List<Map<String, Object>> classStats = new ArrayList<>();
        for (ClassGroup g : classes) {
            List<ClassStudent> links = classStudentMapper.selectList(new LambdaQueryWrapper<ClassStudent>()
                    .eq(ClassStudent::getClassId, g.getId()).eq(ClassStudent::getStatus, "ACTIVE"));
            Set<Long> studentIds = links.stream().map(ClassStudent::getStudentId).collect(Collectors.toSet());
            List<Attendance> classAttendance = attendance.stream()
                    .filter(a -> {
                        Lesson lesson = lessonById.get(a.getLessonId());
                        return lesson != null && g.getId().equals(lesson.getClassId());
                    }).toList();
            BigDecimal classConsumed = classAttendance.stream().map(Attendance::getConsumedHours)
                    .filter(x -> x != null).reduce(BigDecimal.ZERO, BigDecimal::add);
            BigDecimal remaining = accounts.stream().filter(a -> studentIds.contains(a.getStudentId()))
                    .map(HourAccount::getRemainingHours).filter(x -> x != null)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            long p = classAttendance.stream().filter(a -> "PRESENT".equals(a.getStatus())).count();
            long l = classAttendance.stream().filter(a -> "LEAVE".equals(a.getStatus())).count();
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("classId", g.getId());
            row.put("className", g.getName());
            row.put("studentCount", links.size());
            row.put("consumedHours", classConsumed);
            row.put("remainingHours", remaining);
            row.put("attendanceRate", pct(p, classAttendance.size()));
            row.put("leaveRate", pct(l, classAttendance.size()));
            classStats.add(row);
        }

        // 续费预警
        List<Map<String, Object>> renewals = new ArrayList<>();
        for (HourAccount account : accounts) {
            if (account.getRemainingHours() == null
                    || account.getRemainingHours().compareTo(BigDecimal.valueOf(threshold)) > 0) continue;
            Student student = studentMapper.selectById(account.getStudentId());
            if (student == null) continue;
            List<ClassStudent> links = classStudentMapper.selectList(new LambdaQueryWrapper<ClassStudent>()
                    .eq(ClassStudent::getStudentId, student.getId()).eq(ClassStudent::getStatus, "ACTIVE"));
            String classNames = links.stream().map(l -> classGroupMapper.selectById(l.getClassId()))
                    .filter(x -> x != null).map(ClassGroup::getName).collect(Collectors.joining(" / "));
            List<PaymentRecord> studentPayments = paymentMapper.selectList(new LambdaQueryWrapper<PaymentRecord>()
                    .eq(PaymentRecord::getStudentId, student.getId())
                    .orderByDesc(PaymentRecord::getPaidAt).last("limit 1"));
            PaymentRecord last = studentPayments.isEmpty() ? null : studentPayments.get(0);
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("studentId", student.getId());
            row.put("studentName", student.getName());
            row.put("classNames", classNames);
            row.put("remainingHours", account.getRemainingHours());
            row.put("validUntil", account.getValidUntil());
            row.put("lastPaymentAt", last == null ? null : last.getPaidAt());
            row.put("lastPaymentItem", last == null ? "" : last.getItem());
            renewals.add(row);
        }
        renewals.sort(Comparator.comparing(r -> ((BigDecimal) r.get("remainingHours"))));

        Map<String, Object> data = new LinkedHashMap<>();
        Map<String, Object> period = new LinkedHashMap<>();
        period.put("months", span);
        period.put("startDate", start);
        period.put("endDate", end);
        data.put("period", period);
        data.put("stats", stats);
        data.put("chart", chart);
        data.put("revenueByMethod", revenueByMethod);
        data.put("teacherStats", teacherStats);
        data.put("classStats", classStats);
        data.put("renewals", renewals);
        return data;
    }

    @Transactional
    public Map<String, Object> renew(RenewalRequest request, LoginUser operator) {
        Student student = studentMapper.selectById(request.studentId());
        if (student == null) throw new BusinessException(404, "学员不存在");
        PaymentMethod method = paymentMethodMapper.selectById(request.paymentMethodId());
        if (method == null) throw new BusinessException(400, "请选择缴费方式");
        if (request.amount() == null || request.amount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException(400, "续费金额必须大于 0");
        }
        if (request.lessons() == null || request.lessons() <= 0) {
            throw new BusinessException(400, "续费课时必须大于 0");
        }

        PaymentRecord payment = new PaymentRecord();
        payment.setPaymentNo("JF" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"))
                + String.format("%04d", ThreadLocalRandom.current().nextInt(10000)));
        payment.setStudentId(student.getId());
        payment.setType("RENEW");
        payment.setItem(StringUtils.hasText(request.item()) ? request.item().trim()
                : "续费 " + request.lessons() + " 课时");
        payment.setAmount(request.amount());
        payment.setPaymentMethodId(method.getId());
        payment.setPaidAt(LocalDateTime.now());
        payment.setRegisteredBy(operator == null ? null : operator.getId());
        payment.setNote(request.note());
        paymentMapper.insert(payment);

        HourAccount account = hourAccountMapper.selectOne(new LambdaQueryWrapper<HourAccount>()
                .eq(HourAccount::getStudentId, student.getId()).last("limit 1"));
        if (account == null) {
            account = new HourAccount();
            account.setStudentId(student.getId());
            account.setTotalHours(BigDecimal.ZERO);
            account.setUsedHours(BigDecimal.ZERO);
            account.setRemainingHours(BigDecimal.ZERO);
            account.setValidUntil(LocalDate.now().plusMonths(12));
            hourAccountMapper.insert(account);
        }
        BigDecimal add = BigDecimal.valueOf(request.lessons());
        account.setTotalHours((account.getTotalHours() == null ? BigDecimal.ZERO : account.getTotalHours()).add(add));
        account.setRemainingHours((account.getRemainingHours() == null ? BigDecimal.ZERO : account.getRemainingHours()).add(add));
        if (account.getValidUntil() == null || account.getValidUntil().isBefore(LocalDate.now())) {
            account.setValidUntil(LocalDate.now().plusMonths(12));
        }
        hourAccountMapper.updateById(account);

        HourLedger ledger = new HourLedger();
        ledger.setStudentId(student.getId());
        ledger.setType("PURCHASE");
        ledger.setChangeHours(add);
        ledger.setBalanceAfter(account.getRemainingHours());
        ledger.setOperatorId(operator == null ? null : operator.getId());
        ledger.setReason("续费充值 " + payment.getPaymentNo());
        hourLedgerMapper.insert(ledger);

        SysAuditLog log = new SysAuditLog();
        log.setUserId(operator == null ? null : operator.getId());
        log.setOperatorName(operator == null ? "系统" : operator.getName());
        log.setActionType("续费登记");
        log.setTargetType("课时账户");
        log.setTargetId(student.getId());
        log.setContent("为 " + student.getName() + " 登记续费 " + request.lessons()
                + " 课时，金额 " + request.amount() + "，方式 " + method.getName());
        log.setResult("SUCCESS");
        auditLogMapper.insert(log);

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("paymentNo", payment.getPaymentNo());
        data.put("studentName", student.getName());
        data.put("lessons", request.lessons());
        data.put("amount", request.amount());
        data.put("remainingHours", account.getRemainingHours());
        data.put("validUntil", account.getValidUntil());
        return data;
    }

    private BigDecimal pct(long part, long total) {
        if (total <= 0) return BigDecimal.ZERO;
        return BigDecimal.valueOf(part).multiply(BigDecimal.valueOf(100))
                .divide(BigDecimal.valueOf(total), 1, RoundingMode.HALF_UP);
    }

    private int ruleInt(String key, int fallback) {
        BusinessRule rule = ruleMapper.selectOne(new LambdaQueryWrapper<BusinessRule>()
                .eq(BusinessRule::getRuleKey, key).last("limit 1"));
        if (rule == null || !StringUtils.hasText(rule.getRuleValue())) return fallback;
        try {
            return Integer.parseInt(rule.getRuleValue().trim());
        } catch (NumberFormatException e) {
            return fallback;
        }
    }
}
