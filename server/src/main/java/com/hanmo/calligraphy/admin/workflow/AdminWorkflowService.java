package com.hanmo.calligraphy.admin.workflow;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.hanmo.calligraphy.admin.workflow.AdminWorkflowDtos.AttendanceItem;
import com.hanmo.calligraphy.admin.workflow.AdminWorkflowDtos.LeaveActionRequest;
import com.hanmo.calligraphy.admin.workflow.AdminWorkflowDtos.MakeupRequest;
import com.hanmo.calligraphy.admin.workflow.AdminWorkflowDtos.RollcallRequest;
import com.hanmo.calligraphy.common.BusinessException;
import com.hanmo.calligraphy.domain.entity.Attendance;
import com.hanmo.calligraphy.domain.entity.ClassGroup;
import com.hanmo.calligraphy.domain.entity.ClassStudent;
import com.hanmo.calligraphy.domain.entity.Classroom;
import com.hanmo.calligraphy.domain.entity.Course;
import com.hanmo.calligraphy.domain.entity.HourAccount;
import com.hanmo.calligraphy.domain.entity.HourLedger;
import com.hanmo.calligraphy.domain.entity.LeaveReason;
import com.hanmo.calligraphy.domain.entity.LeaveRequest;
import com.hanmo.calligraphy.domain.entity.Lesson;
import com.hanmo.calligraphy.domain.entity.MakeupArrangement;
import com.hanmo.calligraphy.domain.entity.Student;
import com.hanmo.calligraphy.domain.entity.TeacherProfile;
import com.hanmo.calligraphy.domain.mapper.AttendanceMapper;
import com.hanmo.calligraphy.domain.mapper.ClassGroupMapper;
import com.hanmo.calligraphy.domain.mapper.ClassStudentMapper;
import com.hanmo.calligraphy.domain.mapper.ClassroomMapper;
import com.hanmo.calligraphy.domain.mapper.CourseMapper;
import com.hanmo.calligraphy.domain.mapper.HourAccountMapper;
import com.hanmo.calligraphy.domain.mapper.HourLedgerMapper;
import com.hanmo.calligraphy.domain.mapper.LeaveReasonMapper;
import com.hanmo.calligraphy.domain.mapper.LeaveRequestMapper;
import com.hanmo.calligraphy.domain.mapper.LessonMapper;
import com.hanmo.calligraphy.domain.mapper.MakeupArrangementMapper;
import com.hanmo.calligraphy.domain.mapper.StudentMapper;
import com.hanmo.calligraphy.domain.mapper.TeacherProfileMapper;
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
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class AdminWorkflowService {

    private final LessonMapper lessonMapper;
    private final MakeupArrangementMapper makeupMapper;
    private final AttendanceMapper attendanceMapper;
    private final LeaveRequestMapper leaveMapper;
    private final LeaveReasonMapper leaveReasonMapper;
    private final ClassGroupMapper classGroupMapper;
    private final ClassStudentMapper classStudentMapper;
    private final ClassroomMapper classroomMapper;
    private final CourseMapper courseMapper;
    private final TeacherProfileMapper teacherMapper;
    private final StudentMapper studentMapper;
    private final HourAccountMapper hourAccountMapper;
    private final HourLedgerMapper hourLedgerMapper;
    private final SysAuditLogMapper auditMapper;

    public List<Map<String, Object>> lessons(LocalDate date, String status) {
        LambdaQueryWrapper<Lesson> qw = new LambdaQueryWrapper<>();
        if (date != null) qw.eq(Lesson::getLessonDate, date);
        if (StringUtils.hasText(status)) qw.eq(Lesson::getStatus, status);
        qw.orderByDesc(Lesson::getLessonDate).orderByAsc(Lesson::getStartTime);
        return lessonMapper.selectList(qw).stream().map(this::lessonMap).toList();
    }

    public Map<String, Object> roster(Long lessonId) {
        Lesson lesson = requireLesson(lessonId);
        List<Map<String, Object>> students = rosterRows(lesson);
        long present = students.stream().filter(s -> "PRESENT".equals(s.get("mark"))).count();
        long leave = students.stream().filter(s -> "LEAVE".equals(s.get("mark"))).count();
        long absent = students.stream().filter(s -> "ABSENT".equals(s.get("mark"))).count();
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("lesson", lessonMap(lesson));
        data.put("students", students);
        data.put("summary", Map.of("total", students.size(), "present", present,
                "leave", leave, "absent", absent, "unmarked", students.size() - present - leave - absent));
        return data;
    }

    @Transactional
    public Map<String, Object> rollcall(Long lessonId, RollcallRequest request, LoginUser operator) {
        Lesson lesson = requireLesson(lessonId);
        if ("CANCELLED".equals(lesson.getStatus())) throw new BusinessException(400, "已取消的课次不能点名");
        if (request == null || request.items() == null || request.items().isEmpty()) {
            throw new BusinessException(400, "请至少为一位学员标记出勤状态");
        }
        Set<Long> seen = new LinkedHashSet<>();
        for (AttendanceItem item : request.items()) {
            if (item.studentId() == null || !seen.add(item.studentId())) {
                throw new BusinessException(400, "点名名单存在重复或缺失学员");
            }
            if (!List.of("PRESENT", "LEAVE", "ABSENT").contains(item.status())) {
                throw new BusinessException(400, "出勤状态不正确");
            }
            if (classStudentMapper.selectCount(new LambdaQueryWrapper<ClassStudent>()
                    .eq(ClassStudent::getClassId, lesson.getClassId())
                    .eq(ClassStudent::getStudentId, item.studentId())) == 0) {
                throw new BusinessException(400, "学员不在该班级名单中");
            }
            LeaveRequest approved = leaveMapper.selectOne(new LambdaQueryWrapper<LeaveRequest>()
                    .eq(LeaveRequest::getLessonId, lesson.getId())
                    .eq(LeaveRequest::getStudentId, item.studentId())
                    .eq(LeaveRequest::getStatus, "APPROVED")
                    .orderByDesc(LeaveRequest::getId).last("limit 1"));
            BigDecimal target = targetConsumed(item.status(), approved);
            Attendance attendance = attendanceMapper.selectOne(new LambdaQueryWrapper<Attendance>()
                    .eq(Attendance::getLessonId, lesson.getId())
                    .eq(Attendance::getStudentId, item.studentId()).last("limit 1"));
            BigDecimal before = attendance == null || attendance.getConsumedHours() == null
                    ? BigDecimal.ZERO : attendance.getConsumedHours();
            BigDecimal delta = target.subtract(before);
            if (attendance == null) {
                attendance = new Attendance();
                attendance.setLessonId(lesson.getId());
                attendance.setStudentId(item.studentId());
            }
            attendance.setStatus(item.status());
            attendance.setConsumedHours(target);
            attendance.setNote(StringUtils.hasText(item.note()) ? item.note() : statusNote(item.status()));
            attendance.setRecordedBy(operator == null ? null : operator.getId());
            attendance.setRecordedAt(LocalDateTime.now());
            if (attendance.getId() == null) attendanceMapper.insert(attendance);
            else attendanceMapper.updateById(attendance);
            if (delta.compareTo(BigDecimal.ZERO) != 0) {
                changeHours(attendance.getStudentId(), lesson.getId(), delta.negate(),
                        operator == null ? null : operator.getId(),
                        "教务点名（第 " + lesson.getLessonNo() + " 课）" + (delta.signum() > 0 ? "消课" : "修正"));
            }
        }
        lesson.setStatus("DONE");
        lessonMapper.updateById(lesson);
        audit(operator, "点名消课", "课次", lesson.getId(), "教务完成点名，共 " + request.items().size() + " 人");
        return roster(lessonId);
    }

    @Transactional
    public Map<String, Object> handleLeave(Long leaveId, LeaveActionRequest request, LoginUser operator) {
        LeaveRequest leave = leaveMapper.selectById(leaveId);
        if (leave == null) throw new BusinessException(404, "请假申请不存在");
        if (!"PENDING".equals(leave.getStatus())) throw new BusinessException(400, "该请假已处理");
        Lesson lesson = requireLesson(leave.getLessonId());
        boolean approve = request == null || !Boolean.FALSE.equals(request.approve());
        if (!approve && (request == null || !StringUtils.hasText(request.rejectReason()))) {
            throw new BusinessException(400, "请填写驳回原因");
        }
        leave.setApproverId(operator == null ? null : operator.getId());
        leave.setApprovedAt(LocalDateTime.now());
        if (approve) {
            leave.setStatus("APPROVED");
            leave.setRejectReason(null);
            syncLeaveAttendance(leave, lesson, operator);
        } else {
            leave.setStatus("REJECTED");
            leave.setRejectReason(request.rejectReason().trim());
        }
        leaveMapper.updateById(leave);
        audit(operator, "请假审批", "请假", leave.getId(),
                (approve ? "通过" : "驳回") + "请假 " + leave.getLeaveNo());
        return leaveMap(leave);
    }

    @Transactional
    public Map<String, Object> arrangeMakeup(Long leaveId, MakeupRequest request, LoginUser operator) {
        LeaveRequest leave = leaveMapper.selectById(leaveId);
        if (leave == null) throw new BusinessException(404, "请假申请不存在");
        if (!"APPROVED".equals(leave.getStatus())) throw new BusinessException(400, "请假通过后才能安排补课");
        if ("ARRANGED".equals(leave.getMakeupStatus())) throw new BusinessException(400, "补课已安排");
        Lesson lesson = requireLesson(request.lessonId());
        if ("CANCELLED".equals(lesson.getStatus())) throw new BusinessException(400, "补课课次已取消");
        MakeupArrangement arrangement = new MakeupArrangement();
        arrangement.setLeaveRequestId(leave.getId());
        arrangement.setStudentId(leave.getStudentId());
        arrangement.setLessonId(lesson.getId());
        arrangement.setStatus("ARRANGED");
        arrangement.setArrangedBy(operator == null ? null : operator.getId());
        arrangement.setArrangedAt(LocalDateTime.now());
        arrangement.setNote(request.note());
        makeupMapper.insert(arrangement);
        leave.setMakeupStatus("ARRANGED");
        leaveMapper.updateById(leave);

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("id", arrangement.getId());
        data.put("leaveId", leave.getId());
        data.put("leaveNo", leave.getLeaveNo());
        data.put("studentId", leave.getStudentId());
        data.put("lessonId", lesson.getId());
        data.put("lessonDate", lesson.getLessonDate());
        data.put("startTime", lesson.getStartTime());
        data.put("endTime", lesson.getEndTime());
        data.put("status", arrangement.getStatus());
        data.put("note", request.note());
        audit(operator, "补课安排", "请假", leave.getId(),
                "安排补课至课次 " + lesson.getLessonDate() + " 第 " + lesson.getLessonNo() + " 课");
        return data;
    }

    private List<Map<String, Object>> rosterRows(Lesson lesson) {
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
            LeaveRequest leave = leaveMapper.selectOne(new LambdaQueryWrapper<LeaveRequest>()
                    .eq(LeaveRequest::getLessonId, lesson.getId())
                    .eq(LeaveRequest::getStudentId, student.getId())
                    .eq(LeaveRequest::getStatus, "PENDING")
                    .orderByDesc(LeaveRequest::getId).last("limit 1"));
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("studentId", student.getId());
            row.put("name", student.getName());
            row.put("studentNo", student.getStudentNo());
            row.put("mark", attendance == null ? null : attendance.getStatus());
            row.put("consumedHours", attendance == null ? BigDecimal.ZERO : attendance.getConsumedHours());
            row.put("note", attendance == null ? null : attendance.getNote());
            row.put("pendingLeave", leave != null);
            row.put("leaveReason", leave == null ? null : leave.getReasonText());
            list.add(row);
        }
        return list;
    }

    private Map<String, Object> lessonMap(Lesson lesson) {
        ClassGroup group = classGroupMapper.selectById(lesson.getClassId());
        Course course = group == null ? null : courseMapper.selectById(group.getCourseId());
        TeacherProfile teacher = lesson.getTeacherId() == null ? null : teacherMapper.selectById(lesson.getTeacherId());
        Classroom room = lesson.getClassroomId() == null ? null : classroomMapper.selectById(lesson.getClassroomId());
        long roster = classStudentMapper.selectCount(new LambdaQueryWrapper<ClassStudent>()
                .eq(ClassStudent::getClassId, lesson.getClassId()).eq(ClassStudent::getStatus, "ACTIVE"));
        long marked = attendanceMapper.selectCount(new LambdaQueryWrapper<Attendance>()
                .eq(Attendance::getLessonId, lesson.getId()));
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", lesson.getId());
        map.put("classId", lesson.getClassId());
        map.put("className", group == null ? "" : group.getName());
        map.put("courseName", course == null ? "" : course.getName());
        map.put("lessonNo", lesson.getLessonNo());
        map.put("lessonDate", lesson.getLessonDate());
        map.put("startTime", lesson.getStartTime());
        map.put("endTime", lesson.getEndTime());
        map.put("teacherName", teacher == null ? "" : teacher.getName());
        map.put("classroomName", room == null ? "" : room.getName());
        map.put("status", lesson.getStatus());
        map.put("statusText", switch (lesson.getStatus()) {
            case "DONE" -> "已完成"; case "CANCELLED" -> "已取消"; default -> "待上课";
        });
        map.put("rosterCount", roster);
        map.put("markedCount", marked);
        return map;
    }

    private Map<String, Object> leaveMap(LeaveRequest leave) {
        Student student = studentMapper.selectById(leave.getStudentId());
        Lesson lesson = lessonMapper.selectById(leave.getLessonId());
        ClassGroup group = lesson == null ? null : classGroupMapper.selectById(lesson.getClassId());
        LeaveReason reason = leave.getReasonId() == null ? null : leaveReasonMapper.selectById(leave.getReasonId());
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", leave.getId());
        map.put("leaveNo", leave.getLeaveNo());
        map.put("studentName", student == null ? "" : student.getName());
        map.put("className", group == null ? "" : group.getName());
        map.put("lessonNo", lesson == null ? null : lesson.getLessonNo());
        map.put("lessonDate", lesson == null ? null : lesson.getLessonDate());
        map.put("reasonName", reason == null ? "" : reason.getName());
        map.put("reasonText", leave.getReasonText());
        map.put("status", leave.getStatus());
        map.put("statusText", switch (leave.getStatus()) {
            case "PENDING" -> "待审批"; case "APPROVED" -> "已通过";
            case "REJECTED" -> "已驳回"; default -> "已取消";
        });
        map.put("deductHours", leave.getDeductHours());
        map.put("deductText", Integer.valueOf(1).equals(leave.getDeductHours()) ? "扣 1 课时" : "不扣课时");
        map.put("approverId", leave.getApproverId());
        map.put("approvedAt", leave.getApprovedAt());
        map.put("rejectReason", leave.getRejectReason());
        return map;
    }

    private BigDecimal targetConsumed(String status, LeaveRequest approvedLeave) {
        if ("PRESENT".equals(status) || "ABSENT".equals(status)) return BigDecimal.ONE;
        if (approvedLeave != null && Integer.valueOf(0).equals(approvedLeave.getDeductHours())) {
            return BigDecimal.ZERO;
        }
        return BigDecimal.ONE;
    }

    private String statusNote(String status) {
        return switch (status) {
            case "PRESENT" -> "出勤，自动消课"; case "LEAVE" -> "请假"; default -> "缺勤，按消课处理";
        };
    }

    private void syncLeaveAttendance(LeaveRequest leave, Lesson lesson, LoginUser operator) {
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
        attendance.setRecordedBy(operator == null ? null : operator.getId());
        attendance.setRecordedAt(LocalDateTime.now());
        if (attendance.getId() == null) attendanceMapper.insert(attendance);
        else attendanceMapper.updateById(attendance);
        if (delta.compareTo(BigDecimal.ZERO) != 0) {
            changeHours(leave.getStudentId(), lesson.getId(), delta.negate(),
                    operator == null ? null : operator.getId(),
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
        BigDecimal after = remaining.add(change);
        account.setRemainingHours(after);
        account.setUsedHours(used.subtract(change));
        hourAccountMapper.updateById(account);
        HourLedger ledger = new HourLedger();
        ledger.setStudentId(studentId);
        ledger.setLessonId(lessonId);
        ledger.setType("CONSUME");
        ledger.setChangeHours(change);
        ledger.setBalanceAfter(after);
        ledger.setOperatorId(operatorId);
        ledger.setReason(reason);
        hourLedgerMapper.insert(ledger);
    }

    private Lesson requireLesson(Long id) {
        Lesson lesson = lessonMapper.selectById(id);
        if (lesson == null) throw new BusinessException(404, "课次不存在");
        return lesson;
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
        auditMapper.insert(log);
    }
}
