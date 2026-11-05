package com.hanmo.calligraphy.miniprogram;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.hanmo.calligraphy.common.BusinessException;
import com.hanmo.calligraphy.domain.entity.Attendance;
import com.hanmo.calligraphy.domain.entity.ClassGroup;
import com.hanmo.calligraphy.domain.entity.ClassStudent;
import com.hanmo.calligraphy.domain.entity.Classroom;
import com.hanmo.calligraphy.domain.entity.Course;
import com.hanmo.calligraphy.domain.entity.CoursePrice;
import com.hanmo.calligraphy.domain.entity.HourAccount;
import com.hanmo.calligraphy.domain.entity.HourLedger;
import com.hanmo.calligraphy.domain.entity.LeaveReason;
import com.hanmo.calligraphy.domain.entity.LeaveRequest;
import com.hanmo.calligraphy.domain.entity.Lesson;
import com.hanmo.calligraphy.domain.entity.Student;
import com.hanmo.calligraphy.domain.entity.TeacherProfile;
import com.hanmo.calligraphy.domain.entity.Work;
import com.hanmo.calligraphy.domain.entity.WorkAuth;
import com.hanmo.calligraphy.domain.mapper.AttendanceMapper;
import com.hanmo.calligraphy.domain.mapper.ClassGroupMapper;
import com.hanmo.calligraphy.domain.mapper.ClassStudentMapper;
import com.hanmo.calligraphy.domain.mapper.ClassroomMapper;
import com.hanmo.calligraphy.domain.mapper.CourseMapper;
import com.hanmo.calligraphy.domain.mapper.CoursePriceMapper;
import com.hanmo.calligraphy.domain.mapper.HourAccountMapper;
import com.hanmo.calligraphy.domain.mapper.HourLedgerMapper;
import com.hanmo.calligraphy.domain.mapper.LeaveReasonMapper;
import com.hanmo.calligraphy.domain.mapper.LeaveRequestMapper;
import com.hanmo.calligraphy.domain.mapper.LessonMapper;
import com.hanmo.calligraphy.domain.mapper.StudentMapper;
import com.hanmo.calligraphy.domain.mapper.TeacherProfileMapper;
import com.hanmo.calligraphy.domain.mapper.WorkAuthMapper;
import com.hanmo.calligraphy.domain.mapper.WorkMapper;
import com.hanmo.calligraphy.miniprogram.dto.MiniDtos.ApproveLeaveRequest;
import com.hanmo.calligraphy.miniprogram.dto.MiniDtos.AttendanceItem;
import com.hanmo.calligraphy.miniprogram.dto.MiniDtos.RollcallRequest;
import com.hanmo.calligraphy.miniprogram.dto.MiniDtos.WorkCreateRequest;
import com.hanmo.calligraphy.security.LoginUser;
import com.hanmo.calligraphy.system.entity.SysAuditLog;
import com.hanmo.calligraphy.system.mapper.SysAuditLogMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.WeekFields;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class MiniTeacherService {

    private final TeacherProfileMapper teacherMapper;
    private final ClassGroupMapper classGroupMapper;
    private final ClassStudentMapper classStudentMapper;
    private final ClassroomMapper classroomMapper;
    private final CourseMapper courseMapper;
    private final CoursePriceMapper priceMapper;
    private final LessonMapper lessonMapper;
    private final AttendanceMapper attendanceMapper;
    private final LeaveRequestMapper leaveMapper;
    private final LeaveReasonMapper leaveReasonMapper;
    private final HourAccountMapper hourAccountMapper;
    private final HourLedgerMapper hourLedgerMapper;
    private final StudentMapper studentMapper;
    private final WorkMapper workMapper;
    private final WorkAuthMapper workAuthMapper;
    private final SysAuditLogMapper auditMapper;

    public Map<String, Object> home(LoginUser user) {
        TeacherProfile teacher = requireTeacher(user);
        List<ClassGroup> classes = myClasses(teacher.getId());
        List<Long> classIds = classes.stream().map(ClassGroup::getId).toList();
        Set<Long> studentIds = new LinkedHashSet<>();
        if (!classIds.isEmpty()) {
            classStudentMapper.selectList(new LambdaQueryWrapper<ClassStudent>()
                            .in(ClassStudent::getClassId, classIds).eq(ClassStudent::getStatus, "ACTIVE"))
                    .forEach(cs -> studentIds.add(cs.getStudentId()));
        }
        LocalDate today = LocalDate.now();
        List<Map<String, Object>> todayLessons = myLessons(teacher.getId(), today, today).stream()
                .map(l -> lessonMap(l, false)).toList();
        LocalDate monday = today.with(WeekFields.of(Locale.CHINA).dayOfWeek(), 1);
        List<Map<String, Object>> weekLessons = myLessons(teacher.getId(), monday, monday.plusDays(6)).stream()
                .map(l -> lessonMap(l, false)).toList();
        List<Long> myLessonIds = lessonIdsOfTeacher(teacher.getId());
        long pendingLeaves = leaveMapper.selectCount(new LambdaQueryWrapper<LeaveRequest>()
                .in(LeaveRequest::getLessonId, myLessonIds.isEmpty() ? List.of(-1L) : myLessonIds)
                .eq(LeaveRequest::getStatus, "PENDING"));
        long pendingWorks = workMapper.selectCount(new LambdaQueryWrapper<Work>()
                .eq(Work::getTeacherId, teacher.getId()).eq(Work::getStatus, "PENDING"));
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("teacher", teacherMap(teacher));
        data.put("today", today);
        data.put("todayLessons", todayLessons);
        data.put("weekLessons", weekLessons);
        data.put("classCount", classes.size());
        data.put("studentCount", studentIds.size());
        data.put("pendingLeaves", pendingLeaves);
        data.put("pendingWorks", pendingWorks);
        data.put("weekStart", monday);
        return data;
    }

    public List<Map<String, Object>> lessons(LoginUser user, LocalDate date) {
        TeacherProfile teacher = requireTeacher(user);
        LocalDate day = date == null ? LocalDate.now() : date;
        return myLessons(teacher.getId(), day, day).stream().map(l -> lessonMap(l, true)).toList();
    }

    public List<Map<String, Object>> weekLessons(LoginUser user, LocalDate week) {
        TeacherProfile teacher = requireTeacher(user);
        LocalDate base = week == null ? LocalDate.now() : week;
        LocalDate monday = base.with(WeekFields.of(Locale.CHINA).dayOfWeek(), 1);
        return myLessons(teacher.getId(), monday, monday.plusDays(6)).stream()
                .map(l -> lessonMap(l, true)).toList();
    }

    public Map<String, Object> lessonDetail(LoginUser user, Long lessonId) {
        TeacherProfile teacher = requireTeacher(user);
        Lesson lesson = requireMyLesson(teacher, lessonId);
        ClassGroup group = classGroupMapper.selectById(lesson.getClassId());
        List<Map<String, Object>> roster = roster(lesson);
        long present = roster.stream().filter(r -> "PRESENT".equals(r.get("attendanceStatus"))).count();
        long leave = roster.stream().filter(r -> "LEAVE".equals(r.get("attendanceStatus"))).count();
        long absent = roster.stream().filter(r -> "ABSENT".equals(r.get("attendanceStatus"))).count();
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("lesson", lessonMap(lesson, false));
        data.put("classInfo", group == null ? null : classInfo(group));
        data.put("students", roster);
        data.put("summary", Map.of("total", roster.size(), "present", present, "leave", leave,
                "absent", absent, "unmarked", roster.size() - present - leave - absent,
                "done", "DONE".equals(lesson.getStatus())));
        data.put("pendingLeaves", roster.stream()
                .filter(r -> Boolean.TRUE.equals(r.get("pendingLeave"))).toList());
        return data;
    }

    @Transactional
    public Map<String, Object> rollcall(LoginUser user, Long lessonId, RollcallRequest request) {
        TeacherProfile teacher = requireTeacher(user);
        Lesson lesson = requireMyLesson(teacher, lessonId);
        if ("CANCELLED".equals(lesson.getStatus())) throw new BusinessException(400, "已取消的课次不能点名");
        if (request == null || request.items() == null || request.items().isEmpty()) {
            throw new BusinessException(400, "请至少为一位学员标记出勤状态");
        }
        Set<Long> seen = new LinkedHashSet<>();
        List<Map<String, Object>> results = new ArrayList<>();
        for (AttendanceItem item : request.items()) {
            if (item.studentId() == null || !seen.add(item.studentId())) {
                throw new BusinessException(400, "点名名单存在重复或缺失的学员");
            }
            if (!List.of("PRESENT", "LEAVE", "ABSENT").contains(item.status())) {
                throw new BusinessException(400, "出勤状态不正确");
            }
            Student student = requireLessonStudent(lesson, item.studentId());
            LeaveRequest approvedLeave = leaveMapper.selectOne(new LambdaQueryWrapper<LeaveRequest>()
                    .eq(LeaveRequest::getStudentId, student.getId())
                    .eq(LeaveRequest::getLessonId, lesson.getId())
                    .eq(LeaveRequest::getStatus, "APPROVED")
                    .orderByDesc(LeaveRequest::getId).last("limit 1"));
            BigDecimal target = targetConsumed(item.status(), approvedLeave);
            Attendance attendance = attendanceMapper.selectOne(new LambdaQueryWrapper<Attendance>()
                    .eq(Attendance::getLessonId, lesson.getId())
                    .eq(Attendance::getStudentId, student.getId()).last("limit 1"));
            BigDecimal before = attendance == null || attendance.getConsumedHours() == null
                    ? BigDecimal.ZERO : attendance.getConsumedHours();
            BigDecimal delta = target.subtract(before);
            if (attendance == null) {
                attendance = new Attendance();
                attendance.setLessonId(lesson.getId());
                attendance.setStudentId(student.getId());
            }
            attendance.setStatus(item.status());
            attendance.setConsumedHours(target);
            attendance.setNote(StringUtils.hasText(item.note()) ? item.note() : statusNote(item.status()));
            attendance.setRecordedBy(user.getId());
            attendance.setRecordedAt(LocalDateTime.now());
            if (attendance.getId() == null) {
                attendanceMapper.insert(attendance);
            } else {
                attendanceMapper.updateById(attendance);
            }
            if (delta.compareTo(BigDecimal.ZERO) != 0) {
                changeHours(student.getId(), lesson.getId(), delta.negate(), user.getId(),
                        "课次第 " + lesson.getLessonNo() + " 课点名" + (delta.signum() > 0 ? "消课" : "修正"));
            }
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("studentId", student.getId());
            row.put("studentName", student.getName());
            row.put("status", item.status());
            row.put("statusText", statusText(item.status()));
            row.put("consumedHours", target);
            results.add(row);
        }
        lesson.setStatus("DONE");
        lessonMapper.updateById(lesson);
        audit(user, "点名消课", "课次", lesson.getId(),
                "完成第 " + lesson.getLessonNo() + " 课点名，共 " + results.size() + " 人");
        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("lessonId", lesson.getId());
        summary.put("total", results.size());
        summary.put("present", results.stream().filter(r -> "PRESENT".equals(r.get("status"))).count());
        summary.put("leave", results.stream().filter(r -> "LEAVE".equals(r.get("status"))).count());
        summary.put("absent", results.stream().filter(r -> "ABSENT".equals(r.get("status"))).count());
        summary.put("done", true);
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("summary", summary);
        data.put("items", results);
        data.put("message", "点名完成，出勤与缺勤已自动消课");
        return data;
    }

    public List<Map<String, Object>> pendingLeaves(LoginUser user) {
        TeacherProfile teacher = requireTeacher(user);
        List<Long> lessonIds = lessonIdsOfTeacher(teacher.getId());
        if (lessonIds.isEmpty()) return List.of();
        return leaveMapper.selectList(new LambdaQueryWrapper<LeaveRequest>()
                        .in(LeaveRequest::getLessonId, lessonIds)
                        .eq(LeaveRequest::getStatus, "PENDING")
                        .orderByDesc(LeaveRequest::getSubmittedAt))
                .stream().map(this::leaveMap).toList();
    }

    public List<Map<String, Object>> leaveHistory(LoginUser user, String status) {
        TeacherProfile teacher = requireTeacher(user);
        List<Long> lessonIds = lessonIdsOfTeacher(teacher.getId());
        if (lessonIds.isEmpty()) return List.of();
        LambdaQueryWrapper<LeaveRequest> qw = new LambdaQueryWrapper<LeaveRequest>()
                .in(LeaveRequest::getLessonId, lessonIds);
        if (StringUtils.hasText(status)) qw.eq(LeaveRequest::getStatus, status);
        else qw.ne(LeaveRequest::getStatus, "PENDING");
        qw.orderByDesc(LeaveRequest::getId);
        return leaveMapper.selectList(qw).stream().map(this::leaveMap).toList();
    }

    @Transactional
    public Map<String, Object> approveLeave(LoginUser user, Long leaveId, ApproveLeaveRequest request) {
        TeacherProfile teacher = requireTeacher(user);
        LeaveRequest leave = leaveMapper.selectById(leaveId);
        if (leave == null) throw new BusinessException(404, "请假申请不存在");
        Lesson lesson = requireMyLesson(teacher, leave.getLessonId());
        if (!"PENDING".equals(leave.getStatus())) throw new BusinessException(400, "该请假已处理");
        boolean approve = request == null || !Boolean.FALSE.equals(request.approve());
        if (!approve && !StringUtils.hasText(request.rejectReason())) {
            throw new BusinessException(400, "请填写驳回原因");
        }
        leave.setApproverId(user.getId());
        leave.setApprovedAt(LocalDateTime.now());
        if (approve) {
            leave.setStatus("APPROVED");
            leave.setRejectReason(null);
            syncLeaveAttendance(leave, lesson, user);
        } else {
            leave.setStatus("REJECTED");
            leave.setRejectReason(request.rejectReason().trim());
        }
        leaveMapper.updateById(leave);
        audit(user, "请假审批", "请假", leave.getId(),
                (approve ? "通过" : "驳回") + "请假 " + leave.getLeaveNo());
        return leaveMap(leave);
    }

    public Map<String, Object> workOptions(LoginUser user) {
        TeacherProfile teacher = requireTeacher(user);
        List<ClassGroup> classes = myClasses(teacher.getId());
        List<Long> classIds = classes.stream().map(ClassGroup::getId).toList();
        List<Map<String, Object>> students = new ArrayList<>();
        if (!classIds.isEmpty()) {
            List<ClassStudent> links = classStudentMapper.selectList(new LambdaQueryWrapper<ClassStudent>()
                    .in(ClassStudent::getClassId, classIds).eq(ClassStudent::getStatus, "ACTIVE"));
            Set<Long> ids = new LinkedHashSet<>();
            for (ClassStudent link : links) {
                if (!ids.add(link.getStudentId())) continue;
                Student student = studentMapper.selectById(link.getStudentId());
                if (student == null) continue;
                ClassGroup group = classGroupMapper.selectById(link.getClassId());
                Map<String, Object> map = new LinkedHashMap<>();
                map.put("id", student.getId());
                map.put("name", student.getName());
                map.put("classId", link.getClassId());
                map.put("className", group == null ? "" : group.getName());
                map.put("authorized", hasWorkAuth(student.getId()));
                students.add(map);
            }
        }
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("students", students);
        data.put("workTypes", List.of(Map.of("value", "HARD", "label", "硬笔"),
                Map.of("value", "SOFT", "label", "软笔")));
        return data;
    }

    public List<Map<String, Object>> works(LoginUser user) {
        TeacherProfile teacher = requireTeacher(user);
        return workMapper.selectList(new LambdaQueryWrapper<Work>()
                        .eq(Work::getTeacherId, teacher.getId()).orderByDesc(Work::getId))
                .stream().map(w -> {
                    Map<String, Object> map = new LinkedHashMap<>();
                    Student s = studentMapper.selectById(w.getStudentId());
                    ClassGroup g = w.getClassId() == null ? null : classGroupMapper.selectById(w.getClassId());
                    map.put("id", w.getId());
                    map.put("title", w.getTitle());
                    map.put("studentName", s == null ? "" : s.getName());
                    map.put("className", g == null ? "" : g.getName());
                    map.put("workType", w.getWorkType());
                    map.put("contentChar", w.getContentChar());
                    map.put("imageUrl", w.getImageUrl());
                    map.put("comment", w.getComment());
                    map.put("status", w.getStatus());
                    map.put("statusText", switch (w.getStatus()) {
                        case "DISPLAYED" -> "已展示"; case "PENDING" -> "待审核";
                        case "REJECTED" -> "已驳回"; default -> "已隐藏";
                    });
                    map.put("createdAt", w.getCreatedAt());
                    return map;
                }).toList();
    }

    @Transactional
    public Map<String, Object> createWork(LoginUser user, WorkCreateRequest request) {
        TeacherProfile teacher = requireTeacher(user);
        Student student = studentMapper.selectById(request.studentId());
        if (student == null) throw new BusinessException(404, "学员不存在");
        List<Long> classIds = myClasses(teacher.getId()).stream().map(ClassGroup::getId).toList();
        if (classIds.isEmpty() || classStudentMapper.selectCount(new LambdaQueryWrapper<ClassStudent>()
                .in(ClassStudent::getClassId, classIds).eq(ClassStudent::getStudentId, student.getId())
                .eq(ClassStudent::getStatus, "ACTIVE")) == 0) {
            throw new BusinessException(403, "该学员不在您所带班级中");
        }
        if (!Boolean.TRUE.equals(request.authorized())) {
            throw new BusinessException(400, "请先确认已取得家长作品展示授权");
        }
        if (!hasWorkAuth(student.getId())) {
            throw new BusinessException(400, "该学员尚无有效家长授权，请先在后台登记");
        }
        Long classId = request.classId();
        if (classId != null && !classIds.contains(classId)) throw new BusinessException(403, "只能选择自己所带班级");
        if (classId == null) {
            List<ClassStudent> links = classStudentMapper.selectList(new LambdaQueryWrapper<ClassStudent>()
                    .in(ClassStudent::getClassId, classIds).eq(ClassStudent::getStudentId, student.getId())
                    .eq(ClassStudent::getStatus, "ACTIVE").last("limit 1"));
            if (!links.isEmpty()) classId = links.get(0).getClassId();
        }
        Work work = new Work();
        work.setStudentId(student.getId());
        work.setClassId(classId);
        work.setTeacherId(teacher.getId());
        work.setTitle(request.title().trim());
        work.setWorkType(StringUtils.hasText(request.workType()) ? request.workType() : "HARD");
        work.setContentChar(request.contentChar());
        work.setImageUrl(request.imageUrl());
        work.setComment(request.comment());
        work.setAuthorized(1);
        work.setStatus("PENDING");
        work.setLikeCount(0);
        workMapper.insert(work);
        audit(user, "作品上传", "作品", work.getId(), "上传 " + student.getName() + " 作品《" + work.getTitle() + "》");
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", work.getId());
        map.put("title", work.getTitle());
        map.put("status", work.getStatus());
        map.put("message", "作品已提交，待教务审核后展示");
        return map;
    }

    public Map<String, Object> account(LoginUser user) {
        TeacherProfile teacher = requireTeacher(user);
        List<ClassGroup> classes = myClasses(teacher.getId());
        List<Long> classIds = classes.stream().map(ClassGroup::getId).toList();
        long studentCount = 0;
        if (!classIds.isEmpty()) {
            studentCount = classStudentMapper.selectCount(new LambdaQueryWrapper<ClassStudent>()
                    .in(ClassStudent::getClassId, classIds).eq(ClassStudent::getStatus, "ACTIVE"));
        }
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("teacher", teacherMap(teacher));
        data.put("identity", "教师");
        data.put("classCount", classes.size());
        data.put("studentCount", studentCount);
        data.put("classes", classes.stream().map(this::classInfo).toList());
        return data;
    }

    private TeacherProfile requireTeacher(LoginUser user) {
        TeacherProfile teacher = teacherMapper.selectOne(new LambdaQueryWrapper<TeacherProfile>()
                .eq(TeacherProfile::getUserId, user.getId()).last("limit 1"));
        if (teacher == null) throw new BusinessException(403, "当前账号未关联教师档案");
        if (!"ACTIVE".equals(teacher.getStatus())) throw new BusinessException(403, "教师档案已停用");
        return teacher;
    }

    private List<ClassGroup> myClasses(Long teacherId) {
        return classGroupMapper.selectList(new LambdaQueryWrapper<ClassGroup>()
                .eq(ClassGroup::getTeacherId, teacherId).ne(ClassGroup::getStatus, "FINISHED")
                .orderByAsc(ClassGroup::getId));
    }

    private List<Lesson> myLessons(Long teacherId, LocalDate from, LocalDate to) {
        return lessonMapper.selectList(new LambdaQueryWrapper<Lesson>()
                .eq(Lesson::getTeacherId, teacherId)
                .ge(Lesson::getLessonDate, from).le(Lesson::getLessonDate, to)
                .orderByAsc(Lesson::getLessonDate).orderByAsc(Lesson::getStartTime));
    }

    private List<Long> lessonIdsOfTeacher(Long teacherId) {
        return lessonMapper.selectList(new LambdaQueryWrapper<Lesson>()
                        .eq(Lesson::getTeacherId, teacherId))
                .stream().map(Lesson::getId).toList();
    }

    private Lesson requireMyLesson(TeacherProfile teacher, Long lessonId) {
        Lesson lesson = lessonMapper.selectById(lessonId);
        if (lesson == null) throw new BusinessException(404, "课次不存在");
        if (!teacher.getId().equals(lesson.getTeacherId())) {
            throw new BusinessException(403, "只能操作自己的课次");
        }
        return lesson;
    }

    private Student requireLessonStudent(Lesson lesson, Long studentId) {
        Student student = studentMapper.selectById(studentId);
        if (student == null) throw new BusinessException(404, "学员不存在");
        long linked = classStudentMapper.selectCount(new LambdaQueryWrapper<ClassStudent>()
                .eq(ClassStudent::getClassId, lesson.getClassId())
                .eq(ClassStudent::getStudentId, studentId));
        long attendance = attendanceMapper.selectCount(new LambdaQueryWrapper<Attendance>()
                .eq(Attendance::getLessonId, lesson.getId()).eq(Attendance::getStudentId, studentId));
        if (linked == 0 && attendance == 0) {
            throw new BusinessException(400, student.getName() + " 不在该班级名单中");
        }
        return student;
    }

    private List<Map<String, Object>> roster(Lesson lesson) {
        List<ClassStudent> links = classStudentMapper.selectList(new LambdaQueryWrapper<ClassStudent>()
                .eq(ClassStudent::getClassId, lesson.getClassId())
                .eq(ClassStudent::getStatus, "ACTIVE").orderByAsc(ClassStudent::getId));
        List<Map<String, Object>> list = new ArrayList<>();
        for (ClassStudent link : links) {
            Student student = studentMapper.selectById(link.getStudentId());
            if (student == null) continue;
            Attendance attendance = attendanceMapper.selectOne(new LambdaQueryWrapper<Attendance>()
                    .eq(Attendance::getLessonId, lesson.getId())
                    .eq(Attendance::getStudentId, student.getId()).last("limit 1"));
            LeaveRequest pendingLeave = leaveMapper.selectOne(new LambdaQueryWrapper<LeaveRequest>()
                    .eq(LeaveRequest::getLessonId, lesson.getId())
                    .eq(LeaveRequest::getStudentId, student.getId())
                    .eq(LeaveRequest::getStatus, "PENDING")
                    .orderByDesc(LeaveRequest::getId).last("limit 1"));
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("studentId", student.getId());
            row.put("name", student.getName());
            row.put("studentNo", student.getStudentNo());
            row.put("avatarChar", student.getName() == null || student.getName().isEmpty()
                    ? "学" : student.getName().substring(0, 1));
            row.put("attendanceStatus", attendance == null ? null : attendance.getStatus());
            row.put("consumedHours", attendance == null ? BigDecimal.ZERO : attendance.getConsumedHours());
            row.put("note", attendance == null ? null : attendance.getNote());
            row.put("pendingLeave", pendingLeave != null);
            row.put("leaveId", pendingLeave == null ? null : pendingLeave.getId());
            row.put("leaveReason", pendingLeave == null ? null : pendingLeave.getReasonText());
            list.add(row);
        }
        return list;
    }

    private Map<String, Object> lessonMap(Lesson l, boolean withRoster) {
        ClassGroup group = classGroupMapper.selectById(l.getClassId());
        Course course = group == null ? null : courseMapper.selectById(group.getCourseId());
        Classroom room = l.getClassroomId() == null ? null : classroomMapper.selectById(l.getClassroomId());
        long roster = classStudentMapper.selectCount(new LambdaQueryWrapper<ClassStudent>()
                .eq(ClassStudent::getClassId, l.getClassId()).eq(ClassStudent::getStatus, "ACTIVE"));
        long marked = attendanceMapper.selectCount(new LambdaQueryWrapper<Attendance>()
                .eq(Attendance::getLessonId, l.getId()));
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
        map.put("classroomName", room == null ? "" : room.getName());
        map.put("status", l.getStatus());
        map.put("statusText", switch (l.getStatus()) {
            case "DONE" -> "已完成"; case "CANCELLED" -> "已取消"; default -> "待上课";
        });
        map.put("rosterCount", roster);
        map.put("markedCount", marked);
        map.put("note", l.getNote());
        if (withRoster) map.put("students", roster(l).stream()
                .map(r -> Map.of("studentId", r.get("studentId"), "name", r.get("name"),
                        "attendanceStatus", r.get("attendanceStatus") == null ? "" : r.get("attendanceStatus"),
                        "pendingLeave", r.get("pendingLeave"))).toList());
        return map;
    }

    private Map<String, Object> classInfo(ClassGroup g) {
        Course course = courseMapper.selectById(g.getCourseId());
        Classroom room = g.getClassroomId() == null ? null : classroomMapper.selectById(g.getClassroomId());
        long count = classStudentMapper.selectCount(new LambdaQueryWrapper<ClassStudent>()
                .eq(ClassStudent::getClassId, g.getId()).eq(ClassStudent::getStatus, "ACTIVE"));
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", g.getId());
        map.put("name", g.getName());
        map.put("courseName", course == null ? "" : course.getName());
        map.put("classroomName", room == null ? "" : room.getName());
        map.put("weekday", g.getWeekday());
        map.put("startTime", g.getStartTime());
        map.put("endTime", g.getEndTime());
        map.put("capacity", g.getCapacity());
        map.put("studentCount", count);
        map.put("status", g.getStatus());
        return map;
    }

    private Map<String, Object> teacherMap(TeacherProfile t) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", t.getId());
        map.put("name", t.getName());
        map.put("teacherNo", t.getTeacherNo());
        map.put("title", t.getTitle());
        map.put("years", t.getYears());
        map.put("styles", t.getStyles());
        map.put("coursesText", t.getCoursesText());
        map.put("bio", t.getBio());
        map.put("quote", t.getQuote());
        map.put("tags", t.getTags());
        map.put("avatar", t.getAvatar());
        map.put("status", t.getStatus());
        return map;
    }

    private Map<String, Object> leaveMap(LeaveRequest l) {
        Student student = studentMapper.selectById(l.getStudentId());
        Lesson lesson = lessonMapper.selectById(l.getLessonId());
        ClassGroup group = lesson == null ? null : classGroupMapper.selectById(lesson.getClassId());
        LeaveReason reason = l.getReasonId() == null ? null : leaveReasonMapper.selectById(l.getReasonId());
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", l.getId());
        map.put("leaveNo", l.getLeaveNo());
        map.put("studentId", l.getStudentId());
        map.put("studentName", student == null ? "" : student.getName());
        map.put("className", group == null ? "" : group.getName());
        map.put("lessonNo", lesson == null ? null : lesson.getLessonNo());
        map.put("lessonDate", lesson == null ? null : lesson.getLessonDate());
        map.put("startTime", lesson == null ? null : lesson.getStartTime());
        map.put("endTime", lesson == null ? null : lesson.getEndTime());
        map.put("reasonName", reason == null ? "" : reason.getName());
        map.put("reasonText", l.getReasonText());
        map.put("status", l.getStatus());
        map.put("statusText", switch (l.getStatus()) {
            case "PENDING" -> "待审批"; case "APPROVED" -> "已通过";
            case "REJECTED" -> "已驳回"; default -> "已取消";
        });
        map.put("deductHours", l.getDeductHours());
        map.put("deductText", Integer.valueOf(1).equals(l.getDeductHours()) ? "扣 1 课时" : "不扣课时");
        map.put("submittedAt", l.getSubmittedAt());
        map.put("approvedAt", l.getApprovedAt());
        map.put("rejectReason", l.getRejectReason());
        map.put("makeupStatus", l.getMakeupStatus());
        return map;
    }

    private BigDecimal targetConsumed(String status, LeaveRequest approvedLeave) {
        if ("PRESENT".equals(status) || "ABSENT".equals(status)) return BigDecimal.ONE;
        if (approvedLeave != null && Integer.valueOf(0).equals(approvedLeave.getDeductHours())) {
            return BigDecimal.ZERO;
        }
        return BigDecimal.ONE;
    }

    private String statusText(String status) {
        return switch (status) {
            case "PRESENT" -> "出勤"; case "LEAVE" -> "请假"; default -> "缺勤";
        };
    }

    private String statusNote(String status) {
        return switch (status) {
            case "PRESENT" -> "出勤，自动消课"; case "LEAVE" -> "请假"; default -> "缺勤，按消课处理";
        };
    }

    private void syncLeaveAttendance(LeaveRequest leave, Lesson lesson, LoginUser user) {
        Attendance attendance = attendanceMapper.selectOne(new LambdaQueryWrapper<Attendance>()
                .eq(Attendance::getLessonId, lesson.getId())
                .eq(Attendance::getStudentId, leave.getStudentId()).last("limit 1"));
        BigDecimal target = Integer.valueOf(1).equals(leave.getDeductHours()) ? BigDecimal.ONE : BigDecimal.ZERO;
        BigDecimal before = attendance == null || attendance.getConsumedHours() == null
                ? BigDecimal.ZERO : attendance.getConsumedHours();
        BigDecimal delta = target.subtract(before);
        if (attendance == null) {
            attendance = new Attendance();
            attendance.setLessonId(lesson.getId());
            attendance.setStudentId(leave.getStudentId());
        }
        attendance.setStatus("LEAVE");
        attendance.setConsumedHours(target);
        attendance.setNote(Integer.valueOf(1).equals(leave.getDeductHours()) ? "请假，按规则消课" : "请假，不扣课时");
        attendance.setRecordedBy(user.getId());
        attendance.setRecordedAt(LocalDateTime.now());
        if (attendance.getId() == null) attendanceMapper.insert(attendance);
        else attendanceMapper.updateById(attendance);
        if (delta.compareTo(BigDecimal.ZERO) != 0) {
            changeHours(leave.getStudentId(), lesson.getId(), delta.negate(), user.getId(),
                    "请假审批：" + (delta.signum() > 0 ? "补扣 1 课时" : "返还课时"));
        }
    }

    private void changeHours(Long studentId, Long lessonId, BigDecimal change, Long operatorId, String reason) {
        HourAccount account = hourAccountMapper.selectOne(new LambdaQueryWrapper<HourAccount>()
                .eq(HourAccount::getStudentId, studentId).last("limit 1"));
        if (account == null) {
            account = new HourAccount();
            account.setStudentId(studentId);
            account.setTotalHours(BigDecimal.ZERO);
            account.setUsedHours(BigDecimal.ZERO);
            account.setRemainingHours(BigDecimal.ZERO);
            account.setValidUntil(LocalDate.now().plusMonths(12));
            hourAccountMapper.insert(account);
        }
        BigDecimal remaining = account.getRemainingHours() == null ? BigDecimal.ZERO : account.getRemainingHours();
        BigDecimal used = account.getUsedHours() == null ? BigDecimal.ZERO : account.getUsedHours();
        BigDecimal afterBalance = remaining.add(change);
        account.setRemainingHours(afterBalance);
        account.setUsedHours(used.subtract(change));
        hourAccountMapper.updateById(account);
        HourLedger ledger = new HourLedger();
        ledger.setStudentId(studentId);
        ledger.setLessonId(lessonId);
        ledger.setType("CONSUME");
        ledger.setChangeHours(change);
        ledger.setBalanceAfter(afterBalance);
        ledger.setOperatorId(operatorId);
        ledger.setReason(reason);
        hourLedgerMapper.insert(ledger);
    }

    private boolean hasWorkAuth(Long studentId) {
        return workAuthMapper.selectCount(new LambdaQueryWrapper<WorkAuth>()
                .eq(WorkAuth::getStudentId, studentId)
                .eq(WorkAuth::getStatus, "ACTIVE")) > 0;
    }

    private void audit(LoginUser user, String action, String targetType, Long targetId, String content) {
        SysAuditLog log = new SysAuditLog();
        log.setUserId(user == null ? null : user.getId());
        log.setOperatorName(user == null ? "教师" : user.getName());
        log.setActionType(action);
        log.setTargetType(targetType);
        log.setTargetId(targetId);
        log.setContent(content);
        log.setResult("SUCCESS");
        auditMapper.insert(log);
    }
}
