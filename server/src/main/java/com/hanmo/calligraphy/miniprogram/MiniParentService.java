package com.hanmo.calligraphy.miniprogram;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.hanmo.calligraphy.common.BusinessException;
import com.hanmo.calligraphy.domain.entity.Attendance;
import com.hanmo.calligraphy.domain.entity.BusinessRule;
import com.hanmo.calligraphy.domain.entity.ClassGroup;
import com.hanmo.calligraphy.domain.entity.ClassStudent;
import com.hanmo.calligraphy.domain.entity.Classroom;
import com.hanmo.calligraphy.domain.entity.Course;
import com.hanmo.calligraphy.domain.entity.CoursePrice;
import com.hanmo.calligraphy.domain.entity.Enrollment;
import com.hanmo.calligraphy.domain.entity.HourAccount;
import com.hanmo.calligraphy.domain.entity.HourLedger;
import com.hanmo.calligraphy.domain.entity.LeaveReason;
import com.hanmo.calligraphy.domain.entity.LeaveRequest;
import com.hanmo.calligraphy.domain.entity.Lesson;
import com.hanmo.calligraphy.domain.entity.Notice;
import com.hanmo.calligraphy.domain.entity.ParentStudent;
import com.hanmo.calligraphy.domain.entity.Student;
import com.hanmo.calligraphy.domain.entity.TeacherProfile;
import com.hanmo.calligraphy.domain.entity.Work;
import com.hanmo.calligraphy.domain.entity.WorkComment;
import com.hanmo.calligraphy.domain.mapper.AttendanceMapper;
import com.hanmo.calligraphy.domain.mapper.BusinessRuleMapper;
import com.hanmo.calligraphy.domain.mapper.ClassGroupMapper;
import com.hanmo.calligraphy.domain.mapper.ClassStudentMapper;
import com.hanmo.calligraphy.domain.mapper.ClassroomMapper;
import com.hanmo.calligraphy.domain.mapper.CourseMapper;
import com.hanmo.calligraphy.domain.mapper.CoursePriceMapper;
import com.hanmo.calligraphy.domain.mapper.EnrollmentMapper;
import com.hanmo.calligraphy.domain.mapper.HourAccountMapper;
import com.hanmo.calligraphy.domain.mapper.HourLedgerMapper;
import com.hanmo.calligraphy.domain.mapper.LeaveReasonMapper;
import com.hanmo.calligraphy.domain.mapper.LeaveRequestMapper;
import com.hanmo.calligraphy.domain.mapper.LessonMapper;
import com.hanmo.calligraphy.domain.mapper.NoticeMapper;
import com.hanmo.calligraphy.domain.mapper.ParentStudentMapper;
import com.hanmo.calligraphy.domain.mapper.StudentMapper;
import com.hanmo.calligraphy.domain.mapper.TeacherProfileMapper;
import com.hanmo.calligraphy.domain.mapper.WorkCommentMapper;
import com.hanmo.calligraphy.domain.mapper.WorkMapper;
import com.hanmo.calligraphy.miniprogram.dto.MiniDtos.BindingRequest;
import com.hanmo.calligraphy.miniprogram.dto.MiniDtos.ChildProfileRequest;
import com.hanmo.calligraphy.miniprogram.dto.MiniDtos.EnrollmentCreateRequest;
import com.hanmo.calligraphy.miniprogram.dto.MiniDtos.LeaveCreateRequest;
import com.hanmo.calligraphy.security.LoginUser;
import com.hanmo.calligraphy.system.entity.ParentProfile;
import com.hanmo.calligraphy.system.entity.SysAuditLog;
import com.hanmo.calligraphy.system.entity.SysUser;
import com.hanmo.calligraphy.system.mapper.ParentProfileMapper;
import com.hanmo.calligraphy.system.mapper.SysAuditLogMapper;
import com.hanmo.calligraphy.system.mapper.SysUserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.WeekFields;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

@Service
@RequiredArgsConstructor
public class MiniParentService {

    private static final Map<String, String> RELATION_TEXT = Map.of(
            "MOTHER", "母亲", "FATHER", "父亲", "GRANDPARENT", "祖辈", "OTHER", "其他");
    private static final Map<String, String> ENROLL_TEXT = Map.of(
            "PENDING_CONFIRM", "待确认", "PENDING_PAYMENT", "待缴费",
            "ENROLLED", "已入班", "CANCELLED", "已取消");
    private static final Map<String, String> LEAVE_TEXT = Map.of(
            "PENDING", "待审批", "APPROVED", "已通过", "REJECTED", "已驳回", "CANCELLED", "已取消");

    private final ParentStudentMapper bindingMapper;
    private final StudentMapper studentMapper;
    private final SysUserMapper userMapper;
    private final ParentProfileMapper parentProfileMapper;
    private final ClassStudentMapper classStudentMapper;
    private final ClassGroupMapper classGroupMapper;
    private final CourseMapper courseMapper;
    private final CoursePriceMapper priceMapper;
    private final ClassroomMapper classroomMapper;
    private final TeacherProfileMapper teacherMapper;
    private final LessonMapper lessonMapper;
    private final AttendanceMapper attendanceMapper;
    private final LeaveRequestMapper leaveMapper;
    private final LeaveReasonMapper leaveReasonMapper;
    private final HourAccountMapper hourAccountMapper;
    private final HourLedgerMapper hourLedgerMapper;
    private final EnrollmentMapper enrollmentMapper;
    private final WorkMapper workMapper;
    private final WorkCommentMapper commentMapper;
    private final NoticeMapper noticeMapper;
    private final BusinessRuleMapper ruleMapper;
    private final SysAuditLogMapper auditMapper;
    private final MiniPublicService publicService;

