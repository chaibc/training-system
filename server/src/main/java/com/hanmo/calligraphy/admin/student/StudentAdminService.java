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
            if (ids.isEmpty()) return PageResult.of(List.of(), 0, current, size);
            qw.in(Student::getId, ids);
        }
        qw.orderByDesc(Student::getId);
        studentMapper.selectPage(page, qw);
        List<StudentRow> rows = page.getRecords().stream().map(this::toRow).toList();
        return PageResult.of(rows, page.getTotal(), page.getCurrent(), page.getSize());
    }

    public StudentDetail detail(Long id) {
        Student student = requireStudent(id);
        HourAccount account = hourAccountMapper.selectOne(new LambdaQueryWrapper<HourAccount>()
                .eq(HourAccount::getStudentId, id).last("limit 1"));
        List<ParentView> parents = parentViews(id);
        List<ClassView> classes = classViews(id);
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
        BigDecimal delta;
        switch (request.type()) {
            case "ADD" -> {
                delta = hours.abs();
                account.setTotalHours(account.getTotalHours().add(delta));
            }
            case "DEDUCT" -> delta = hours.abs().negate();
            case "CORRECT" -> delta = hours;
            default -> throw new BusinessException(400, "未知调整类型");
        }
        BigDecimal balance = account.getRemainingHours().add(delta);
        if (balance.compareTo(BigDecimal.ZERO) < 0) throw new BusinessException(400, "调整后剩余课时不能为负数");
        account.setRemainingHours(balance);
        hourAccountMapper.updateById(account);
        HourLedger ledger = new HourLedger();
        ledger.setStudentId(id);
        ledger.setType("ADD".equals(request.type()) ? "PURCHASE" : "CORRECT");
        ledger.setChangeHours(delta);
        ledger.setBalanceAfter(balance);
        ledger.setOperatorId(operator == null ? null : operator.getId());
        ledger.setReason(request.reason());
        hourLedgerMapper.insert(ledger);
        audit(operator, "课时调整", id, student.getName() + " 课时调整 " + delta + "，原因：" + request.reason());
        return balance;
    }

    public List<StudentOption> options() {
        return studentMapper.selectList(new LambdaQueryWrapper<Student>()
                        .eq(Student::getStatus, "ACTIVE").orderByAsc(Student::getName))
                .stream().map(s -> new StudentOption(s.getId(), s.getName(), s.getStudentNo())).toList();
    }

    private StudentRow toRow(Student s) {
        HourAccount account = hourAccountMapper.selectOne(new LambdaQueryWrapper<HourAccount>()
                .eq(HourAccount::getStudentId, s.getId()).last("limit 1"));
        List<ClassView> classes = classViews(s.getId());
        List<ParentView> parents = parentViews(s.getId());
        ParentView primary = parents.stream().filter(ParentView::primary).findFirst()
                .orElse(parents.isEmpty() ? null : parents.get(0));
        Attendance last = attendanceMapper.selectOne(new LambdaQueryWrapper<Attendance>()
                .eq(Attendance::getStudentId, s.getId()).orderByDesc(Attendance::getRecordedAt).last("limit 1"));
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

    private Student requireStudent(Long id) {
        Student s = studentMapper.selectById(id);
        if (s == null) throw new BusinessException(404, "学员不存在");
        return s;
    }

    private String relationText(String relation) {
        if (relation == null) return "其他";
        return switch (relation) {
            case "MOTHER" -> "母亲";
            case "FATHER" -> "父亲";
            case "GRANDPARENT" -> "祖父母";
            default -> "其他监护人";
        };
    }

    private String maskPhone(String phone) {
        if (phone == null || phone.length() < 7) return phone;
        return phone.substring(0, 3) + "****" + phone.substring(phone.length() - 4);
    }

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
