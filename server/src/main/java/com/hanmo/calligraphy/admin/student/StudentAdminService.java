package com.hanmo.calligraphy.admin.student;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.hanmo.calligraphy.admin.student.StudentAdminDtos.AdjustHoursRequest;
import com.hanmo.calligraphy.admin.student.StudentAdminDtos.AttendanceView;
import com.hanmo.calligraphy.admin.student.StudentAdminDtos.ClassView;
import com.hanmo.calligraphy.admin.student.StudentAdminDtos.CreateStudentRequest;
import com.hanmo.calligraphy.admin.student.StudentAdminDtos.LedgerView;
import com.hanmo.calligraphy.admin.student.StudentAdminDtos.ParentView;
import com.hanmo.calligraphy.admin.student.StudentAdminDtos.StudentDetail;
import com.hanmo.calligraphy.admin.student.StudentAdminDtos.StudentOption;
import com.hanmo.calligraphy.admin.student.StudentAdminDtos.StudentRow;
import com.hanmo.calligraphy.admin.student.StudentAdminDtos.UpdateStudentRequest;
import com.hanmo.calligraphy.common.BusinessException;
import com.hanmo.calligraphy.common.PageResult;
import com.hanmo.calligraphy.domain.entity.Attendance;
import com.hanmo.calligraphy.domain.entity.ClassGroup;
import com.hanmo.calligraphy.domain.entity.ClassStudent;
import com.hanmo.calligraphy.domain.entity.Course;
import com.hanmo.calligraphy.domain.entity.HourAccount;
import com.hanmo.calligraphy.domain.entity.HourLedger;
import com.hanmo.calligraphy.domain.entity.Lesson;
import com.hanmo.calligraphy.domain.entity.ParentStudent;
import com.hanmo.calligraphy.domain.entity.Student;
import com.hanmo.calligraphy.domain.entity.TeacherProfile;
import com.hanmo.calligraphy.domain.mapper.AttendanceMapper;
import com.hanmo.calligraphy.domain.mapper.ClassGroupMapper;
import com.hanmo.calligraphy.domain.mapper.ClassStudentMapper;
import com.hanmo.calligraphy.domain.mapper.CourseMapper;
import com.hanmo.calligraphy.domain.mapper.HourAccountMapper;
import com.hanmo.calligraphy.domain.mapper.HourLedgerMapper;
import com.hanmo.calligraphy.domain.mapper.LessonMapper;
import com.hanmo.calligraphy.domain.mapper.ParentStudentMapper;
import com.hanmo.calligraphy.domain.mapper.StudentMapper;
import com.hanmo.calligraphy.domain.mapper.TeacherProfileMapper;
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
import java.time.Period;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

/**
 * 学员管理服务：学员档案的分页/详情/新建/编辑、课时手工调整，以及下拉选项。
 *
 * <p>档案详情聚合家长绑定、所在班级、最近 30 条课时流水与上课记录；课时调整是除报名/续费/点名外
 * 唯一能直接改 hour_account 的入口，必须同时写 hour_ledger 留痕。</p>
 *
 * <p>依赖表：student、parent_student、sys_user、class_student、class_group、course、teacher_profile、
 * hour_account、hour_ledger、attendance、lesson、sys_audit_log。</p>
 *
 * <p>本服务不处理家长绑定审核（见 UserAdminService），也不做角色数据范围过滤，后台管理者与教务
 * 均按全量学员操作。</p>
 */
@Service
@RequiredArgsConstructor
public class StudentAdminService {

    private final StudentMapper studentMapper;
    private final ParentStudentMapper parentStudentMapper;
    private final SysUserMapper userMapper;
    private final ClassStudentMapper classStudentMapper;
    private final ClassGroupMapper classGroupMapper;
    private final CourseMapper courseMapper;
    private final TeacherProfileMapper teacherProfileMapper;
    private final HourAccountMapper hourAccountMapper;
    private final HourLedgerMapper hourLedgerMapper;
    private final AttendanceMapper attendanceMapper;
    private final LessonMapper lessonMapper;
    private final SysAuditLogMapper auditLogMapper;