    public Map<String, Object> overview(LoginUser user) {
        List<Map<String, Object>> children = children(user.getId());
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("parentName", user.getName());
        data.put("children", children);
        data.put("notices", noticeMapper.selectList(new LambdaQueryWrapper<Notice>()
                        .eq(Notice::getStatus, "PUBLISHED").orderByDesc(Notice::getPublishDate).last("limit 3"))
                .stream().map(publicService::noticeMap).toList());
        data.put("pendingBindings", bindingMapper.selectCount(new LambdaQueryWrapper<ParentStudent>()
                .eq(ParentStudent::getParentUserId, user.getId()).eq(ParentStudent::getStatus, "PENDING")));
        long pendingLeave = 0;
        for (Map<String, Object> child : children) {
            pendingLeave += ((Number) child.getOrDefault("pendingLeaves", 0)).longValue();
        }
        data.put("pendingLeaves", pendingLeave);
        return data;
    }

    public List<Map<String, Object>> children(Long parentId) {
        List<ParentStudent> bindings = activeBindings(parentId);
        List<Map<String, Object>> list = new ArrayList<>();
        for (ParentStudent binding : bindings) {
            Student student = studentMapper.selectById(binding.getStudentId());
            if (student == null) continue;
            Map<String, Object> item = childSummary(student, binding);
            list.add(item);
        }
        return list;
    }

    public Map<String, Object> childDetail(LoginUser user, Long studentId) {
        ParentStudent binding = requireBinding(user.getId(), studentId);
        Student student = studentMapper.selectById(studentId);
        Map<String, Object> data = childSummary(student, binding);
        data.put("relationText", RELATION_TEXT.getOrDefault(binding.getRelation(), "家长"));
        data.put("primary", Integer.valueOf(1).equals(binding.getIsPrimary()));
        data.put("school", student.getSchool());
        data.put("grade", student.getGrade());
        data.put("birthDate", student.getBirthDate());
        data.put("gender", student.getGender());
        data.put("classes", classesOfStudent(studentId));
        data.put("hourAccount", hourAccount(studentId));
        data.put("recentAttendance", attendanceRecords(studentId, null).stream().limit(3).toList());
        return data;
    }

    public Map<String, Object> updateChild(LoginUser user, Long studentId, ChildProfileRequest request) {
        requireBinding(user.getId(), studentId);
        Student student = studentMapper.selectById(studentId);
        if (student == null) throw new BusinessException(404, "学员不存在");
        if (request != null) {
            if (StringUtils.hasText(request.gender())) student.setGender(request.gender());
            if (request.birthDate() != null) student.setBirthDate(request.birthDate());
            if (request.school() != null) student.setSchool(request.school());
            if (request.grade() != null) student.setGrade(request.grade());
            studentMapper.updateById(student);
            audit(user, "资料修改", "学员", studentId, "家长修改学员基础资料");
        }
        return childDetail(user, studentId);
    }

    public List<Map<String, Object>> bindings(LoginUser user) {
        return bindingMapper.selectList(new LambdaQueryWrapper<ParentStudent>()
                        .eq(ParentStudent::getParentUserId, user.getId())
                        .orderByDesc(ParentStudent::getId))
                .stream().map(b -> {
                    Student s = studentMapper.selectById(b.getStudentId());
                    Map<String, Object> map = new LinkedHashMap<>();
                    map.put("id", b.getId());
                    map.put("studentId", b.getStudentId());
                    map.put("studentName", s == null ? "" : s.getName());
                    map.put("studentNo", s == null ? "" : s.getStudentNo());
                    map.put("relation", b.getRelation());
                    map.put("relationText", RELATION_TEXT.getOrDefault(b.getRelation(), "家长"));
                    map.put("primary", Integer.valueOf(1).equals(b.getIsPrimary()));
                    map.put("status", "UNBIND".equals(b.getSource()) && "ACTIVE".equals(b.getStatus())
                            ? "UNBIND_PENDING" : b.getStatus());
                    map.put("statusText", bindingStatusText(b));
                    map.put("source", b.getSource());
                    map.put("createdAt", b.getCreatedAt());
                    return map;
                }).toList();
    }

    @Transactional
    public Map<String, Object> createBinding(LoginUser user, BindingRequest request) {
        SysUser parent = userMapper.selectById(user.getId());
        if (parent == null) throw new BusinessException(401, "账号不存在");
        Student student;
        if (Boolean.TRUE.equals(request.newStudent())) {
            if (!StringUtils.hasText(request.studentName())) throw new BusinessException(400, "请输入学员姓名");
            student = new Student();
            student.setStudentNo(nextStudentNo());
            student.setName(request.studentName().trim());
            student.setGender(request.gender());
            student.setBirthDate(request.birthDate());
            student.setSchool(request.school());
            student.setGrade(request.grade());
            student.setStatus("ACTIVE");
            student.setEnrollDate(LocalDate.now());
            student.setPrimaryParentUserId(user.getId());
            student.setRemark("家长端新建，待教务审核绑定");
            studentMapper.insert(student);
        } else if (request.studentId() != null || StringUtils.hasText(request.studentNo())) {
            if (request.studentId() != null) {
                student = studentMapper.selectById(request.studentId());
                if (student == null) throw new BusinessException(404, "学员档案不存在");
                if (StringUtils.hasText(request.studentNo())
                        && !student.getStudentNo().equalsIgnoreCase(request.studentNo().trim())) {
                    throw new BusinessException(400, "学员编号与档案不匹配");
                }
            } else {
                student = studentMapper.selectOne(new LambdaQueryWrapper<Student>()
                        .eq(Student::getStudentNo, request.studentNo().trim()).last("limit 1"));
                if (student == null) throw new BusinessException(404, "未找到该学员编号，请核对或联系教务");
            }
            if (!StringUtils.hasText(request.phoneTail())
                    || !request.phoneTail().trim().matches("\\d{4}")) {
                throw new BusinessException(400, "请填写监护人手机号后四位");
            }
            SysUser primary = student.getPrimaryParentUserId() == null ? null
                    : userMapper.selectById(student.getPrimaryParentUserId());
            String phone = primary == null ? null : primary.getPhone();
            if (phone == null || !phone.endsWith(request.phoneTail().trim())) {
                throw new BusinessException(400, "监护人手机号后四位不正确");
            }
        } else {
            throw new BusinessException(400, "请选择学员或填写新学员信息");
        }

        ParentStudent exists = bindingMapper.selectOne(new LambdaQueryWrapper<ParentStudent>()
                .eq(ParentStudent::getParentUserId, user.getId())
                .eq(ParentStudent::getStudentId, student.getId()).last("limit 1"));
        if (exists != null && !"REJECTED".equals(exists.getStatus()) && !"REVOKED".equals(exists.getStatus())) {
            throw new BusinessException(400, "该学员已绑定或正在审核中");
        }
        long guardians = bindingMapper.selectCount(new LambdaQueryWrapper<ParentStudent>()
                .eq(ParentStudent::getStudentId, student.getId()).eq(ParentStudent::getStatus, "ACTIVE"));
        if (guardians >= 2) throw new BusinessException(400, "该学员已有 2 位有效监护人，请联系教务");
        boolean first = guardians == 0;
        if (exists != null) {
            exists.setStatus("PENDING");
            exists.setSource("SELF");
            exists.setRelation(normalizeRelation(request.relation()));
            exists.setIsPrimary(first ? 1 : 0);
            exists.setVerifiedBy(null);
            exists.setVerifiedAt(null);
            bindingMapper.updateById(exists);
        } else {
            ParentStudent binding = new ParentStudent();
            binding.setParentUserId(user.getId());
            binding.setStudentId(student.getId());
            binding.setRelation(normalizeRelation(request.relation()));
            binding.setIsPrimary(first ? 1 : 0);
            binding.setStatus("PENDING");
            binding.setSource("SELF");
            bindingMapper.insert(binding);
        }
        audit(user, "绑定申请", "家长-学员", student.getId(), "提交学员绑定申请：" + student.getName());
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("studentId", student.getId());
        map.put("studentName", student.getName());
        map.put("studentNo", student.getStudentNo());
        map.put("status", "PENDING");
        map.put("statusText", "待教务审核");
        map.put("message", "绑定申请已提交，教务审核通过后即可查看课表与课时");
        return map;
    }

