package com.hanmo.calligraphy.admin.enrollment;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.hanmo.calligraphy.admin.enrollment.EnrollmentAdminDtos.ConfirmRequest;
import com.hanmo.calligraphy.admin.enrollment.EnrollmentAdminDtos.CreateEnrollmentRequest;
import com.hanmo.calligraphy.admin.enrollment.EnrollmentAdminDtos.EnrollmentRow;
import com.hanmo.calligraphy.admin.enrollment.EnrollmentAdminDtos.FollowUpRequest;
import com.hanmo.calligraphy.admin.enrollment.EnrollmentAdminDtos.PayRequest;
import com.hanmo.calligraphy.admin.enrollment.EnrollmentAdminDtos.PaymentRow;
import com.hanmo.calligraphy.admin.enrollment.EnrollmentAdminDtos.TrialRow;
import com.hanmo.calligraphy.common.BusinessException;
import com.hanmo.calligraphy.common.PageResult;
import com.hanmo.calligraphy.domain.entity.ClassGroup;
import com.hanmo.calligraphy.domain.entity.ClassStudent;
import com.hanmo.calligraphy.domain.entity.Course;
import com.hanmo.calligraphy.domain.entity.Enrollment;
import com.hanmo.calligraphy.domain.entity.HourAccount;
import com.hanmo.calligraphy.domain.entity.HourLedger;
import com.hanmo.calligraphy.domain.entity.PaymentMethod;
import com.hanmo.calligraphy.domain.entity.PaymentRecord;
import com.hanmo.calligraphy.domain.entity.Student;
import com.hanmo.calligraphy.domain.entity.TrialBooking;
import com.hanmo.calligraphy.domain.entity.TrialSlot;
import com.hanmo.calligraphy.domain.mapper.ClassGroupMapper;
import com.hanmo.calligraphy.domain.mapper.ClassStudentMapper;
import com.hanmo.calligraphy.domain.mapper.CourseMapper;
import com.hanmo.calligraphy.domain.mapper.EnrollmentMapper;
import com.hanmo.calligraphy.domain.mapper.HourAccountMapper;
import com.hanmo.calligraphy.domain.mapper.HourLedgerMapper;
import com.hanmo.calligraphy.domain.mapper.PaymentMethodMapper;
import com.hanmo.calligraphy.domain.mapper.PaymentRecordMapper;
import com.hanmo.calligraphy.domain.mapper.StudentMapper;
import com.hanmo.calligraphy.domain.mapper.TrialBookingMapper;
import com.hanmo.calligraphy.domain.mapper.TrialSlotMapper;
import com.hanmo.calligraphy.security.LoginUser;
import com.hanmo.calligraphy.system.entity.SysAuditLog;
import com.hanmo.calligraphy.system.entity.SysUser;
import com.hanmo.calligraphy.system.mapper.SysAuditLogMapper;
import com.hanmo.calligraphy.system.mapper.SysUserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class EnrollmentAdminService {

    private final EnrollmentMapper enrollmentMapper;
    private final StudentMapper studentMapper;
    private final CourseMapper courseMapper;
    private final ClassGroupMapper classGroupMapper;
    private final SysUserMapper userMapper;
    private final PaymentRecordMapper paymentMapper;
    private final PaymentMethodMapper paymentMethodMapper;
    private final HourAccountMapper hourAccountMapper;
    private final HourLedgerMapper hourLedgerMapper;
    private final ClassStudentMapper classStudentMapper;
    private final TrialBookingMapper trialMapper;
    private final TrialSlotMapper trialSlotMapper;
    private final SysAuditLogMapper auditLogMapper;

    public PageResult<EnrollmentRow> page(long current, long size, String status, String keyword) {
        Page<Enrollment> page = new Page<>(Math.max(1, current), Math.min(Math.max(1, size), 200));
        LambdaQueryWrapper<Enrollment> qw = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(status)) qw.eq(Enrollment::getStatus, status);
        if (StringUtils.hasText(keyword)) {
            List<Long> studentIds = studentMapper.selectList(new LambdaQueryWrapper<Student>()
                            .like(Student::getName, keyword)).stream().map(Student::getId).toList();
            qw.and(w -> {
                w.like(Enrollment::getEnrollmentNo, keyword);
                if (!studentIds.isEmpty()) w.or().in(Enrollment::getStudentId, studentIds);
            });
        }
        qw.orderByDesc(Enrollment::getId);
        enrollmentMapper.selectPage(page, qw);
        List<EnrollmentRow> rows = page.getRecords().stream().map(this::toRow).toList();
        return PageResult.of(rows, page.getTotal(), page.getCurrent(), page.getSize());
    }

    @Transactional
    public EnrollmentRow create(CreateEnrollmentRequest request, LoginUser operator) {
        if (request.studentId() == null) throw new BusinessException(400, "请选择学员");
        if (request.courseId() == null) throw new BusinessException(400, "请选择课程");
        Student student = studentMapper.selectById(request.studentId());
        if (student == null) throw new BusinessException(404, "学员不存在");
        Course course = courseMapper.selectById(request.courseId());
        if (course == null || !"ON".equals(course.getStatus())) {
            throw new BusinessException(400, "课程不存在或已下架");
        }
        Long classId = request.classId();
        if (classId != null) {
            ClassGroup group = classGroupMapper.selectById(classId);
            if (group == null) throw new BusinessException(404, "班级不存在");
            if (!group.getCourseId().equals(course.getId())) throw new BusinessException(400, "班级与课程不匹配");
            long count = classStudentMapper.selectCount(new LambdaQueryWrapper<ClassStudent>()
                    .eq(ClassStudent::getClassId, classId).eq(ClassStudent::getStatus, "ACTIVE"));
            if (group.getCapacity() != null && count >= group.getCapacity()) {
                throw new BusinessException(400, "该班级名额已满");
            }
        }
        long duplicated = enrollmentMapper.selectCount(new LambdaQueryWrapper<Enrollment>()
                .eq(Enrollment::getStudentId, student.getId())
                .eq(Enrollment::getCourseId, course.getId())
                .in(Enrollment::getStatus, "PENDING_CONFIRM", "PENDING_PAYMENT", "ENROLLED"));
        if (duplicated > 0) throw new BusinessException(400, "该学员已有此课程的有效报名，请直接处理原报名");
        Enrollment enrollment = new Enrollment();
        enrollment.setEnrollmentNo("BM" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"))
                + String.format("%04d", ThreadLocalRandom.current().nextInt(10000)));
        enrollment.setStudentId(student.getId());
        enrollment.setCourseId(course.getId());
        enrollment.setClassId(classId);
        enrollment.setParentUserId(student.getPrimaryParentUserId());
        enrollment.setSource("ADMIN");
        enrollment.setStatus("PENDING_CONFIRM");
        enrollment.setAmount(request.amount());
        enrollment.setSubmittedAt(LocalDateTime.now());
        enrollment.setRemark(request.remark());
        enrollmentMapper.insert(enrollment);
        audit(operator, "报名登记", "报名", enrollment.getId(), "后台为 " + student.getName() + " 新建报名");
        return toRow(enrollment);
    }

    @Transactional
    public EnrollmentRow confirm(Long id, ConfirmRequest request, LoginUser operator) {
        Enrollment enrollment = requireEnrollment(id);
        if (!List.of("PENDING_CONFIRM", "PENDING_PAYMENT").contains(enrollment.getStatus())) {
            throw new BusinessException(400, "当前状态不能确认报名");
        }
        if (request != null && request.classId() != null) {
            ClassGroup group = classGroupMapper.selectById(request.classId());
            if (group == null) throw new BusinessException(404, "班级不存在");
            if (!group.getCourseId().equals(enrollment.getCourseId())) {
                throw new BusinessException(400, "班级与报名课程不匹配");
            }
            long count = classStudentMapper.selectCount(new LambdaQueryWrapper<ClassStudent>()
                    .eq(ClassStudent::getClassId, group.getId()).eq(ClassStudent::getStatus, "ACTIVE"));
            if (group.getCapacity() != null && count >= group.getCapacity()) {
                throw new BusinessException(400, "该班级名额已满，请调整班级");
            }
            enrollment.setClassId(request.classId());
        }
        if (request != null && request.amount() != null) enrollment.setAmount(request.amount());
        enrollment.setStatus("PENDING_PAYMENT");
        enrollment.setConfirmedBy(operator == null ? null : operator.getId());
        enrollment.setConfirmedAt(LocalDateTime.now());
        enrollmentMapper.updateById(enrollment);
        audit(operator, "报名确认", "报名", enrollment.getId(), "确认报名 " + enrollment.getEnrollmentNo());
        return toRow(enrollment);
    }

    @Transactional
    public EnrollmentRow pay(Long id, PayRequest request, LoginUser operator) {
        Enrollment enrollment = requireEnrollment(id);
        if ("ENROLLED".equals(enrollment.getStatus())) return toRow(enrollment);
        if (!"PENDING_PAYMENT".equals(enrollment.getStatus())) {
            throw new BusinessException(400, "请先确认报名再登记缴费");
        }
        if (enrollment.getClassId() != null) {
            long exists = classStudentMapper.selectCount(new LambdaQueryWrapper<ClassStudent>()
                    .eq(ClassStudent::getClassId, enrollment.getClassId())
                    .eq(ClassStudent::getStudentId, enrollment.getStudentId()).eq(ClassStudent::getStatus, "ACTIVE"));
            if (exists == 0) {
                ClassGroup group = classGroupMapper.selectById(enrollment.getClassId());
                if (group == null) throw new BusinessException(400, "报名班级不存在");
                long count = classStudentMapper.selectCount(new LambdaQueryWrapper<ClassStudent>()
                        .eq(ClassStudent::getClassId, group.getId()).eq(ClassStudent::getStatus, "ACTIVE"));
                if (group.getCapacity() != null && count >= group.getCapacity()) {
                    throw new BusinessException(400, "该班级名额已满，请先在报名确认中调整班级");
                }
            }
        }
        BigDecimal amount = request != null && request.amount() != null ? request.amount() : enrollment.getAmount();
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException(400, "缴费金额必须大于 0");
        }
        PaymentMethod method = request != null && request.paymentMethodId() != null
                ? paymentMethodMapper.selectById(request.paymentMethodId()) : null;
        if (method == null) throw new BusinessException(400, "请选择缴费方式");

        PaymentRecord payment = new PaymentRecord();
        payment.setPaymentNo("JF" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"))
                + String.format("%04d", ThreadLocalRandom.current().nextInt(10000)));
        payment.setEnrollmentId(enrollment.getId());
        payment.setStudentId(enrollment.getStudentId());
        payment.setType("NEW");
        payment.setItem(courseName(enrollment.getCourseId()) + " 报名");
        payment.setAmount(amount);
        payment.setPaymentMethodId(method.getId());
        payment.setPaidAt(LocalDateTime.now());
        payment.setRegisteredBy(operator == null ? null : operator.getId());
        payment.setNote(request.note());
        paymentMapper.insert(payment);

        enrollment.setStatus("ENROLLED");
        enrollment.setAmount(amount);
        enrollmentMapper.updateById(enrollment);

        Course course = courseMapper.selectById(enrollment.getCourseId());
        int lessons = course == null || course.getLessons() == null ? 0 : course.getLessons();
        HourAccount account = hourAccountMapper.selectOne(new LambdaQueryWrapper<HourAccount>()
                .eq(HourAccount::getStudentId, enrollment.getStudentId()).last("limit 1"));
        if (account == null) {
            account = new HourAccount();
            account.setStudentId(enrollment.getStudentId());
            account.setTotalHours(BigDecimal.valueOf(lessons));
            account.setUsedHours(BigDecimal.ZERO);
            account.setRemainingHours(BigDecimal.valueOf(lessons));
            account.setValidUntil(LocalDate.now().plusMonths(12));
            hourAccountMapper.insert(account);
        } else {
            BigDecimal total = account.getTotalHours() == null ? BigDecimal.ZERO : account.getTotalHours();
            BigDecimal remaining = account.getRemainingHours() == null ? BigDecimal.ZERO : account.getRemainingHours();
            account.setTotalHours(total.add(BigDecimal.valueOf(lessons)));
            account.setRemainingHours(remaining.add(BigDecimal.valueOf(lessons)));
            if (account.getValidUntil() == null) account.setValidUntil(LocalDate.now().plusMonths(12));
            hourAccountMapper.updateById(account);
        }
        HourLedger ledger = new HourLedger();
        ledger.setStudentId(enrollment.getStudentId());
        ledger.setType("PURCHASE");
        ledger.setChangeHours(BigDecimal.valueOf(lessons));
        ledger.setBalanceAfter(account.getRemainingHours());
        ledger.setOperatorId(operator == null ? null : operator.getId());
        ledger.setReason("报名缴费 " + payment.getPaymentNo());
        hourLedgerMapper.insert(ledger);

        if (enrollment.getClassId() != null) {
            long exists = classStudentMapper.selectCount(new LambdaQueryWrapper<ClassStudent>()
                    .eq(ClassStudent::getClassId, enrollment.getClassId())
                    .eq(ClassStudent::getStudentId, enrollment.getStudentId()));
            if (exists == 0) {
                ClassStudent link = new ClassStudent();
                link.setClassId(enrollment.getClassId());
                link.setStudentId(enrollment.getStudentId());
                link.setJoinedAt(LocalDate.now());
                link.setStatus("ACTIVE");
                classStudentMapper.insert(link);
            }
        }
        audit(operator, "缴费登记", "报名", enrollment.getId(),
                "登记线下缴费 " + amount + "，方式 " + method.getName());
        return toRow(enrollment);
    }

    @Transactional
    public EnrollmentRow cancel(Long id, LoginUser operator) {
        Enrollment enrollment = requireEnrollment(id);
        if ("ENROLLED".equals(enrollment.getStatus())) throw new BusinessException(400, "已入班报名不能取消");
        enrollment.setStatus("CANCELLED");
        enrollmentMapper.updateById(enrollment);
        audit(operator, "报名取消", "报名", enrollment.getId(), "取消报名 " + enrollment.getEnrollmentNo());
        return toRow(enrollment);
    }

    public PageResult<TrialRow> trialPage(long current, long size, String status, String keyword) {
        Page<TrialBooking> page = new Page<>(Math.max(1, current), Math.min(Math.max(1, size), 200));
        LambdaQueryWrapper<TrialBooking> qw = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(status)) qw.eq(TrialBooking::getStatus, status);
        if (StringUtils.hasText(keyword)) {
            qw.and(w -> w.like(TrialBooking::getStudentName, keyword)
                    .or().like(TrialBooking::getPhone, keyword)
                    .or().like(TrialBooking::getBookingNo, keyword));
        }
        qw.orderByDesc(TrialBooking::getId);
        trialMapper.selectPage(page, qw);
        List<TrialRow> rows = page.getRecords().stream().map(this::toTrialRow).toList();
        return PageResult.of(rows, page.getTotal(), page.getCurrent(), page.getSize());
    }

    @Transactional
    public TrialRow followUp(Long id, FollowUpRequest request) {
        TrialBooking booking = trialMapper.selectById(id);
        if (booking == null) throw new BusinessException(404, "试听预约不存在");
        if (request != null) {
            if (StringUtils.hasText(request.status())) booking.setStatus(request.status());
            if (request.note() != null) booking.setFollowUpNote(request.note());
            if (request.nextFollowUp() != null) booking.setNextFollowUp(request.nextFollowUp());
        }
        trialMapper.updateById(booking);
        return toTrialRow(booking);
    }

    public PageResult<PaymentRow> paymentPage(long current, long size, String keyword) {
        Page<PaymentRecord> page = new Page<>(Math.max(1, current), Math.min(Math.max(1, size), 200));
        LambdaQueryWrapper<PaymentRecord> qw = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(keyword)) {
            List<Long> studentIds = studentMapper.selectList(new LambdaQueryWrapper<Student>()
                            .like(Student::getName, keyword)).stream().map(Student::getId).toList();
            qw.and(w -> {
                w.like(PaymentRecord::getPaymentNo, keyword);
                if (!studentIds.isEmpty()) w.or().in(PaymentRecord::getStudentId, studentIds);
            });
        }
        qw.orderByDesc(PaymentRecord::getId);
        paymentMapper.selectPage(page, qw);
        List<PaymentRow> rows = page.getRecords().stream().map(this::toPaymentRow).toList();
        return PageResult.of(rows, page.getTotal(), page.getCurrent(), page.getSize());
    }

    private EnrollmentRow toRow(Enrollment e) {
        Student student = studentMapper.selectById(e.getStudentId());
        Course course = courseMapper.selectById(e.getCourseId());
        ClassGroup group = e.getClassId() == null ? null : classGroupMapper.selectById(e.getClassId());
        SysUser parent = e.getParentUserId() == null ? null : userMapper.selectById(e.getParentUserId());
        return new EnrollmentRow(e.getId(), e.getEnrollmentNo(), e.getStudentId(),
                student == null ? "" : student.getName(), e.getCourseId(),
                course == null ? "" : course.getName(), e.getClassId(),
                group == null ? "" : group.getName(),
                parent == null ? "" : parent.getName(),
                parent == null ? "" : mask(parent.getPhone()),
                e.getAmount(), e.getStatus(), e.getSubmittedAt(), e.getConfirmedAt());
    }

    private TrialRow toTrialRow(TrialBooking b) {
        Course course = b.getCourseId() == null ? null : courseMapper.selectById(b.getCourseId());
        TrialSlot slot = b.getSlotId() == null ? null : trialSlotMapper.selectById(b.getSlotId());
        return new TrialRow(b.getId(), b.getBookingNo(), b.getStudentName(), b.getAge(),
                b.getParentName(), mask(b.getPhone()),
                course == null ? "" : course.getName(), slot == null ? "" : slot.getName(),
                b.getStatus(), b.getFollowUpNote(), b.getNextFollowUp(), b.getCreatedAt());
    }

    private PaymentRow toPaymentRow(PaymentRecord p) {
        Student student = studentMapper.selectById(p.getStudentId());
        PaymentMethod method = p.getPaymentMethodId() == null ? null : paymentMethodMapper.selectById(p.getPaymentMethodId());
        SysUser user = p.getRegisteredBy() == null ? null : userMapper.selectById(p.getRegisteredBy());
        return new PaymentRow(p.getId(), p.getPaymentNo(), student == null ? "" : student.getName(),
                p.getItem(), p.getAmount(), method == null ? "" : method.getName(),
                user == null ? "" : user.getName(), p.getPaidAt(), "已到账");
    }

    private Enrollment requireEnrollment(Long id) {
        Enrollment e = enrollmentMapper.selectById(id);
        if (e == null) throw new BusinessException(404, "报名记录不存在");
        return e;
    }

    private String courseName(Long courseId) {
        Course c = courseMapper.selectById(courseId);
        return c == null ? "课程" : c.getName();
    }

    private String mask(String phone) {
        if (phone == null || phone.length() < 7) return phone;
        return phone.substring(0, 3) + "****" + phone.substring(phone.length() - 4);
    }

    private void audit(LoginUser operator, String action, String targetType, Long targetId, String content) {
        SysAuditLog log = new SysAuditLog();
        log.setUserId(operator == null ? null : operator.getId());
        log.setOperatorName(operator == null ? "系统" : operator.getName());
        log.setActionType(action);
        log.setTargetType(targetType);
        log.setTargetId(targetId);
        log.setContent(content);
        log.setResult("SUCCESS");
        auditLogMapper.insert(log);
    }
}
