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

/**
 * 点名消课与请假/补课联动服务：后台课次列表与名单、点名消课、请假审批、补课安排。
 *
 * <p>核心链路：点名按「目标课时 - 已记课时」的差额调整 hour_account 并写 hour_ledger，
 * 因此重复点名天然幂等；请假审批通过后写一条 LEAVE 出勤并按审批时确定的 deduct_hours
 * 补扣或返还课时；补课只写 makeup_arrangement，不重复计费。</p>
 *
 * <p>依赖表：lesson、attendance、leave_request、makeup_arrangement、class_student、class_group、
 * classroom、course、teacher_profile、student、hour_account、hour_ledger、leave_reason、sys_audit_log。</p>
 */
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

    /**
     * 点名页的课次列表。
     *
     * <p>纯查询；date/status 为空则不过滤，按日期倒序、同日按开始时间升序。
     * 返回字段见 lessonMap（班级/课程/教师/教室名、名单人数、已点名人数、状态文案）。</p>
     *
     * @param date   上课日期，可为空
     * @param status SCHEDULED/DONE/CANCELLED，可为空
     * @return 课次视图列表
     */
    public List<Map<String, Object>> lessons(LocalDate date, String status) {
        LambdaQueryWrapper<Lesson> qw = new LambdaQueryWrapper<>();
        if (date != null) qw.eq(Lesson::getLessonDate, date);
        if (StringUtils.hasText(status)) qw.eq(Lesson::getStatus, status);
        qw.orderByDesc(Lesson::getLessonDate).orderByAsc(Lesson::getStartTime);
        return lessonMapper.selectList(qw).stream().map(this::lessonMap).toList();
    }

    /**
     * 课次点名名单与汇总。
     *
     * <p>纯查询。名单只含 ACTIVE 班级学员，每人带已记出勤状态/课时、是否待审批请假。
     * summary.unmarked = total - present - leave - absent（即尚未点名的条数）。</p>
     *
     * @param lessonId 课次 ID
     * @return lesson + students + summary
     * @throws BusinessException 404 课次不存在
     */
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

    /**
     * 后台点名并自动消课（可重复提交，按差额修正）。
     *
     * <p>消课口径：PRESENT / ABSENT 各记 1 课时；LEAVE 时若该学员本课次有已通过且
     * deduct_hours=0 的请假则记 0，否则（含未审批请假）记 1。</p>
     *
     * <p>幂等与修正：target 是「本次提交后应记的课时」，before 是库里已记课时，
     * delta = target - before；只有 delta≠0 才调整账户并写流水。因此同状态重复提交不会重复扣课，
     * 把出勤改判为请假（或反之）只补扣/返还差额。</p>
     *
     * <p>副作用（@Transactional 内）：① upsert attendance（uk_lesson_student 唯一）；
     * ② delta≠0 时 hour_account.remaining -= delta、used_hours += delta，并写一条
     * hour_ledger（type=CONSUME，change_hours=-delta，balance_after 为变动后余额）；
     * ③ 全部在班学员都已标记时课次置 DONE，仍有未标记则保持原状态；④ 写审计日志。点名不校验账户余额，可能出现负剩余。</p>
     *
     * @param lessonId 课次 ID
     * @param request  学员出勤明细列表（至少 1 条，学员不可重复）
     * @param operator 操作人，写入 attendance.recorded_by、流水与审计
     * @return 点名后的最新名单（结构同 roster）
     * @throws BusinessException 404 课次不存在；400 课次已取消、名单为空/重复、状态非法、学员不在班级
     */
    @Transactional
    public Map<String, Object> rollcall(Long lessonId, RollcallRequest request, LoginUser operator) {
        Lesson lesson = requireLesson(lessonId);
        // 已取消的课次不产生出勤与课时，直接拒绝点名
        if ("CANCELLED".equals(lesson.getStatus())) throw new BusinessException(400, "已取消的课次不能点名");
        if (request == null || request.items() == null || request.items().isEmpty()) {
            throw new BusinessException(400, "请至少为一位学员标记出勤状态");
        }
        // 同一次提交内防重：LinkedHashSet 既判重又保留前端顺序，报错信息稳定
        Set<Long> seen = new LinkedHashSet<>();
        for (AttendanceItem item : request.items()) {
            if (item.studentId() == null || !seen.add(item.studentId())) {
                throw new BusinessException(400, "点名名单存在重复或缺失学员");
            }
            if (!List.of("PRESENT", "LEAVE", "ABSENT").contains(item.status())) {
                throw new BusinessException(400, "出勤状态不正确");
            }
            // 只允许给本班学员点名；class_student 移出是物理删除，故这里不另加 status 条件
            if (classStudentMapper.selectCount(new LambdaQueryWrapper<ClassStudent>()
                    .eq(ClassStudent::getClassId, lesson.getClassId())
                    .eq(ClassStudent::getStudentId, item.studentId())) == 0) {
                throw new BusinessException(400, "学员不在该班级名单中");
            }
            // 取该学员本课次最近一条「已通过」请假：扣不扣课时在审批时就已经定好
            LeaveRequest approved = leaveMapper.selectOne(new LambdaQueryWrapper<LeaveRequest>()
                    .eq(LeaveRequest::getLessonId, lesson.getId())
                    .eq(LeaveRequest::getStudentId, item.studentId())
                    .eq(LeaveRequest::getStatus, "APPROVED")
                    .orderByDesc(LeaveRequest::getId).last("limit 1"));
            // 目标课时 = 出勤状态与已通过请假共同决定，详见 targetConsumed
            BigDecimal target = targetConsumed(item.status(), approved);
            Attendance attendance = attendanceMapper.selectOne(new LambdaQueryWrapper<Attendance>()
                    .eq(Attendance::getLessonId, lesson.getId())
                    .eq(Attendance::getStudentId, item.studentId()).last("limit 1"));
            // before 是上次点名已记的消课数；delta 才是本次真正要补扣/返还的差额
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
            // 首次点名 insert，重复点名 update 同一行（uk_attendance_lesson_student 唯一）
            if (attendance.getId() == null) attendanceMapper.insert(attendance);
            else attendanceMapper.updateById(attendance);
            // 差额为 0 时完全不动账户：同状态重复提交不会重复扣课，也不产生多余流水
            if (delta.compareTo(BigDecimal.ZERO) != 0) {
                // 账户变动取 delta 的相反数：delta>0（多消课）则扣减，delta<0（修正）则返还
                changeHours(attendance.getStudentId(), lesson.getId(), delta.negate(),
                        operator == null ? null : operator.getId(),
                        "教务点名（第 " + lesson.getLessonNo() + " 课）" + (delta.signum() > 0 ? "消课" : "修正"));
            }
        }
        // 只有「全部在班学员都已标记」才置 DONE；仍有未标记时保持原状态，
        // 否则统计与「待点名」提醒会把半截点名当成已完成。已 DONE 的课次不回退（学员后加班等场景）。
        Map<String, Object> result = roster(lessonId);
        @SuppressWarnings("unchecked")
        Map<String, Object> summary = (Map<String, Object>) result.get("summary");
        long unmarked = summary == null ? 0 : ((Number) summary.getOrDefault("unmarked", 0)).longValue();
        if (unmarked == 0) {
            lesson.setStatus("DONE");
            lessonMapper.updateById(lesson);
        }
        audit(operator, "点名消课", "课次", lesson.getId(),
                "教务点名 " + request.items().size() + " 人"
                        + (unmarked == 0 ? "，全部在班学员已标记，课次完成" : "，仍有 " + unmarked + " 人未标记"));
        // result 是更新前的快照，置 DONE 后重查一次，保证返回给前端的课次状态与库一致
        return unmarked == 0 ? roster(lessonId) : result;
    }

    /**
     * 后台审批请假，并在通过时联动出勤与课时。
     *
     * <p>通过（approve 缺省即为通过）：status=APPROVED、清空驳回原因，调用 syncLeaveAttendance
     * 写一条 LEAVE 出勤，并按 deduct_hours（1 扣 1 课时 / 0 不扣）与已记课时的差额补扣或返还。
     * 驳回：status=REJECTED，必须填写原因，只改状态、不动课时。</p>
     *
     * <p>副作用（@Transactional 内）：更新 leave_request（审批人/时间/状态）；通过时还会
     * upsert attendance、调整 hour_account、写 hour_ledger；最后写审计日志。
     * 已处理的请假不能再次审批，避免重复联动。</p>
     *
     * @param leaveId  请假 ID
     * @param request  approve（false 才是驳回）与 rejectReason
     * @param operator 审批人，写入 approver_id 与审计
     * @return 请假视图（含状态/扣课文案）
     * @throws BusinessException 404 请假或课次不存在；400 已处理、驳回未填原因
     */
    @Transactional
    public Map<String, Object> handleLeave(Long leaveId, LeaveActionRequest request, LoginUser operator) {
        LeaveRequest leave = leaveMapper.selectById(leaveId);
        if (leave == null) throw new BusinessException(404, "请假申请不存在");
        // 只允许审批一次：重复审批会再次联动课时，造成重复扣/退
        if (!"PENDING".equals(leave.getStatus())) throw new BusinessException(400, "该请假已处理");
        Lesson lesson = requireLesson(leave.getLessonId());
        // 请求体缺省或 approve≠false 都按「通过」处理；只有显式 false 才是驳回
        boolean approve = request == null || !Boolean.FALSE.equals(request.approve());
        if (!approve && (request == null || !StringUtils.hasText(request.rejectReason()))) {
            throw new BusinessException(400, "请填写驳回原因");
        }
        leave.setApproverId(operator == null ? null : operator.getId());
        leave.setApprovedAt(LocalDateTime.now());
        if (approve) {
            leave.setStatus("APPROVED");
            leave.setRejectReason(null);
            // 通过：按提交时判定好的 deduct_hours 写出勤并补扣/返还课时
            syncLeaveAttendance(leave, lesson, operator);
        } else {
            leave.setStatus("REJECTED");
            // 驳回只记录原因，不写 attendance、不动课时
            leave.setRejectReason(request.rejectReason().trim());
        }
        leaveMapper.updateById(leave);
        audit(operator, "请假审批", "请假", leave.getId(),
                (approve ? "通过" : "驳回") + "请假 " + leave.getLeaveNo());
        return leaveMap(leave);
    }

    /**
     * 为已通过的请假安排补课。
     *
     * <p>校验：请假必须 APPROVED、makeup_status≠ARRANGED（同一请假只能安排一次）、
     * 目标课次存在且未取消。补课不重复计费，因此不动课时账户。</p>
     *
     * <p>副作用（@Transactional 内）：insert makeup_arrangement（status=ARRANGED）；
     * 更新 leave_request.makeup_status=ARRANGED；写审计日志。</p>
     *
     * @param leaveId  请假 ID
     * @param request  补课课次（必填）与备注
     * @param operator 安排人，写入 arranged_by 与审计
     * @return 补课安排摘要（含课次日期与时间）
     * @throws BusinessException 404 请假或课次不存在；400 请假未通过、已安排过、补课课次已取消
     */
    @Transactional
    public Map<String, Object> arrangeMakeup(Long leaveId, MakeupRequest request, LoginUser operator) {
        LeaveRequest leave = leaveMapper.selectById(leaveId);
        if (leave == null) throw new BusinessException(404, "请假申请不存在");
        // 补课前置：请假必须已通过，且同一请假只能安排一次
        if (!"APPROVED".equals(leave.getStatus())) throw new BusinessException(400, "请假通过后才能安排补课");
        if ("ARRANGED".equals(leave.getMakeupStatus())) throw new BusinessException(400, "补课已安排");
        Lesson lesson = requireLesson(request.lessonId());
        if ("CANCELLED".equals(lesson.getStatus())) throw new BusinessException(400, "补课课次已取消");
        // 写补课安排并把请假置 ARRANGED；补课不扣课时（原请假已按规则处理过，避免重复计费）
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

    // 名单只取 ACTIVE；逐人查点名与待审批请假，属于 N+1，班级人数少时可接受
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

    // 课次视图：补齐班级/课程/教师/教室名与名单人数、已点名人数，供点名列表展示
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
        // 状态文案：DONE=已完成、CANCELLED=已取消，其余（含空值）按待上课展示
        map.put("statusText", switch (lesson.getStatus()) {
            case "DONE" -> "已完成"; case "CANCELLED" -> "已取消"; default -> "待上课";
        });
        map.put("rosterCount", roster);
        map.put("markedCount", marked);
        return map;
    }

    // 请假视图：关联学员/班级/课次/原因，并把 deduct_hours 转成「扣 1 课时 / 不扣课时」
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
        // 只有 deduct_hours=1 才算扣课；0 与 null 都按不扣展示
        map.put("deductText", Integer.valueOf(1).equals(leave.getDeductHours()) ? "扣 1 课时" : "不扣课时");
        map.put("approverId", leave.getApproverId());
        map.put("approvedAt", leave.getApprovedAt());
        map.put("rejectReason", leave.getRejectReason());
        return map;
    }

    // 消课口径：出勤/缺勤一律 1 课时；请假时只有「已通过且 deduct_hours=0」才记 0，
    // 未审批或审批为扣课的请假仍记 1（文档口径：未审批请假按扣课处理）
    private BigDecimal targetConsumed(String status, LeaveRequest approvedLeave) {
        if ("PRESENT".equals(status) || "ABSENT".equals(status)) return BigDecimal.ONE;
        if (approvedLeave != null && Integer.valueOf(0).equals(approvedLeave.getDeductHours())) {
            return BigDecimal.ZERO;
        }
        return BigDecimal.ONE;
    }

    // 未填备注时按出勤状态生成默认说明，保证点名记录可读
    private String statusNote(String status) {
        return switch (status) {
            case "PRESENT" -> "出勤，自动消课"; case "LEAVE" -> "请假"; default -> "缺勤，按消课处理";
        };
    }

    // 请假审批通过后的联动：写/更新 LEAVE 出勤，并按「目标 - 已记」差额调整课时与流水。
    // 若点名时已按出勤记了 1 课时，后来改判为「不扣课时」的请假，这里会返还 1 课时。
    private void syncLeaveAttendance(LeaveRequest leave, Lesson lesson, LoginUser operator) {
        Attendance attendance = attendanceMapper.selectOne(new LambdaQueryWrapper<Attendance>()
                .eq(Attendance::getLessonId, lesson.getId())
                .eq(Attendance::getStudentId, leave.getStudentId()).last("limit 1"));
        // 目标课时只看审批时的 deduct_hours：1=扣 1，0=不扣
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
        // 与已有点名记录比对差额：先出勤后改判请假时，在这里补扣或返还
        if (delta.compareTo(BigDecimal.ZERO) != 0) {
            changeHours(leave.getStudentId(), lesson.getId(), delta.negate(),
                    operator == null ? null : operator.getId(),
                    "请假审批：" + (delta.signum() > 0 ? "补扣 1 课时" : "返还课时"));
        }
    }

    // 课时变动统一入口：upsert 账户 + 写流水。change 为负是消课，为正是返还/修正。
    private void changeHours(Long studentId, Long lessonId, BigDecimal change, Long operatorId, String reason) {
        HourAccount account = hourAccountMapper.selectOne(new LambdaQueryWrapper<HourAccount>()
                .eq(HourAccount::getStudentId, studentId).last("limit 1"));
        // 账户缺失时补建（有效期 12 个月），避免点名/审批因缺账户失败
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
        // 余额与已用课时同步：remaining += change，used -= change（消课时 change 为负，used 增加）
        account.setUsedHours(used.subtract(change));
        hourAccountMapper.updateById(account);
        HourLedger ledger = new HourLedger();
        ledger.setStudentId(studentId);
        ledger.setLessonId(lessonId);
        // 扣课与返还都记 CONSUME：change_hours 带符号，返还时为正数；类型没有区分 CORRECT
        ledger.setType("CONSUME");
        ledger.setChangeHours(change);
        ledger.setBalanceAfter(after);
        ledger.setOperatorId(operatorId);
        ledger.setReason(reason);
        hourLedgerMapper.insert(ledger);
    }

    // 课次不存在统一抛 404
    private Lesson requireLesson(Long id) {
        Lesson lesson = lessonMapper.selectById(id);
        if (lesson == null) throw new BusinessException(404, "课次不存在");
        return lesson;
    }

    // 统一审计：记录点名/请假审批/补课安排的操作人与内容
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