    @Transactional
    public Map<String, Object> requestUnbind(LoginUser user, Long bindingId) {
        ParentStudent binding = bindingMapper.selectById(bindingId);
        if (binding == null || !user.getId().equals(binding.getParentUserId())) {
            throw new BusinessException(404, "绑定关系不存在");
        }
        if (!"ACTIVE".equals(binding.getStatus())) throw new BusinessException(400, "当前绑定状态不能申请解绑");
        if ("UNBIND".equals(binding.getSource())) throw new BusinessException(400, "已提交解绑申请，请等待教务审核");
        // 审核期间保持 ACTIVE，家长仍可查看学员；source=UNBIND 作为待审核标记
        binding.setSource("UNBIND");
        bindingMapper.updateById(binding);
        audit(user, "解绑申请", "家长-学员", binding.getId(), "家长申请解除绑定");
        return Map.of("status", "UNBIND_PENDING", "statusText", "解绑待审核");
    }

    public Map<String, Object> schedule(LoginUser user, Long studentId, LocalDate anyDay) {
        Student student = requireChild(user.getId(), studentId);
        LocalDate base = anyDay == null ? LocalDate.now() : anyDay;
        LocalDate monday = base.with(WeekFields.of(Locale.CHINA).dayOfWeek(), 1);
        LocalDate sunday = monday.plusDays(6);
        List<Long> classIds = activeClassIds(studentId);
        List<Map<String, Object>> lessons = classIds.isEmpty() ? List.of()
                : lessonMapper.selectList(new LambdaQueryWrapper<Lesson>()
                        .in(Lesson::getClassId, classIds)
                        .ge(Lesson::getLessonDate, monday).le(Lesson::getLessonDate, sunday)
                        .orderByAsc(Lesson::getLessonDate).orderByAsc(Lesson::getStartTime))
                .stream().map(l -> lessonMap(l, studentId)).toList();
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("child", childSummary(student, requireBinding(user.getId(), studentId)));
        data.put("hours", hourAccount(studentId));
        data.put("weekStart", monday);
        data.put("weekEnd", sunday);
        data.put("lessons", lessons);
        data.put("today", LocalDate.now());
        return data;
    }

    public Map<String, Object> lessonDetail(LoginUser user, Long studentId, Long lessonId) {
        Student student = requireChild(user.getId(), studentId);
        Lesson lesson = lessonMapper.selectById(lessonId);
        if (lesson == null) throw new BusinessException(404, "课次不存在");
        boolean belongs = activeClassIds(studentId).contains(lesson.getClassId());
        if (!belongs && attendanceMapper.selectCount(new LambdaQueryWrapper<Attendance>()
                .eq(Attendance::getLessonId, lessonId).eq(Attendance::getStudentId, studentId)) == 0) {
            throw new BusinessException(403, "无权查看该课次");
        }
        Map<String, Object> data = lessonMap(lesson, studentId);
        data.put("child", childSummary(student, requireBinding(user.getId(), studentId)));
        data.put("leave", leaveOfLesson(studentId, lessonId));
        LeaveRequest leave = leaveOfLesson(studentId, lessonId);
        LocalDateTime start = LocalDateTime.of(lesson.getLessonDate(), lesson.getStartTime());
        long hours = Duration.between(LocalDateTime.now(), start).toHours();
        int cutoff = ruleInt("leave_cutoff_hours", 4);
        data.put("canLeave", "SCHEDULED".equals(lesson.getStatus()) && start.isAfter(LocalDateTime.now())
                && leave == null);
        data.put("leaveHint", start.isAfter(LocalDateTime.now())
                ? (hours >= cutoff ? "距开课超过 " + cutoff + " 小时，请假通过后不扣课时"
                : "距开课不足 " + cutoff + " 小时，请假通过后仍按消课处理")
                : "课次已开始，不能请假");
        return data;
    }