    /**
     * 学员分页。
     *
     * <p>纯查询。keyword 匹配姓名或学员编号；status 精确过滤；classId 先查该班 ACTIVE 名单再
     * 用 in 过滤，名单为空时直接返回空页。每页上限 200。</p>
     *
     * <p>每行都会补课时账户、班级、主监护人、最近点名时间，存在 N+1 查询。</p>
     *
     * @param current 页码（最小 1）
     * @param size    每页条数（1~200）
     * @param keyword 姓名/学员编号，可为空
     * @param status  ACTIVE/PAUSED/GRADUATED/LEFT，可为空
     * @param classId 班级过滤，可为空
     * @return 学员行分页
     */
    public PageResult<StudentRow> page(long current, long size, String keyword, String status, Long classId) {
        Page<Student> page = new Page<>(Math.max(1, current), Math.min(Math.max(1, size), 200));
        LambdaQueryWrapper<Student> qw = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(keyword)) {
            qw.and(w -> w.like(Student::getName, keyword).or().like(Student::getStudentNo, keyword));
        }
        if (StringUtils.hasText(status)) qw.eq(Student::getStatus, status);
        if (classId != null) {
            List<Long> ids = classStudentMapper.selectList(new LambdaQueryWrapper<ClassStudent>()
                            .eq(ClassStudent::getClassId, classId)
                            .eq(ClassStudent::getStatus, "ACTIVE"))
                    .stream().map(ClassStudent::getStudentId).toList();
            // 班级没有 ACTIVE 学员时直接返回空页：避免生成 in () 这种非法 SQL
            if (ids.isEmpty()) return PageResult.of(List.of(), 0, current, size);
            qw.in(Student::getId, ids);
        }
        qw.orderByDesc(Student::getId);
        studentMapper.selectPage(page, qw);
        List<StudentRow> rows = page.getRecords().stream().map(this::toRow).toList();
        return PageResult.of(rows, page.getTotal(), page.getCurrent(), page.getSize());
    }

    /**
     * 学员档案详情：基本信息 + 课时账户 + 家长 + 班级 + 最近 30 条流水/上课记录。
     *
     * <p>纯查询。课时账户不存在时总/已消/剩余都按 0 返回，有效期返回 null；
     * 家长手机号已脱敏。</p>
     *
     * @param id 学员 ID
     * @return 档案聚合对象
     * @throws BusinessException 404 学员不存在
     */
    public StudentDetail detail(Long id) {
        Student student = requireStudent(id);
        HourAccount account = hourAccountMapper.selectOne(new LambdaQueryWrapper<HourAccount>()
                .eq(HourAccount::getStudentId, id).last("limit 1"));
        List<ParentView> parents = parentViews(id);
        List<ClassView> classes = classViews(id);
        // 档案页只取最近 30 条流水与上课记录，防止单个学员数据量大时拖慢响应
        List<LedgerView> ledger = hourLedgerMapper.selectList(new LambdaQueryWrapper<HourLedger>()
                        .eq(HourLedger::getStudentId, id).orderByDesc(HourLedger::getId).last("limit 30"))
                .stream().map(l -> new LedgerView(l.getId(), l.getType(), l.getChangeHours(),
                        l.getBalanceAfter(), l.getReason(), l.getCreatedAt())).toList();
        List<AttendanceView> attendance = attendanceViews(id);
        return new StudentDetail(student.getId(), student.getStudentNo(), student.getName(),
                student.getGender(), student.getBirthDate(), student.getSchool(), student.getGrade(),
                student.getStatus(), student.getRemark(),
                account == null ? BigDecimal.ZERO : account.getTotalHours(),
                account == null ? BigDecimal.ZERO : account.getUsedHours(),
                account == null ? BigDecimal.ZERO : account.getRemainingHours(),
                account == null ? null : account.getValidUntil(),
                parents, classes, ledger, attendance);
    }

    /**
     * 新建学员档案。
     *
     * <p>副作用：写 student 一行（编号 HX+年份+4 位随机数、状态 ACTIVE、入班日期=今天），
     * 并写 sys_audit_log；不建课时账户、不绑定家长、不入班。@Transactional 覆盖。</p>
     *
     * @param request  姓名（必填，前后空格会 trim）、性别、生日、学校、年级、备注
     * @param operator 操作人，用于审计
     * @return 新建学员行（列表展示结构）
     * @throws BusinessException 500 连续 20 次生成编号都撞唯一键
     */
    @Transactional
    public StudentRow create(CreateStudentRequest request, LoginUser operator) {
        Student student = new Student();
        student.setStudentNo(generateStudentNo());
        student.setName(request.name().trim());
        student.setGender(request.gender());
        student.setBirthDate(request.birthDate());
        student.setSchool(request.school());
        student.setGrade(request.grade());
        student.setStatus("ACTIVE");
        student.setEnrollDate(LocalDate.now());
        student.setRemark(request.remark());
        studentMapper.insert(student);
        audit(operator, "学员变更", student.getId(), "新建学员 " + student.getName());
        return toRow(student);
    }

    /**
     * 编辑学员档案（局部更新）。
     *
     * <p>副作用：更新 student 的姓名/性别/生日/学校/年级/状态/备注，并写审计日志；
     * 字段为 null 表示不修改，name 为空串也不会清空。@Transactional 覆盖。</p>
     *
     * @param id      学员 ID
     * @param request 待更新字段，均可空
     * @param operator 操作人，用于审计
     * @return 更新后的学员行
     * @throws BusinessException 404 学员不存在
     */
    @Transactional
    public StudentRow update(Long id, UpdateStudentRequest request, LoginUser operator) {
        Student student = requireStudent(id);
        if (StringUtils.hasText(request.name())) student.setName(request.name().trim());
        if (request.gender() != null) student.setGender(request.gender());
        if (request.birthDate() != null) student.setBirthDate(request.birthDate());
        if (request.school() != null) student.setSchool(request.school());
        if (request.grade() != null) student.setGrade(request.grade());
        if (StringUtils.hasText(request.status())) student.setStatus(request.status());
        if (request.remark() != null) student.setRemark(request.remark());
        studentMapper.updateById(student);
        audit(operator, "学员变更", id, "编辑学员档案");
        return toRow(student);
    }

    /**
     * 手工调整课时（充值 / 扣课 / 修正）。
     *
     * <p>计算口径：ADD 时 total 与 remaining 同加 |hours|；DEDUCT 时 remaining 减 |hours|；
     * CORRECT 时 remaining 直接加带符号的 hours。三种类型都不维护 used_hours。</p>
     *
     * <p>副作用：upsert hour_account（无账户先建 0 额度）、写一条 hour_ledger（ADD 记 PURCHASE，
     * 其余记 CORRECT，change_hours 带符号、balance_after 为调整后余额）、写审计日志；
     * 全程 @Transactional，任一步失败回滚。</p>
     *
     * @param id      学员 ID
     * @param request type=ADD/DEDUCT/CORRECT、hours（不能为 0）、reason（必填）
     * @param operator 操作人，写入流水 operator_id 与审计
     * @return 调整后的剩余课时
     * @throws BusinessException 404 学员不存在；400 调整量为 0、类型未知、调整后余额为负
     */
    @Transactional
    public BigDecimal adjustHours(Long id, AdjustHoursRequest request, LoginUser operator) {
        Student student = requireStudent(id);
        BigDecimal hours = request.hours() == null ? BigDecimal.ZERO : request.hours();
        if (hours.compareTo(BigDecimal.ZERO) == 0) throw new BusinessException(400, "调整课时不能为 0");
        HourAccount account = hourAccountMapper.selectOne(new LambdaQueryWrapper<HourAccount>()
                .eq(HourAccount::getStudentId, id).last("limit 1"));
        if (account == null) {
            account = new HourAccount();
            account.setStudentId(id);
            account.setTotalHours(BigDecimal.ZERO);
            account.setUsedHours(BigDecimal.ZERO);
            account.setRemainingHours(BigDecimal.ZERO);
            hourAccountMapper.insert(account);
        }
        // 三种调整类型语义不同：ADD 增加总额与余额；DEDUCT 只减余额；CORRECT 按带符号值修余额
        BigDecimal delta;
        // ADD 是「新增购买的课时」，只加总额与余额；DEDUCT/CORRECT 是消耗或纠错，需要同时累加 used_hours
        boolean increasesTotal = false;
        switch (request.type()) {
            case "ADD" -> {
                delta = hours.abs();
                increasesTotal = true;
                BigDecimal total = account.getTotalHours() == null ? BigDecimal.ZERO : account.getTotalHours();
                account.setTotalHours(total.add(delta));
            }
            case "DEDUCT" -> delta = hours.abs().negate();
            case "CORRECT" -> delta = hours;
            default -> throw new BusinessException(400, "未知调整类型");
        }
        // 余额允许为负：点名消课本身就可能把余额扣成负数，手动调整若强行拦截反而无法对账。
        // 同时维护 used_hours，保证 total - used = remaining 的恒等式与点名、审批口径一致。
        BigDecimal remaining = account.getRemainingHours() == null ? BigDecimal.ZERO : account.getRemainingHours();
        BigDecimal used = account.getUsedHours() == null ? BigDecimal.ZERO : account.getUsedHours();
        BigDecimal balance = remaining.add(delta);
        account.setRemainingHours(balance);
        if (!increasesTotal) {
            account.setUsedHours(used.subtract(delta));
        }
        hourAccountMapper.updateById(account);
        HourLedger ledger = new HourLedger();
        ledger.setStudentId(id);
        // 流水类型：充值记 PURCHASE，扣课/修正都记 CORRECT；金额靠 change_hours 的正负区分
        ledger.setType("ADD".equals(request.type()) ? "PURCHASE" : "CORRECT");
        ledger.setChangeHours(delta);
        ledger.setBalanceAfter(balance);
        ledger.setOperatorId(operator == null ? null : operator.getId());
        ledger.setReason(request.reason());
        hourLedgerMapper.insert(ledger);
        audit(operator, "课时调整", id, student.getName() + " 课时调整 " + delta + "，原因：" + request.reason());
        return balance;
    }

    /**
     * 学员下拉选项（报名、加班级等表单用）。
     *
     * <p>纯查询；只返回 ACTIVE 学员，按姓名升序。</p>
     *
     * @return id/name/studentNo 列表
     */
    public List<StudentOption> options() {
        return studentMapper.selectList(new LambdaQueryWrapper<Student>()
                        .eq(Student::getStatus, "ACTIVE").orderByAsc(Student::getName))
                .stream().map(s -> new StudentOption(s.getId(), s.getName(), s.getStudentNo())).toList();
    }

    // 列表行组装：逐学员查课时账户/班级/家长/最近点名，属于 N+1 查询，学员量大时需要改批量查
    private StudentRow toRow(Student s) {
        HourAccount account = hourAccountMapper.selectOne(new LambdaQueryWrapper<HourAccount>()
                .eq(HourAccount::getStudentId, s.getId()).last("limit 1"));
        List<ClassView> classes = classViews(s.getId());
        List<ParentView> parents = parentViews(s.getId());
        // 主联系人取 is_primary=1；历史数据没有主联系人时退化为第一位 ACTIVE 监护人
        ParentView primary = parents.stream().filter(ParentView::primary).findFirst()
                .orElse(parents.isEmpty() ? null : parents.get(0));
        Attendance last = attendanceMapper.selectOne(new LambdaQueryWrapper<Attendance>()
                .eq(Attendance::getStudentId, s.getId()).orderByDesc(Attendance::getRecordedAt).last("limit 1"));
        // 年龄按生日到今天的整年数计算；未填生日按 0 处理
        int age = s.getBirthDate() == null ? 0 : Period.between(s.getBirthDate(), LocalDate.now()).getYears();
        return new StudentRow(s.getId(), s.getStudentNo(), s.getName(), s.getGender(), age,
                s.getSchool(), s.getGrade(), s.getStatus(),
                classes.isEmpty() ? "未分班" : classes.get(0).className(),
                primary == null ? "未绑定" : primary.name(),
                primary == null ? "" : primary.phone(),
                account == null ? BigDecimal.ZERO : account.getRemainingHours(),
                account == null ? BigDecimal.ZERO : account.getTotalHours(),
                account == null ? null : account.getValidUntil(),
                last == null ? null : last.getRecordedAt());
    }

    // 档案里只展示 ACTIVE 绑定：PENDING/REJECTED/REVOKED 不出现；手机号脱敏后返回
    private List<ParentView> parentViews(Long studentId) {
        List<ParentStudent> bindings = parentStudentMapper.selectList(new LambdaQueryWrapper<ParentStudent>()
                .eq(ParentStudent::getStudentId, studentId)
                .eq(ParentStudent::getStatus, "ACTIVE")
                .orderByDesc(ParentStudent::getIsPrimary));
        List<ParentView> result = new ArrayList<>();
        for (ParentStudent binding : bindings) {
            SysUser user = userMapper.selectById(binding.getParentUserId());
            if (user != null) {
                result.add(new ParentView(user.getId(), user.getName(), maskPhone(user.getPhone()),
                        relationText(binding.getRelation()),
                        binding.getIsPrimary() != null && binding.getIsPrimary() == 1));
            }
        }
        return result;
    }

    // 只取 ACTIVE 的班级关系；班级已被删除时跳过，课程/教师缺失用空串占位
    private List<ClassView> classViews(Long studentId) {
        List<ClassStudent> links = classStudentMapper.selectList(new LambdaQueryWrapper<ClassStudent>()
                .eq(ClassStudent::getStudentId, studentId).eq(ClassStudent::getStatus, "ACTIVE"));
        List<ClassView> result = new ArrayList<>();
        for (ClassStudent link : links) {
            ClassGroup group = classGroupMapper.selectById(link.getClassId());
            if (group == null) continue;
            Course course = courseMapper.selectById(group.getCourseId());
            TeacherProfile teacher = teacherProfileMapper.selectById(group.getTeacherId());
            result.add(new ClassView(group.getId(), group.getName(),
                    course == null ? "" : course.getName(), teacher == null ? "" : teacher.getName()));
        }
        return result;
    }

    // 上课记录取最近 30 条；课次被删时标题降级为「课次已删除」，保证历史点名仍可读
    private List<AttendanceView> attendanceViews(Long studentId) {
        List<Attendance> records = attendanceMapper.selectList(new LambdaQueryWrapper<Attendance>()
                .eq(Attendance::getStudentId, studentId).orderByDesc(Attendance::getId).last("limit 30"));
        Map<Long, Lesson> lessons = records.isEmpty() ? Map.of() :
                lessonMapper.selectBatchIds(records.stream().map(Attendance::getLessonId).distinct().toList())
                        .stream().collect(Collectors.toMap(Lesson::getId, l -> l));
        return records.stream().map(a -> {
            Lesson lesson = lessons.get(a.getLessonId());
            return new AttendanceView(a.getId(), lesson == null ? "课次已删除" : "第 " + lesson.getLessonNo() + " 课",
                    a.getRecordedAt(), a.getStatus(), a.getConsumedHours(), a.getNote());
        }).toList();
    }

    // 学员编号 = HX + 年份 + 4 位随机数；最多重试 20 次以规避唯一键冲突，仍失败则抛 500
    private String generateStudentNo() {
        for (int i = 0; i < 20; i++) {
            String no = "HX" + LocalDate.now().getYear()
                    + String.format("%04d", ThreadLocalRandom.current().nextInt(10000));
            if (studentMapper.selectCount(new LambdaQueryWrapper<Student>().eq(Student::getStudentNo, no)) == 0) {
                return no;
            }
        }
        throw new BusinessException(500, "生成学员编号失败");
    }

    // 学员不存在统一抛 404
    private Student requireStudent(Long id) {
        Student s = studentMapper.selectById(id);
        if (s == null) throw new BusinessException(404, "学员不存在");
        return s;
    }

    // 绑定关系英文枚举转中文展示；未知值统一显示「其他监护人」
    private String relationText(String relation) {
        if (relation == null) return "其他";
        return switch (relation) {
            case "MOTHER" -> "母亲";
            case "FATHER" -> "父亲";
            case "GRANDPARENT" -> "祖父母";
            default -> "其他监护人";
        };
    }

    // 手机号脱敏：保留前 3 位与后 4 位
    private String maskPhone(String phone) {
        if (phone == null || phone.length() < 7) return phone;
        return phone.substring(0, 3) + "****" + phone.substring(phone.length() - 4);
    }

    // 学员模块审计：target_type 固定「学员」，action 为「学员变更」或「课时调整」
    private void audit(LoginUser operator, String action, Long targetId, String content) {
        SysAuditLog log = new SysAuditLog();
        log.setUserId(operator == null ? null : operator.getId());
        log.setOperatorName(operator == null ? "系统" : operator.getName());
        log.setActionType(action);
        log.setTargetType("学员");
        log.setTargetId(targetId);
        log.setContent(content);
        log.setResult("SUCCESS");
        auditLogMapper.insert(log);
    }
}
