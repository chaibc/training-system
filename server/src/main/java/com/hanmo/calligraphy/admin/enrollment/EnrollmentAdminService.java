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

/**
 * 报名与缴费管理服务：负责后台代报名、报名确认、线下缴费登记（充值课时 + 入班）、报名取消，
 * 以及试听预约跟进与缴费记录查询。
 *
 * <p>状态机：PENDING_CONFIRM（待确认）→ PENDING_PAYMENT（待缴费）→ ENROLLED（已入班）；
 * 未入班前可置 CANCELLED。缴费是唯一会写课时账户的节点，符合「线下到账后再登记」的口径。</p>
 *
 * <p>依赖表：enrollment、payment_record、hour_account、hour_ledger、class_student、class_group、
 * course、student、sys_user（家长/操作人姓名）、trial_booking、trial_slot、sys_audit_log。</p>
 */
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

    /**
     * 分页查询报名申请。
     *
     * <p>纯读操作，无副作用。keyword 同时匹配报名单号与学员姓名（先按姓名模糊查 student.id 再回查）；
     * status 为空表示不过滤状态。</p>
     *
     * @param current 页码，从 1 开始（小于 1 按 1 处理）
     * @param size    每页条数，限制在 1~200，避免后台一次拉全表
     * @param status  报名状态过滤，可为空
     * @param keyword 报名单号 / 学员姓名关键字，可为空
     * @return 报名行分页结果，含学员、课程、班级、家长姓名与脱敏手机号
     */
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

    /**
     * 后台新建报名（前台/电话/到店代录）。
     *
     * <p>校验顺序：学员存在 → 课程存在且未下架 → 班级存在、与课程匹配且名额未满 →
     * 该学员该课程无有效报名（PENDING_CONFIRM / PENDING_PAYMENT / ENROLLED）。</p>
     *
     * <p>副作用：写 enrollment 一条（source=ADMIN、状态 PENDING_CONFIRM、报名号 BM+日期+4 位随机数），
     * 并写 sys_audit_log；不扣课时、不充值、不入班。写库在 @Transactional 内。</p>
     *
     * @param request  学员、课程、可选班级、应缴金额与备注
     * @param operator 当前后台操作人，用于审计留痕
     * @return 新建的报名行
     * @throws BusinessException 400 缺学员/课程、课程已下架、班级与课程不匹配、名额已满、重复报名；
     *                           404 学员或班级不存在
     */
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
        // 重复报名拦截：同一学员同一课程只允许一条「待确认/待缴费/已入班」记录，
        // 已取消的历史报名不算，允许重新报名
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

    /**
     * 确认报名：PENDING_CONFIRM → PENDING_PAYMENT，可同时调整班级与应缴金额。
     *
     * <p>副作用：更新 enrollment 的 class_id / amount / status / confirmed_by / confirmed_at，
     * 并写 sys_audit_log；不写缴费、不充值课时。写库在 @Transactional 内。</p>
     *
     * @param id       报名 ID
     * @param request  可选的班级与金额调整，可为 null（只推进状态）
     * @param operator 操作人，写入 confirmed_by 并记审计
     * @return 更新后的报名行
     * @throws BusinessException 404 报名或班级不存在；400 当前状态不能确认、班级课程不匹配、班级已满
     */
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

    /**
     * 登记线下缴费，并一次性完成「写缴费记录 + 入班 + 充值课时 + 写流水」。
     *
     * <p>这是报名流程中唯一会充值课时的写操作，@Transactional 覆盖的副作用共 6 处：
     * ① payment_record 写一条 type=NEW 的缴费；② enrollment 置 ENROLLED；
     * ③ hour_account 按 course.lessons 累加课时（首次开户新建，有效期 12 个月）；
     * ④ hour_ledger 记一条 PURCHASE 流水（balance_after 为充值后余额）；
     * ⑤ 报名带班级时补写 class_student；⑥ sys_audit_log 留痕。任一步失败整体回滚。</p>
     *
     * <p>幂等：已 ENROLLED 直接返回，防止重复点击造成重复充值。</p>
     *
     * @param id       报名 ID
     * @param request  缴费方式（必填）、金额（为空则取报名应缴额）、备注
     * @param operator 操作人，写入 registered_by 与流水 operator_id
     * @return 入班后的报名行
     * @throws BusinessException 404 报名不存在；400 未确认即缴费、班级已满、金额非正、未选缴费方式
     */
    @Transactional
    public EnrollmentRow pay(Long id, PayRequest request, LoginUser operator) {
        Enrollment enrollment = requireEnrollment(id);
        // 幂等分支：已入班说明缴费已登记过，直接返回，避免重复充值课时
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
                // 缴费前再核一次容量：确认与缴费之间可能有人插班，这里是最后一道闸
                if (group.getCapacity() != null && count >= group.getCapacity()) {
                    throw new BusinessException(400, "该班级名额已满，请先在报名确认中调整班级");
                }
            }
        }
        // 金额缺省沿用报名应缴额；本期不支持线上支付，必须真实到账后再登记，所以金额必须为正
        BigDecimal amount = request != null && request.amount() != null ? request.amount() : enrollment.getAmount();
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException(400, "缴费金额必须大于 0");
        }
        // 缴费方式必填，作为对账依据
        PaymentMethod method = request != null && request.paymentMethodId() != null
                ? paymentMethodMapper.selectById(request.paymentMethodId()) : null;
        if (method == null) throw new BusinessException(400, "请选择缴费方式");

        // ① 写缴费记录：payment_no = JF + 日期 + 4 位随机数
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

        // ② 报名置为已入班（后续 cancel 会拒绝该状态）
        enrollment.setStatus("ENROLLED");
        enrollment.setAmount(amount);
        enrollmentMapper.updateById(enrollment);

        // ③ 充值课时：到账课时取课程配置的 lessons（课时包），不由缴费金额换算
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
        // ④ 写课时流水：正数表示充值，balance_after 记录充值后余额，供家长端与报表对账
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
                // ⑤ 入班：class_student 对 (class_id, student_id) 有唯一键，已存在则跳过，避免重复插入报错
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

    /**
     * 取消报名：未入班的报名置为 CANCELLED。
     *
     * <p>副作用：更新 enrollment.status，并写 sys_audit_log；不回收课时、不移出班级名单，
     * 写库在 @Transactional 内。</p>
     *
     * @param id       报名 ID
     * @param operator 操作人，用于审计
     * @return 取消后的报名行
     * @throws BusinessException 404 报名不存在；400 已入班不能取消（需走线下退费流程）
     */
    @Transactional
    public EnrollmentRow cancel(Long id, LoginUser operator) {
        Enrollment enrollment = requireEnrollment(id);
        if ("ENROLLED".equals(enrollment.getStatus())) throw new BusinessException(400, "已入班报名不能取消");
        enrollment.setStatus("CANCELLED");
        enrollmentMapper.updateById(enrollment);
        audit(operator, "报名取消", "报名", enrollment.getId(), "取消报名 " + enrollment.getEnrollmentNo());
        return toRow(enrollment);
    }

    /**
     * 分页查询试听预约，供教务跟进列表使用。
     *
     * <p>纯读操作。keyword 同时匹配学员姓名、手机号、预约单号；手机号返回前已脱敏
     * （中间四位打码）。</p>
     *
     * @param current 页码；@param size 每页条数（上限 200）
     * @param status  试听状态过滤 NEW/CONTACTED/VISITED/CONVERTED/CANCELLED，可为空
     * @param keyword 姓名/手机号/预约单号关键字，可为空
     * @return 试听预约分页
     */
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

    /**
     * 更新试听预约的跟进结果。
     *
     * <p>副作用：更新 trial_booking 的 status / follow_up_note / next_follow_up，
     * 无审计日志、无课时变动；服务端不校验状态流转合法性（由前端下拉约束），
     * 事务边界：@Transactional 覆盖单表 update。</p>
     *
     * @param id      试听预约 ID
     * @param request 状态、跟进备注、下次跟进日期，字段为 null 表示不修改
     * @return 更新后的试听行
     * @throws BusinessException 404 试听预约不存在
     */
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

    /**
     * 分页查询线下缴费记录。
     *
     * <p>纯读操作；keyword 匹配缴费单号与学员姓名。返回的 status 固定为「已到账」——
     * 本期无线上支付，登记即视为到账；登记人取 sys_user.name。</p>
     *
     * @param current 页码；@param size 每页条数（上限 200）
     * @param keyword 缴费单号或学员姓名，可为空
     * @return 缴费记录分页
     */
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

    // 组装报名行：补齐学员/课程/班级名与家长信息，手机号先脱敏再返回
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

    // 组装试听行：课程/时段可能已被删除，取不到时用空串占位，避免整页报错
    private TrialRow toTrialRow(TrialBooking b) {
        Course course = b.getCourseId() == null ? null : courseMapper.selectById(b.getCourseId());
        TrialSlot slot = b.getSlotId() == null ? null : trialSlotMapper.selectById(b.getSlotId());
        return new TrialRow(b.getId(), b.getBookingNo(), b.getStudentName(), b.getAge(),
                b.getParentName(), mask(b.getPhone()),
                course == null ? "" : course.getName(), slot == null ? "" : slot.getName(),
                b.getStatus(), b.getFollowUpNote(), b.getNextFollowUp(), b.getCreatedAt());
    }

    // 组装缴费行：登记人取 sys_user.name，缴费方式取 payment_method.name
    private PaymentRow toPaymentRow(PaymentRecord p) {
        Student student = studentMapper.selectById(p.getStudentId());
        PaymentMethod method = p.getPaymentMethodId() == null ? null : paymentMethodMapper.selectById(p.getPaymentMethodId());
        SysUser user = p.getRegisteredBy() == null ? null : userMapper.selectById(p.getRegisteredBy());
        return new PaymentRow(p.getId(), p.getPaymentNo(), student == null ? "" : student.getName(),
                p.getItem(), p.getAmount(), method == null ? "" : method.getName(),
                user == null ? "" : user.getName(), p.getPaidAt(), "已到账");
    }

    // 报名不存在统一抛 404，供各个写操作复用
    private Enrollment requireEnrollment(Long id) {
        Enrollment e = enrollmentMapper.selectById(id);
        if (e == null) throw new BusinessException(404, "报名记录不存在");
        return e;
    }

    // 缴费明细里的课程名；课程被逻辑删除时降级为「课程」
    private String courseName(Long courseId) {
        Course c = courseMapper.selectById(courseId);
        return c == null ? "课程" : c.getName();
    }

    // 手机号脱敏：保留前 3 位与后 4 位；长度不足 7 位无法脱敏则原样返回
    private String mask(String phone) {
        if (phone == null || phone.length() < 7) return phone;
        return phone.substring(0, 3) + "****" + phone.substring(phone.length() - 4);
    }

    // 统一写 sys_audit_log：operator 为空时记为「系统」；result 固定 SUCCESS。
    // 注意 action_type 实际存的是中文动作名（如「报名登记」），与 schema.sql 注释里的英文枚举不一致
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