    public Map<String, Object> leaveOptions(LoginUser user, Long studentId) {
        Student student = requireChild(user.getId(), studentId);
        List<Long> classIds = activeClassIds(studentId);
        List<Map<String, Object>> lessons = classIds.isEmpty() ? List.of()
                : lessonMapper.selectList(new LambdaQueryWrapper<Lesson>()
                        .in(Lesson::getClassId, classIds)
                        .eq(Lesson::getStatus, "SCHEDULED")
                        .ge(Lesson::getLessonDate, LocalDate.now())
                        .orderByAsc(Lesson::getLessonDate).orderByAsc(Lesson::getStartTime))
                .stream().filter(l -> LocalDateTime.of(l.getLessonDate(), l.getStartTime()).isAfter(LocalDateTime.now()))
                .filter(l -> leaveOfLesson(studentId, l.getId()) == null)
                .map(l -> lessonMap(l, studentId)).toList();
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("child", childSummary(student, requireBinding(user.getId(), studentId)));
        data.put("lessons", lessons);
        data.put("reasons", leaveReasonMapper.selectList(new LambdaQueryWrapper<LeaveReason>()
                .eq(LeaveReason::getStatus, "ENABLED").orderByAsc(LeaveReason::getSort)));
        data.put("cutoffHours", ruleInt("leave_cutoff_hours", 4));
        data.put("ruleText", "课前 " + ruleInt("leave_cutoff_hours", 4)
                + " 小时以上请假不扣课时；不足该时长或未到课按消课处理。同一课次只能提交一条有效申请。");
        return data;
    }

