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

/**
 * 统计报表服务：聚合核心指标（报名、消课、实收、出勤率、续费率、续费预警）、
 * 报名/试听趋势、收入构成、教师与班级维度统计，并提供「登记续费」写入口。
 *
 * <p>报表只读部分不做角色数据范围过滤，是机构整体口径；所有金额为线下登记金额，
 * 时间按服务器本地时区。</p>
 *
 * <p>依赖表：enrollment、payment_record、payment_method、student、class_group、class_student、
 * lesson、attendance、leave_request、teacher_profile、hour_account、trial_booking、business_rule、
 * hour_ledger、sys_audit_log。</p>
 */
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

    /**
     * 报表总览：一次返回核心指标、趋势、收入构成、教师统计、班级统计与续费预警名单。
     *
     * <p>统计口径（关键公式）：
     * ① 实收 revenue = 区间内 payment_record.amount 之和（按 paid_at）；
     * ② 消课 consumedHours = 区间内课次的 attendance.consumed_hours 之和；
     * ③ 出勤率 = PRESENT 条数 / 全部点名条数（PRESENT+LEAVE+ABSENT）× 100，保留 1 位小数；
     * ④ 续费率 = 区间内有 RENEW 缴费的去重学员数 / 在读（ACTIVE）学员数 × 100；
     * ⑤ 续费预警 = hour_account.remaining_hours ≤ 阈值（business_rule.renewal_threshold，缺省 3）。</p>
     *
     * <p>纯读操作，无副作用。多次全表/区间查询，数据量大时应加缓存或改聚合 SQL。</p>
     *
     * @param months 统计区间档位：≤1 取 1 个月，≤6 取 6 个月，其余取 12 个月
     * @return period / stats / chart / revenueByMethod / teacherStats / classStats / renewals
     */
    public Map<String, Object> overview(int months) {
        // 只支持 1/6/12 三档：前端传任意数字都收敛到最近档位，避免出现没有实现的区间
        int span = months <= 1 ? 1 : months <= 6 ? 6 : 12;
        LocalDate end = LocalDate.now();
        // 区间 = 从 (span-1) 个月前的 1 号 00:00 到 今天 23:59:59，含首尾
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
        // 消课口径：把区间内课次的点名明细 consumed_hours 直接求和（点名时已按出勤/缺勤/请假规则算好）
        BigDecimal consumed = attendance.stream().map(Attendance::getConsumedHours)
                .filter(x -> x != null).reduce(BigDecimal.ZERO, BigDecimal::add);
        // 出勤率分母 = 已点名条数，而不是班级人数或课次数；未点名的不进分母
        long present = attendance.stream().filter(a -> "PRESENT".equals(a.getStatus())).count();
        long leaveCount = attendance.stream().filter(a -> "LEAVE".equals(a.getStatus())).count();
        long absent = attendance.stream().filter(a -> "ABSENT".equals(a.getStatus())).count();
        long marked = present + leaveCount + absent;
        long renewPayments = payments.stream().filter(p -> "RENEW".equals(p.getType())).count();
        Set<Long> renewStudents = payments.stream().filter(p -> "RENEW".equals(p.getType()))
                .map(PaymentRecord::getStudentId).collect(Collectors.toSet());
        // 预警阈值走业务规则表（renewal_threshold），缺省 3 课时，与文档「剩余 ≤ 3 课时」一致
        int threshold = ruleInt("renewal_threshold", 3);
        List<HourAccount> accounts = hourAccountMapper.selectList(new LambdaQueryWrapper<>());
        // 预警按课时账户计：剩余 ≤ 阈值的账户都算（含 0 与负数）
        long warning = accounts.stream().filter(a -> a.getRemainingHours() != null
                && a.getRemainingHours().compareTo(BigDecimal.valueOf(threshold)) <= 0).count();

        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("enrollments", enrollments.size());
        // 新报名：区间内提交且未被取消的报名（已入班/待确认/待缴费）
        stats.put("newEnrollments", enrollments.stream().filter(e -> "ENROLLED".equals(e.getStatus())
                || "PENDING_CONFIRM".equals(e.getStatus()) || "PENDING_PAYMENT".equals(e.getStatus())).count());
        stats.put("renewPayments", renewPayments);
        stats.put("consumedHours", consumed);
        // 公式：出勤率 = PRESENT ÷ (PRESENT+LEAVE+ABSENT) × 100，保留 1 位；无点名时为 0
        stats.put("attendanceRate", pct(present, marked));
        // 公式：续费率 = 区间内有 RENEW 缴费的去重学员数 ÷ 在读学员数 × 100
        stats.put("renewalRate", pct(renewStudents.size(), activeStudents));
        stats.put("activeStudents", activeStudents);
        stats.put("warningStudents", warning);
        // 实收为缴费登记金额，未扣退款（本期无退款模型）
        stats.put("revenue", revenue);
        // 请假条数按提交时间落在区间内统计，不区分审批状态
        stats.put("leaveCount", leaves.size());
        stats.put("renewalThreshold", threshold);

        // 近 6 个月报名 / 试听走势
        // 趋势图至少展示 6 个月，选 12 个月档时跟着变成 12 个月
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
        // 收入构成：按缴费方式分组求和，按金额降序；占比 = 该方式金额 ÷ 区间实收 × 100（1 位小数）
        List<Map<String, Object>> revenueByMethod = amountByMethod.entrySet().stream()
                .sorted((a, b) -> b.getValue().compareTo(a.getValue()))
                .map(e -> {
                    Map<String, Object> row = new LinkedHashMap<>();
                    row.put("name", methodNames.getOrDefault(e.getKey(), "未登记方式"));
                    row.put("amount", e.getValue());
                    // 实收为 0 时占比直接给 0，避免除零
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
        // 教师统计：带班数看班级主表（不限状态），课次数按区间课次，出勤率按该教师课次下的点名明细
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
            // 教师出勤率分母是该教师课次的点名条数，与全局出勤率口径一致
            row.put("attendanceRate", pct(p, list.size()));
            // 请假审批数只统计区间内提交且课次属于该教师的请假
            row.put("leaveApproved", approved);
            row.put("leaveTotal", totalLeaves);
            return row;
        }).toList();

        // 班级统计
        // 班级统计：名单取 ACTIVE，课时消耗与出勤只算该班课次的点名记录
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
            // 班级出勤率/请假率的分母都是该班区间内的点名条数
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
        // 续费预警名单：逐个课时账户判断（N+1 查询学员/班级/最近缴费），按剩余课时升序，最紧急在前
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
            // 最近一次缴费按 paid_at 倒序取 1 条，供教务电话跟进时参考
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
        // 剩余课时最少的排最前
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

    /**
     * 登记线下续费：写缴费记录、充值课时、写课时流水、写审计日志。
     *
     * <p>副作用（全部在 @Transactional 内）：① payment_record 写一条 type=RENEW；
     * ② hour_account 累加 lessons（无账户则先建 0 额度账户）；③ hour_ledger 写 PURCHASE 流水；
     * ④ sys_audit_log 记「续费登记」。不改变报名状态、不动班级名单。</p>
     *
     * <p>幂等：60 秒内同一学员 + 相同金额的续费会被判定为重复提交并返回 409，
     * 用于拦住双击、网关重试与超时重发；超过该窗口的再次续费视为新的业务动作。</p>
     *
     * <p>有效期在原值过期或为空时顺延 12 个月，未过期则保持不变。</p>
     *
     * @param request  学员、金额、缴费方式、课时（均必填，金额与课时须为正）、项目、备注
     * @param operator 操作人，写入 registered_by、流水与审计
     * @return paymentNo / studentName / lessons / amount / remainingHours / validUntil
     * @throws BusinessException 404 学员不存在；400 缴费方式不存在、金额或课时非正；409 疑似重复提交
     */
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

        // ⓪ 幂等保护：60 秒内同一学员 + 相同金额的续费视为重复提交（双击、网关重试、超时重发都会命中）。
        //    续费没有业务唯一键，重复执行会重复充值课时并多记一条缴费流水，因此必须先拦截。
        PaymentRecord duplicated = paymentMapper.selectOne(new LambdaQueryWrapper<PaymentRecord>()
                .eq(PaymentRecord::getType, "RENEW")
                .eq(PaymentRecord::getStudentId, student.getId())
                .eq(PaymentRecord::getAmount, request.amount())
                .ge(PaymentRecord::getPaidAt, LocalDateTime.now().minusSeconds(60))
                .last("limit 1"));
        if (duplicated != null) {
            throw new BusinessException(409, "检测到 60 秒内相同学员、相同金额的续费记录（单号 "
                    + duplicated.getPaymentNo() + "），已阻止重复提交；如确需再次续费请稍后再试");
        }

        // ① 缴费记录：type=RENEW，item 缺省为「续费 N 课时」，payment_no = JF+日期+4 位随机
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

        // ② 课时账户：没有则先建 0 额度账户，再统一累加，保证后续 update 走同一分支
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
        // 课时按 DECIMAL(8,1) 存储，这里把整数课时转成 BigDecimal 参与累加
        BigDecimal add = BigDecimal.valueOf(request.lessons());
        account.setTotalHours((account.getTotalHours() == null ? BigDecimal.ZERO : account.getTotalHours()).add(add));
        account.setRemainingHours((account.getRemainingHours() == null ? BigDecimal.ZERO : account.getRemainingHours()).add(add));
        // 仅在有效期为空或已过期时顺延 12 个月；仍在有效期内则保留原到期日
        if (account.getValidUntil() == null || account.getValidUntil().isBefore(LocalDate.now())) {
            account.setValidUntil(LocalDate.now().plusMonths(12));
        }
        hourAccountMapper.updateById(account);

        // ③ 课时流水：正数表示充值，balance_after 为续费后余额，供对账与家长端查询
        HourLedger ledger = new HourLedger();
        ledger.setStudentId(student.getId());
        ledger.setType("PURCHASE");
        ledger.setChangeHours(add);
        ledger.setBalanceAfter(account.getRemainingHours());
        ledger.setOperatorId(operator == null ? null : operator.getId());
        ledger.setReason("续费充值 " + payment.getPaymentNo());
        hourLedgerMapper.insert(ledger);

        // ④ 审计日志：action_type 存中文「续费登记」（与 schema 注释的英文枚举不一致）
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

    // 百分比工具：part/total×100，保留 1 位小数、四舍五入；total<=0 时返回 0，避免除零
    private BigDecimal pct(long part, long total) {
        if (total <= 0) return BigDecimal.ZERO;
        return BigDecimal.valueOf(part).multiply(BigDecimal.valueOf(100))
                .divide(BigDecimal.valueOf(total), 1, RoundingMode.HALF_UP);
    }

    // 读取 business_rule 的整型配置：记录缺失、值为空或解析失败都回落到兜底值，不影响报表出数
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