    @Transactional
    public Map<String, Object> createLeave(LoginUser user, LeaveCreateRequest request) {
        Student student = requireChild(user.getId(), request.studentId());
        Lesson lesson = lessonMapper.selectById(request.lessonId());
        if (lesson == null) throw new BusinessException(404, "课次不存在");
        if (!activeClassIds(student.getId()).contains(lesson.getClassId())) {
            throw new BusinessException(400, "该课次不属于当前学员");
        }
        if (!"SCHEDULED".equals(lesson.getStatus())) throw new BusinessException(400, "该课次不可请假");
        LocalDateTime start = LocalDateTime.of(lesson.getLessonDate(), lesson.getStartTime());
        if (!start.isAfter(LocalDateTime.now())) throw new BusinessException(400, "课次已开始，不能请假");
        LeaveRequest exists = leaveOfLesson(student.getId(), lesson.getId());
        if (exists != null) throw new BusinessException(400, "该课次已有待审批或已通过的请假申请");
        long cutoff = ruleInt("leave_cutoff_hours", 4);
        boolean deduct = Duration.between(LocalDateTime.now(), start).toHours() < cutoff;
        LeaveRequest leave = new LeaveRequest();
        leave.setLeaveNo("QJ" + LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE)
                + String.format("%04d", ThreadLocalRandom.current().nextInt(10000)));
        leave.setStudentId(student.getId());
        leave.setLessonId(lesson.getId());
        leave.setReasonId(request.reasonId());
        leave.setReasonText(StringUtils.hasText(request.reasonText()) ? request.reasonText()
                : reasonName(request.reasonId()));
        leave.setStatus("PENDING");
        leave.setDeductHours(deduct ? 1 : 0);
        leave.setSubmittedAt(LocalDateTime.now());
        leave.setMakeupStatus("NONE");
        leaveMapper.insert(leave);
        audit(user, "请假申请", "请假", leave.getId(), student.getName() + " 提交请假 " + leave.getLeaveNo());
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", leave.getId());
        map.put("leaveNo", leave.getLeaveNo());
        map.put("status", leave.getStatus());
        map.put("statusText", "待审批");
        map.put("deductHours", leave.getDeductHours());
        map.put("deductText", deduct ? "按消课处理（开课前不足 " + cutoff + " 小时）" : "不扣课时");
        map.put("lesson", lessonMap(lesson, student.getId()));
        return map;
    }

    public List<Map<String, Object>> leaveRecords(LoginUser user, Long studentId, String status) {
        Student student = requireChild(user.getId(), studentId);
        LambdaQueryWrapper<LeaveRequest> qw = new LambdaQueryWrapper<LeaveRequest>()
                .eq(LeaveRequest::getStudentId, studentId);
        if (StringUtils.hasText(status)) {
            if ("UNBIND".equals(status)) return List.of();
            qw.eq(LeaveRequest::getStatus, status);
        }
        qw.orderByDesc(LeaveRequest::getId);
        return leaveMapper.selectList(qw).stream().map(l -> leaveMap(l, student)).toList();
    }

    @Transactional
    public Map<String, Object> applyMakeup(LoginUser user, Long leaveId) {
        LeaveRequest leave = leaveMapper.selectById(leaveId);
        if (leave == null) throw new BusinessException(404, "请假记录不存在");
        Student student = requireChild(user.getId(), leave.getStudentId());
        if (!"APPROVED".equals(leave.getStatus())) throw new BusinessException(400, "请假通过后才能申请补课");
        if (ruleInt("makeup_enabled", 1) != 1) throw new BusinessException(400, "当前未开放补课申请");
        if ("PENDING".equals(leave.getMakeupStatus()) || "ARRANGED".equals(leave.getMakeupStatus())) {
            throw new BusinessException(400, "补课已申请或已安排");
        }
        leave.setMakeupStatus("PENDING");
        leaveMapper.updateById(leave);
        audit(user, "补课申请", "请假", leave.getId(), "家长申请补课 " + leave.getLeaveNo());
        return leaveMap(leave, student);
    }

    public List<Map<String, Object>> enrollments(LoginUser user, Long studentId) {
        Student student = requireChild(user.getId(), studentId);
        return enrollmentMapper.selectList(new LambdaQueryWrapper<Enrollment>()
                        .eq(Enrollment::getStudentId, student.getId()).orderByDesc(Enrollment::getId))
                .stream().map(this::enrollmentMap).toList();
    }

    public Map<String, Object> enrollmentOptions(LoginUser user, Long studentId) {
        Student student = requireChild(user.getId(), studentId);
        List<Map<String, Object>> courses = publicService.courses(null, null, null);
        List<Map<String, Object>> classes = classGroupMapper.selectList(new LambdaQueryWrapper<ClassGroup>()
                        .eq(ClassGroup::getStatus, "ACTIVE").orderByAsc(ClassGroup::getId))
                .stream().map(g -> classOption(g)).toList();
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("child", childSummary(student, requireBinding(user.getId(), studentId)));
        data.put("courses", courses);
        data.put("classes", classes);
        return data;
    }

    @Transactional
    public Map<String, Object> createEnrollment(LoginUser user, EnrollmentCreateRequest request) {
        Student student = requireChild(user.getId(), request.studentId());
        Course course = courseMapper.selectById(request.courseId());
        if (course == null || !"ON".equals(course.getStatus())) throw new BusinessException(400, "课程不存在或已下架");
        ClassGroup group = request.classId() == null ? null : classGroupMapper.selectById(request.classId());
        if (group != null) {
            if (!course.getId().equals(group.getCourseId())) throw new BusinessException(400, "班级与课程不匹配");
            if (!"ACTIVE".equals(group.getStatus())) throw new BusinessException(400, "该班级暂不接受报名");
            long count = classStudentMapper.selectCount(new LambdaQueryWrapper<ClassStudent>()
                    .eq(ClassStudent::getClassId, group.getId()).eq(ClassStudent::getStatus, "ACTIVE"));
            if (group.getCapacity() != null && count >= group.getCapacity()) {
                throw new BusinessException(400, "该班级名额已满");
            }
        }
        long duplicated = enrollmentMapper.selectCount(new LambdaQueryWrapper<Enrollment>()
                .eq(Enrollment::getStudentId, student.getId())
                .eq(Enrollment::getCourseId, course.getId())
                .in(Enrollment::getStatus, "PENDING_CONFIRM", "PENDING_PAYMENT", "ENROLLED"));
        if (duplicated > 0) throw new BusinessException(400, "该学员已有此课程的报名记录");
        Enrollment enrollment = new Enrollment();
        enrollment.setEnrollmentNo("BM" + LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE)
                + String.format("%04d", ThreadLocalRandom.current().nextInt(10000)));
        enrollment.setStudentId(student.getId());
        enrollment.setCourseId(course.getId());
        enrollment.setClassId(group == null ? null : group.getId());
        enrollment.setParentUserId(user.getId());
        enrollment.setSource("MINI");
        enrollment.setStatus("PENDING_CONFIRM");
        enrollment.setAmount(minPrice(course.getId()));
        enrollment.setSubmittedAt(LocalDateTime.now());
        enrollmentMapper.insert(enrollment);
        audit(user, "报名申请", "报名", enrollment.getId(), student.getName() + " 提交报名 " + course.getName());
        Map<String, Object> map = enrollmentMap(enrollment);
        map.put("message", "报名已提交，教务确认后到店缴费");
        return map;
    }

    @Transactional
    public Map<String, Object> cancelEnrollment(LoginUser user, Long id) {
        Enrollment enrollment = enrollmentMapper.selectById(id);
        if (enrollment == null || !user.getId().equals(enrollment.getParentUserId())) {
            throw new BusinessException(404, "报名记录不存在");
        }
        if (!List.of("PENDING_CONFIRM", "PENDING_PAYMENT").contains(enrollment.getStatus())) {
            throw new BusinessException(400, "当前报名状态不能取消，请联系教务");
        }
        enrollment.setStatus("CANCELLED");
        enrollmentMapper.updateById(enrollment);
        audit(user, "报名取消", "报名", enrollment.getId(), "家长取消报名 " + enrollment.getEnrollmentNo());
        return enrollmentMap(enrollment);
    }

    public Map<String, Object> hours(LoginUser user, Long studentId) {
        Student student = requireChild(user.getId(), studentId);
        Map<String, Object> account = hourAccount(studentId);
        List<Map<String, Object>> ledger = hourLedgerMapper.selectList(new LambdaQueryWrapper<HourLedger>()
                        .eq(HourLedger::getStudentId, studentId).orderByDesc(HourLedger::getId))
                .stream().map(l -> {
                    Map<String, Object> item = new LinkedHashMap<>();
                    item.put("id", l.getId());
                    item.put("type", l.getType());
                    item.put("typeText", switch (l.getType() == null ? "" : l.getType()) {
                        case "PURCHASE" -> "充值"; case "CONSUME" -> "消课"; default -> "修正";
                    });
                    item.put("changeHours", l.getChangeHours());
                    item.put("balanceAfter", l.getBalanceAfter());
                    item.put("reason", l.getReason());
                    item.put("createdAt", l.getCreatedAt());
                    return item;
                }).toList();
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("child", childSummary(student, requireBinding(user.getId(), studentId)));
        data.put("account", account);
        data.put("ledger", ledger);
        data.put("renewalHint", ruleInt("renewal_threshold", 3));
        return data;
    }

    public List<Map<String, Object>> attendanceRecords(Long studentId, String status) {
        LambdaQueryWrapper<Attendance> qw = new LambdaQueryWrapper<Attendance>()
                .eq(Attendance::getStudentId, studentId);
        if (StringUtils.hasText(status)) qw.eq(Attendance::getStatus, status);
        qw.orderByDesc(Attendance::getId);
        return attendanceMapper.selectList(qw).stream().map(a -> {
            Lesson lesson = lessonMapper.selectById(a.getLessonId());
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("id", a.getId());
            map.put("lessonId", a.getLessonId());
            map.put("status", a.getStatus());
            map.put("statusText", switch (a.getStatus()) {
                case "PRESENT" -> "出勤"; case "LEAVE" -> "请假"; default -> "缺勤";
            });
            map.put("consumedHours", a.getConsumedHours());
            map.put("note", a.getNote());
            map.put("lessonDate", lesson == null ? null : lesson.getLessonDate());
            map.put("startTime", lesson == null ? null : lesson.getStartTime());
            map.put("endTime", lesson == null ? null : lesson.getEndTime());
            map.put("lessonNo", lesson == null ? null : lesson.getLessonNo());
            if (lesson != null) {
                ClassGroup group = classGroupMapper.selectById(lesson.getClassId());
                map.put("className", group == null ? "" : group.getName());
                map.put("teacherName", teacherName(lesson.getTeacherId()));
                map.put("classroomName", classroomName(lesson.getClassroomId()));
            }
            return map;
        }).toList();
    }

    public Map<String, Object> attendance(LoginUser user, Long studentId, String status) {
        Student student = requireChild(user.getId(), studentId);
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("child", childSummary(student, requireBinding(user.getId(), studentId)));
        data.put("records", attendanceRecords(studentId, status));
        return data;
    }

    public Map<String, Object> growth(LoginUser user, Long studentId) {
        Student student = requireChild(user.getId(), studentId);
        List<Work> works = workMapper.selectList(new LambdaQueryWrapper<Work>()
                .eq(Work::getStudentId, studentId).eq(Work::getStatus, "DISPLAYED")
                .eq(Work::getAuthorized, 1).orderByDesc(Work::getCreatedAt));
        Map<String, List<Map<String, Object>>> grouped = new LinkedHashMap<>();
        for (Work work : works) {
            String month = work.getCreatedAt() == null ? "未知"
                    : work.getCreatedAt().format(DateTimeFormatter.ofPattern("yyyy 年 M 月"));
            grouped.computeIfAbsent(month, k -> new ArrayList<>()).add(publicService.workMap(work));
        }
        List<Map<String, Object>> timeline = grouped.entrySet().stream().map(e -> {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("month", e.getKey());
            item.put("works", e.getValue());
            Work latest = works.stream().filter(w -> w.getCreatedAt() != null
                    && w.getCreatedAt().format(DateTimeFormatter.ofPattern("yyyy 年 M 月")).equals(e.getKey()))
                    .findFirst().orElse(null);
            item.put("teacherComment", latest == null ? "" : latest.getComment());
            return item;
        }).toList();
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("child", childSummary(student, requireBinding(user.getId(), studentId)));
        data.put("timeline", timeline);
        data.put("workCount", works.size());
        return data;
    }

    @Transactional
    public Map<String, Object> comment(LoginUser user, Long workId, String content) {
        Work work = workMapper.selectById(workId);
        if (work == null || !"DISPLAYED".equals(work.getStatus())) {
            throw new BusinessException(404, "作品不存在或未展示");
        }
        if (ruleInt("comment_enabled", 1) != 1) throw new BusinessException(400, "当前未开放作品留言");
        String text = content == null ? "" : content.trim();
        if (text.isEmpty()) throw new BusinessException(400, "请输入留言内容");
        if (text.length() > 300) throw new BusinessException(400, "留言不超过 300 字");
        WorkComment comment = new WorkComment();
        comment.setWorkId(workId);
        comment.setParentUserId(user.getId());
        comment.setContent(text);
        comment.setStatus("PENDING");
        commentMapper.insert(comment);
        return Map.of("id", comment.getId(), "status", "PENDING", "message", "留言已提交，审核通过后展示");
    }

    public Map<String, Object> account(LoginUser user) {
        SysUser sysUser = userMapper.selectById(user.getId());
        if (sysUser == null) throw new BusinessException(401, "账号不存在");
        ParentProfile profile = parentProfileMapper.selectOne(new LambdaQueryWrapper<ParentProfile>()
                .eq(ParentProfile::getUserId, user.getId()).last("limit 1"));
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("name", sysUser.getName());
        data.put("phone", mask(sysUser.getPhone()));
        data.put("avatar", sysUser.getAvatar());
        data.put("wechatNickname", profile == null ? "" : profile.getWechatNickname());
        data.put("status", sysUser.getStatus());
        data.put("statusText", "ENABLED".equals(sysUser.getStatus()) ? "正常" : "已停用");
        data.put("wechatBound", StringUtils.hasText(sysUser.getWechatOpenid()));
        data.put("identity", "家长");
        data.put("bindings", bindings(user));
        data.put("children", children(user.getId()).stream()
                .map(c -> Map.of("id", c.get("id"), "name", c.get("name"))).toList());
        return data;
    }

    private Map<String, Object> childSummary(Student student, ParentStudent binding) {
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("id", student.getId());
        item.put("name", student.getName());
        item.put("studentNo", student.getStudentNo());
        item.put("gender", student.getGender());
        item.put("birthDate", student.getBirthDate());
        item.put("school", student.getSchool());
        item.put("grade", student.getGrade());
        item.put("status", student.getStatus());
        item.put("avatarChar", student.getName() == null || student.getName().isEmpty()
                ? "学" : student.getName().substring(0, 1));
        item.put("avatarColor", avatarColor(student.getId()));
        item.put("relation", binding.getRelation());
        item.put("relationText", RELATION_TEXT.getOrDefault(binding.getRelation(), "家长"));
        List<ClassGroup> classes = classesOfStudent(student.getId());
        item.put("className", classes.isEmpty() ? "未分班" : classes.stream()
                .map(ClassGroup::getName).reduce((a, b) -> a + " / " + b).orElse("未分班"));
        item.put("classNames", classes.stream().map(ClassGroup::getName).toList());
        Map<String, Object> hours = hourAccount(student.getId());
        item.put("remainingHours", hours.get("remainingHours"));
        item.put("usedHours", hours.get("usedHours"));
        item.put("totalHours", hours.get("totalHours"));
        item.put("validUntil", hours.get("validUntil"));
        item.put("pendingLeaves", leaveMapper.selectCount(new LambdaQueryWrapper<LeaveRequest>()
                .eq(LeaveRequest::getStudentId, student.getId())
                .eq(LeaveRequest::getStatus, "PENDING")));
        item.put("enrollmentCount", enrollmentMapper.selectCount(new LambdaQueryWrapper<Enrollment>()
                .eq(Enrollment::getStudentId, student.getId())));
        item.put("attendanceCount", attendanceMapper.selectCount(new LambdaQueryWrapper<Attendance>()
                .eq(Attendance::getStudentId, student.getId()).eq(Attendance::getStatus, "PRESENT")));
        return item;
    }

    private Map<String, Object> lessonMap(Lesson l, Long studentId) {
        ClassGroup group = classGroupMapper.selectById(l.getClassId());
        Course course = group == null ? null : courseMapper.selectById(group.getCourseId());
        Attendance attendance = attendanceMapper.selectOne(new LambdaQueryWrapper<Attendance>()
                .eq(Attendance::getLessonId, l.getId()).eq(Attendance::getStudentId, studentId).last("limit 1"));
        LeaveRequest leave = leaveOfLesson(studentId, l.getId());
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", l.getId());
        map.put("classId", l.getClassId());
        map.put("className", group == null ? "" : group.getName());
        map.put("courseName", course == null ? "" : course.getName());
        map.put("lessonNo", l.getLessonNo());
        map.put("lessonDate", l.getLessonDate());
        map.put("startTime", l.getStartTime());
        map.put("endTime", l.getEndTime());
        map.put("weekday", l.getLessonDate() == null ? null : l.getLessonDate().getDayOfWeek().getValue());
        map.put("teacherName", teacherName(l.getTeacherId()));
        map.put("classroomName", classroomName(l.getClassroomId()));
        map.put("status", l.getStatus());
        map.put("statusText", switch (l.getStatus()) {
            case "DONE" -> "已完成"; case "CANCELLED" -> "已取消"; default -> "待上课";
        });
        map.put("attendanceStatus", attendance == null ? null : attendance.getStatus());
        map.put("attendanceText", attendance == null ? null : switch (attendance.getStatus()) {
            case "PRESENT" -> "出勤"; case "LEAVE" -> "请假"; default -> "缺勤";
        });
        map.put("consumedHours", attendance == null ? BigDecimal.ZERO : attendance.getConsumedHours());
        map.put("leaveId", leave == null ? null : leave.getId());
        map.put("leaveStatus", leave == null ? null : leave.getStatus());
        map.put("leaveStatusText", leave == null ? null : LEAVE_TEXT.getOrDefault(leave.getStatus(), leave.getStatus()));
        map.put("note", l.getNote());
        return map;
    }

    private Map<String, Object> leaveMap(LeaveRequest leave, Student student) {
        Lesson lesson = lessonMapper.selectById(leave.getLessonId());
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", leave.getId());
        map.put("leaveNo", leave.getLeaveNo());
        map.put("studentName", student.getName());
        map.put("studentId", student.getId());
        map.put("lessonId", leave.getLessonId());
        map.put("lesson", lesson == null ? null : lessonMap(lesson, student.getId()));
        map.put("reasonText", leave.getReasonText());
        map.put("status", leave.getStatus());
        map.put("statusText", LEAVE_TEXT.getOrDefault(leave.getStatus(), leave.getStatus()));
        map.put("deductHours", leave.getDeductHours());
        map.put("deductText", Integer.valueOf(1).equals(leave.getDeductHours()) ? "扣 1 课时" : "不扣课时");
        map.put("submittedAt", leave.getSubmittedAt());
        map.put("approvedAt", leave.getApprovedAt());
        map.put("approverName", leave.getApproverId() == null ? null : userName(leave.getApproverId()));
        map.put("rejectReason", leave.getRejectReason());
        map.put("makeupStatus", leave.getMakeupStatus());
        map.put("makeupText", switch (leave.getMakeupStatus() == null ? "NONE" : leave.getMakeupStatus()) {
            case "PENDING" -> "补课待安排"; case "ARRANGED" -> "已安排补课"; default -> "未申请补课";
        });
        map.put("canMakeup", "APPROVED".equals(leave.getStatus())
                && !"PENDING".equals(leave.getMakeupStatus()) && !"ARRANGED".equals(leave.getMakeupStatus()));
        return map;
    }

    private Map<String, Object> enrollmentMap(Enrollment e) {
        Course course = courseMapper.selectById(e.getCourseId());
        ClassGroup group = e.getClassId() == null ? null : classGroupMapper.selectById(e.getClassId());
        Student student = studentMapper.selectById(e.getStudentId());
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", e.getId());
        map.put("enrollmentNo", e.getEnrollmentNo());
        map.put("studentId", e.getStudentId());
        map.put("studentName", student == null ? "" : student.getName());
        map.put("courseId", e.getCourseId());
        map.put("courseName", course == null ? "" : course.getName());
        map.put("classId", e.getClassId());
        map.put("className", group == null ? "" : group.getName());
        map.put("amount", e.getAmount());
        map.put("status", e.getStatus());
        map.put("statusText", ENROLL_TEXT.getOrDefault(e.getStatus(), e.getStatus()));
        map.put("submittedAt", e.getSubmittedAt());
        map.put("confirmedAt", e.getConfirmedAt());
        map.put("remark", e.getRemark());
        map.put("source", e.getSource());
        return map;
    }

    private Map<String, Object> classOption(ClassGroup g) {
        Course course = courseMapper.selectById(g.getCourseId());
        long count = classStudentMapper.selectCount(new LambdaQueryWrapper<ClassStudent>()
                .eq(ClassStudent::getClassId, g.getId()).eq(ClassStudent::getStatus, "ACTIVE"));
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", g.getId());
        map.put("name", g.getName());
        map.put("courseId", g.getCourseId());
        map.put("courseName", course == null ? "" : course.getName());
        map.put("teacherName", teacherName(g.getTeacherId()));
        map.put("weekday", g.getWeekday());
        map.put("startTime", g.getStartTime());
        map.put("endTime", g.getEndTime());
        map.put("capacity", g.getCapacity());
        map.put("enrolled", count);
        map.put("remaining", g.getCapacity() == null ? null : Math.max(0, g.getCapacity() - (int) count));
        map.put("status", g.getStatus());
        map.put("price", course == null ? null : minPrice(course.getId()));
        return map;
    }

    private Map<String, Object> hourAccount(Long studentId) {
        HourAccount account = hourAccountMapper.selectOne(new LambdaQueryWrapper<HourAccount>()
                .eq(HourAccount::getStudentId, studentId).last("limit 1"));
        Map<String, Object> map = new LinkedHashMap<>();
        if (account == null) {
            map.put("totalHours", BigDecimal.ZERO);
            map.put("usedHours", BigDecimal.ZERO);
            map.put("remainingHours", BigDecimal.ZERO);
            map.put("validUntil", null);
            map.put("renewalWarning", true);
        } else {
            map.put("totalHours", account.getTotalHours());
            map.put("usedHours", account.getUsedHours());
            map.put("remainingHours", account.getRemainingHours());
            map.put("validUntil", account.getValidUntil());
            map.put("renewalWarning", account.getRemainingHours() != null
                    && account.getRemainingHours().compareTo(BigDecimal.valueOf(ruleInt("renewal_threshold", 3))) <= 0);
        }
        return map;
    }

    private List<ClassGroup> classesOfStudent(Long studentId) {
        List<Long> ids = activeClassIds(studentId);
        return ids.isEmpty() ? List.of() : classGroupMapper.selectBatchIds(ids);
    }

    private List<Long> activeClassIds(Long studentId) {
        return classStudentMapper.selectList(new LambdaQueryWrapper<ClassStudent>()
                        .eq(ClassStudent::getStudentId, studentId).eq(ClassStudent::getStatus, "ACTIVE"))
                .stream().map(ClassStudent::getClassId).toList();
    }

    private Student requireChild(Long parentId, Long studentId) {
        requireBinding(parentId, studentId);
        Student student = studentMapper.selectById(studentId);
        if (student == null) throw new BusinessException(404, "学员不存在");
        return student;
    }

    private ParentStudent requireBinding(Long parentId, Long studentId) {
        ParentStudent binding = bindingMapper.selectOne(new LambdaQueryWrapper<ParentStudent>()
                .eq(ParentStudent::getParentUserId, parentId)
                .eq(ParentStudent::getStudentId, studentId)
                .eq(ParentStudent::getStatus, "ACTIVE").last("limit 1"));
        if (binding == null) throw new BusinessException(403, "未绑定该学员或绑定仍在审核中");
        return binding;
    }

    private List<ParentStudent> activeBindings(Long parentId) {
        return bindingMapper.selectList(new LambdaQueryWrapper<ParentStudent>()
                .eq(ParentStudent::getParentUserId, parentId)
                .eq(ParentStudent::getStatus, "ACTIVE").orderByDesc(ParentStudent::getIsPrimary));
    }

    private LeaveRequest leaveOfLesson(Long studentId, Long lessonId) {
        return leaveMapper.selectOne(new LambdaQueryWrapper<LeaveRequest>()
                .eq(LeaveRequest::getStudentId, studentId)
                .eq(LeaveRequest::getLessonId, lessonId)
                .in(LeaveRequest::getStatus, "PENDING", "APPROVED")
                .orderByDesc(LeaveRequest::getId).last("limit 1"));
    }

    private String teacherName(Long teacherId) {
        TeacherProfile t = teacherId == null ? null : teacherMapper.selectById(teacherId);
        return t == null ? "" : t.getName();
    }

    private String classroomName(Long classroomId) {
        Classroom c = classroomId == null ? null : classroomMapper.selectById(classroomId);
        return c == null ? "" : c.getName();
    }

    private String userName(Long userId) {
        SysUser user = userId == null ? null : userMapper.selectById(userId);
        return user == null ? "" : user.getName();
    }

    private String reasonName(Long id) {
        LeaveReason r = id == null ? null : leaveReasonMapper.selectById(id);
        return r == null ? "请假" : r.getName();
    }

    private String normalizeRelation(String relation) {
        if (!StringUtils.hasText(relation)) return "OTHER";
        return RELATION_TEXT.containsKey(relation) ? relation : "OTHER";
    }

    private String bindingStatusText(ParentStudent b) {
        if ("ACTIVE".equals(b.getStatus()) && "UNBIND".equals(b.getSource())) return "解绑待审核";
        return switch (b.getStatus()) {
            case "PENDING" -> "待审核";
            case "ACTIVE" -> "已绑定";
            case "REJECTED" -> "已驳回";
            case "REVOKED" -> "已解绑";
            default -> b.getStatus();
        };
    }

    private String nextStudentNo() {
        String prefix = "HX" + LocalDate.now().getYear();
        for (int i = 0; i < 20; i++) {
            String no = prefix + String.format("%04d", ThreadLocalRandom.current().nextInt(10000));
            if (studentMapper.selectCount(new LambdaQueryWrapper<Student>().eq(Student::getStudentNo, no)) == 0) {
                return no;
            }
        }
        throw new BusinessException(500, "学员编号生成失败，请重试");
    }

    private BigDecimal minPrice(Long courseId) {
        List<CoursePrice> prices = priceMapper.selectList(new LambdaQueryWrapper<CoursePrice>()
                .eq(CoursePrice::getCourseId, courseId).eq(CoursePrice::getStatus, "ON"));
        return prices.stream().filter(p -> p.getPrice() != null)
                .min(Comparator.comparing(CoursePrice::getPrice))
                .map(CoursePrice::getPrice).orElse(BigDecimal.ZERO);
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

    private String avatarColor(Long id) {
        String[] colors = {"#8A6A49", "#7C5B6B", "#4C6455", "#92664A", "#4A5C72"};
        return colors[(int) Math.floorMod(id == null ? 0 : id, colors.length)];
    }

    private String mask(String phone) {
        if (phone == null || phone.length() < 7) return phone;
        return phone.substring(0, 3) + "****" + phone.substring(phone.length() - 4);
    }

    private void audit(LoginUser user, String action, String targetType, Long targetId, String content) {
        SysAuditLog log = new SysAuditLog();
        log.setUserId(user == null ? null : user.getId());
        log.setOperatorName(user == null ? "家长" : user.getName());
        log.setActionType(action);
        log.setTargetType(targetType);
        log.setTargetId(targetId);
        log.setContent(content);
        log.setResult("SUCCESS");
        auditMapper.insert(log);
    }
}
