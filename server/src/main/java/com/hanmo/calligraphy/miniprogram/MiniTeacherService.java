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

/**
 * 教师端业务服务：工作台聚合、课表、点名消课、请假审批、作品上传与账号信息。
 *
 * <p><b>权限边界</b>：所有方法入口先 {@link #requireTeacher} 取当前登录人的教师档案
 * （未关联或已停用 → 403）；涉及具体课次的操作再经 {@link #requireMyLesson} 校验
 * {@code lesson.teacher_id == 本人}；班级、学员、待审批请假、作品列表都按本人 ID 过滤。</p>
 *
 * <p><b>消课幂等</b>：点名不直接扣课时，而是算出该学员本次应消课时 target 与该课次已记录的
 * {@code consumed_hours} before 的差额 delta，只按差额调整课时账户并写 {@code hour_ledger}，
 * 因此重复点名或改判出勤状态不会重复扣课。</p>
 *
 * <p><b>审批联动</b>：请假通过时同步写/更新 {@code attendance}（status=LEAVE）并按
 * {@code leave.deduct_hours} 补扣或返还课时；驳回只改状态与原因，不动课时。</p>
 */
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

    /**
     * 教师工作台聚合。
     *
     * <p>带班数按未结班班级统计，学员数按这些班里的 ACTIVE 学员去重统计；
     * 待审批请假只统计本人课次上的 PENDING 记录。</p>
     *
     * @param user 当前登录教师
     * @return teacher/today/todayLessons/weekLessons/classCount/studentCount/pendingLeaves/pendingWorks/weekStart
     * @throws BusinessException 403 未关联教师档案或档案已停用
     */
    public Map<String, Object> home(LoginUser user) {
        TeacherProfile teacher = requireTeacher(user);
        List<ClassGroup> classes = myClasses(teacher.getId());
        List<Long> classIds = classes.stream().map(ClassGroup::getId).toList();
        Set<Long> studentIds = new LinkedHashSet<>();
        if (!classIds.isEmpty()) {
            // 班级学员去重：一个学员可能同时在多个班
            classStudentMapper.selectList(new LambdaQueryWrapper<ClassStudent>()
                            .in(ClassStudent::getClassId, classIds).eq(ClassStudent::getStatus, "ACTIVE"))
                    .forEach(cs -> studentIds.add(cs.getStudentId()));
        }
        LocalDate today = LocalDate.now();
        List<Map<String, Object>> todayLessons = myLessons(teacher.getId(), today, today).stream()
                .map(l -> lessonMap(l, false)).toList();
        // 以今天定位到本周周一（中国习惯周一为一周起点）
        LocalDate monday = today.with(WeekFields.of(Locale.CHINA).dayOfWeek(), 1);
        List<Map<String, Object>> weekLessons = myLessons(teacher.getId(), monday, monday.plusDays(6)).stream()
                .map(l -> lessonMap(l, false)).toList();
        List<Long> myLessonIds = lessonIdsOfTeacher(teacher.getId());
        long pendingLeaves = leaveMapper.selectCount(new LambdaQueryWrapper<LeaveRequest>()
                // 没有课次时用 -1 占位，避免 IN () 空集合导致 SQL 语法错误
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

    /**
     * 某日课程。
     *
     * @param user 当前登录教师
     * @param date 日期，null 表示今天
     * @return 课次数组（含学员点名状态）
     * @throws BusinessException 403 未关联教师档案
     */
    public List<Map<String, Object>> lessons(LoginUser user, LocalDate date) {
        TeacherProfile teacher = requireTeacher(user);
        LocalDate day = date == null ? LocalDate.now() : date;
        return myLessons(teacher.getId(), day, day).stream().map(l -> lessonMap(l, true)).toList();
    }

    /**
     * 本周课程（周一至周日）。
     *
     * @param user 当前登录教师
     * @param week 该周任意日期，null 表示本周
     * @return 课次数组
     * @throws BusinessException 403 未关联教师档案
     */
    public List<Map<String, Object>> weekLessons(LoginUser user, LocalDate week) {
        TeacherProfile teacher = requireTeacher(user);
        LocalDate base = week == null ? LocalDate.now() : week;
        LocalDate monday = base.with(WeekFields.of(Locale.CHINA).dayOfWeek(), 1);
        return myLessons(teacher.getId(), monday, monday.plusDays(6)).stream()
                .map(l -> lessonMap(l, true)).toList();
    }

    /**
     * 课次详情 + 点名名单。
     *
     * @param user     当前登录教师
     * @param lessonId 课次 ID
     * @return lesson/classInfo/students/summary/pendingLeaves
     * @throws BusinessException 403 非本人课次；404 课次不存在
     */
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
        // summary 供教师端顶部进度使用：unmarked = 总人数 - 出勤 - 请假 - 缺勤
        data.put("summary", Map.of("total", roster.size(), "present", present, "leave", leave,
                "absent", absent, "unmarked", roster.size() - present - leave - absent,
                "done", "DONE".equals(lesson.getStatus())));
        data.put("pendingLeaves", roster.stream()
                .filter(r -> Boolean.TRUE.equals(r.get("pendingLeave"))).toList());
        return data;
    }

    /**
     * 点名并自动消课。
     *
     * <p>对每个学员：校验在课次班级名单中、状态合法；查该课次已审批的请假以决定应消课时；
     * 写/更新 {@code attendance}，并按与已有 consumedHours 的差额调整课时账户；
     * 最后把课次置为 DONE。整段在事务内。</p>
     *
     * <p>副作用：写 {@code attendance}、{@code hour_account}、{@code hour_ledger}、
     * 更新 {@code lesson.status}、写审计日志。</p>
     *
     * @param user     当前登录教师
     * @param lessonId 课次 ID
     * @param request  点名明细（不能为空、不能重复学员）
     * @return summary/items/message
     * @throws BusinessException 400 名单为空/重复/状态非法/学员不在班级；403 非本人课次；404 课次不存在
     */
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
            // 名单内学员重复或缺失都视为参数错误，避免同一人被扣两次
            if (item.studentId() == null || !seen.add(item.studentId())) {
                throw new BusinessException(400, "点名名单存在重复或缺失的学员");
            }
            if (!List.of("PRESENT", "LEAVE", "ABSENT").contains(item.status())) {
                throw new BusinessException(400, "出勤状态不正确");
            }
            Student student = requireLessonStudent(lesson, item.studentId());
            // 已审批的请假决定该学员是否扣课（deductHours=0 则不扣）
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
            // 只按差额调整课时：重复点名/改判状态时差额为 0，不产生重复扣课
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
        // 点名即视为本次课结束，课次统一标记 DONE
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

    /**
     * 待审批请假。
     *
     * <p>先取本人所有课次 ID，再按课次过滤 PENDING 请假；没有课次时直接返回空数组。</p>
     *
     * @param user 当前登录教师
     * @return 请假数组，按提交时间倒序
     * @throws BusinessException 403 未关联教师档案
     */
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

    /**
     * 已处理请假。
     *
     * @param user   当前登录教师
     * @param status 状态过滤，可选；不传时查非 PENDING
     * @return 请假数组，按 id 倒序
     * @throws BusinessException 403 未关联教师档案
     */
    public List<Map<String, Object>> leaveHistory(LoginUser user, String status) {
        TeacherProfile teacher = requireTeacher(user);
        List<Long> lessonIds = lessonIdsOfTeacher(teacher.getId());
        if (lessonIds.isEmpty()) return List.of();
        LambdaQueryWrapper<LeaveRequest> qw = new LambdaQueryWrapper<LeaveRequest>()
                .in(LeaveRequest::getLessonId, lessonIds);
        if (StringUtils.hasText(status)) qw.eq(LeaveRequest::getStatus, status);
        // 不传 status 时默认列为“已处理”，避免与待审批列表重复
        else qw.ne(LeaveRequest::getStatus, "PENDING");
        qw.orderByDesc(LeaveRequest::getId);
        return leaveMapper.selectList(qw).stream().map(this::leaveMap).toList();
    }

    /**
     * 审批请假。
     *
     * <p>权限：请假所在课次必须属于本人（{@link #requireMyLesson}），否则 403。
     * 通过时调用 {@link #syncLeaveAttendance} 同步出勤与课时；驳回必须填写原因。</p>
     *
     * <p>副作用：更新 {@code leave_request}；通过时写/更新 {@code attendance}、
     * 调整 {@code hour_account} 并写 {@code hour_ledger}；写审计日志。</p>
     *
     * @param user     当前登录教师
     * @param leaveId  请假记录 ID
     * @param request  approve=true 通过 / false 驳回 + 原因；null 视为通过
     * @return 审批后的请假记录
     * @throws BusinessException 400 已处理或缺驳回原因；403 非本人课次；404 请假不存在
     */
    @Transactional
    public Map<String, Object> approveLeave(LoginUser user, Long leaveId, ApproveLeaveRequest request) {
        TeacherProfile teacher = requireTeacher(user);
        LeaveRequest leave = leaveMapper.selectById(leaveId);
        if (leave == null) throw new BusinessException(404, "请假申请不存在");
        Lesson lesson = requireMyLesson(teacher, leave.getLessonId());
        if (!"PENDING".equals(leave.getStatus())) throw new BusinessException(400, "该请假已处理");
        // 只有显式传 false 才算驳回；null 或未传按通过处理
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

    /**
     * 作品上传选项。
     *
     * <p>返回本人所带班级的 ACTIVE 学员（去重）及家长授权状态，前端据此提示是否需要先登记授权。</p>
     *
     * @param user 当前登录教师
     * @return students + workTypes
     * @throws BusinessException 403 未关联教师档案
     */
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
                // 家长授权状态来自 work_auth，前端据此提示“该学员尚未登记授权”
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

    /**
     * 我上传的作品（含审核状态）。
     *
     * @param user 当前登录教师
     * @return 作品数组，按 id 倒序
     * @throws BusinessException 403 未关联教师档案
     */
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

    /**
     * 上传学员作品（提交审核）。
     *
     * <p>校验顺序：学员存在 → 学员在自己所带班级中 → 前端已确认授权 → 后台存在 ACTIVE 的
     * {@code work_auth} → 班级属于自己。任一不满足直接拒绝，避免越权上传或侵权展示。</p>
     *
     * <p>副作用：插入 {@code work}（PENDING，authorized=1，likeCount=0），写审计日志。</p>
     *
     * @param user    当前登录教师
     * @param request 作品信息
     * @return id/title/status/message
     * @throws BusinessException 400 未确认授权或无有效授权；403 学员不在自己班/班级越权；
     *         404 学员不存在
     */
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
        // 请求里的 authorized 只是教师勾选确认，后台 work_auth 才是事实，下面还要再查一次
        if (!Boolean.TRUE.equals(request.authorized())) {
            throw new BusinessException(400, "请先确认已取得家长作品展示授权");
        }
        if (!hasWorkAuth(student.getId())) {
            throw new BusinessException(400, "该学员尚无有效家长授权，请先在后台登记");
        }
        Long classId = request.classId();
        // 只能挂到自己所带班级，防止把作品挂到别人的班
        if (classId != null && !classIds.contains(classId)) throw new BusinessException(403, "只能选择自己所带班级");
        if (classId == null) {
            List<ClassStudent> links = classStudentMapper.selectList(new LambdaQueryWrapper<ClassStudent>()
                    .in(ClassStudent::getClassId, classIds).eq(ClassStudent::getStudentId, student.getId())
                    .eq(ClassStudent::getStatus, "ACTIVE").last("limit 1"));
            // 未指定班级时，默认取该学员在自己班中的第一个班
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

    /**
     * 教师账号信息（个人档案 + 带班与学员统计）。
     *
     * @param user 当前登录教师
     * @return teacher/identity/classCount/studentCount/classes
     * @throws BusinessException 403 未关联教师档案
     */
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

    // 取当前登录人的教师档案；未关联或档案停用一律 403
    private TeacherProfile requireTeacher(LoginUser user) {
        TeacherProfile teacher = teacherMapper.selectOne(new LambdaQueryWrapper<TeacherProfile>()
                .eq(TeacherProfile::getUserId, user.getId()).last("limit 1"));
        // 角色是 TEACHER 不代表一定有档案，没有档案不允许操作任何教师端数据
        if (teacher == null) throw new BusinessException(403, "当前账号未关联教师档案");
        if (!"ACTIVE".equals(teacher.getStatus())) throw new BusinessException(403, "教师档案已停用");
        return teacher;
    }

    // 本人所带且未结班的班级
    private List<ClassGroup> myClasses(Long teacherId) {
        return classGroupMapper.selectList(new LambdaQueryWrapper<ClassGroup>()
                // 已结班（FINISHED）不再出现在工作台与可上传范围
                .eq(ClassGroup::getTeacherId, teacherId).ne(ClassGroup::getStatus, "FINISHED")
                .orderByAsc(ClassGroup::getId));
    }

    // 按 teacher_id 和日期区间取本人课次
    private List<Lesson> myLessons(Long teacherId, LocalDate from, LocalDate to) {
        return lessonMapper.selectList(new LambdaQueryWrapper<Lesson>()
                .eq(Lesson::getTeacherId, teacherId)
                .ge(Lesson::getLessonDate, from).le(Lesson::getLessonDate, to)
                .orderByAsc(Lesson::getLessonDate).orderByAsc(Lesson::getStartTime));
    }

    // 本人全部课次 ID，用于过滤请假等挂在课次上的数据
    private List<Long> lessonIdsOfTeacher(Long teacherId) {
        return lessonMapper.selectList(new LambdaQueryWrapper<Lesson>()
                        .eq(Lesson::getTeacherId, teacherId))
                .stream().map(Lesson::getId).toList();
    }

    // 取课次并校验归属；非本人课次直接 403
    private Lesson requireMyLesson(TeacherProfile teacher, Long lessonId) {
        Lesson lesson = lessonMapper.selectById(lessonId);
        if (lesson == null) throw new BusinessException(404, "课次不存在");
        // 教师端最主要的权限边界：只能操作自己的课次
        if (!teacher.getId().equals(lesson.getTeacherId())) {
            throw new BusinessException(403, "只能操作自己的课次");
        }
        return lesson;
    }

    // 校验学员属于课次班级（或已有该课次出勤记录）
    private Student requireLessonStudent(Lesson lesson, Long studentId) {
        Student student = studentMapper.selectById(studentId);
        if (student == null) throw new BusinessException(404, "学员不存在");
        long linked = classStudentMapper.selectCount(new LambdaQueryWrapper<ClassStudent>()
                .eq(ClassStudent::getClassId, lesson.getClassId())
                .eq(ClassStudent::getStudentId, studentId));
        long attendance = attendanceMapper.selectCount(new LambdaQueryWrapper<Attendance>()
                .eq(Attendance::getLessonId, lesson.getId()).eq(Attendance::getStudentId, studentId));
        // 兼容历史数据：班级里已有该学员的记录（含非 ACTIVE）或已有出勤时，仍允许点名修正
        if (linked == 0 && attendance == 0) {
            throw new BusinessException(400, student.getName() + " 不在该班级名单中");
        }
        return student;
    }

    // 点名名单：班级 ACTIVE 学员 + 已有出勤 + 待审批请假
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
            // 名单里带上待审批请假，教师点名时能直接看到
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

    // 课次展示 Map；withRoster=true 时附带精简学员列表（工作台不需要）
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

    // 班级信息：课程名、教室名、上课时间、容量与在读人数
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

    // 教师档案展示字段（比公开接口多 teacherNo）
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

    // 教师端请假展示 Map：学员、班级、课次、原因、审批状态与扣课
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

    // 计算本次应消课时：出勤/缺勤 1；请假按已审批 deductHours，未审批按 1
    private BigDecimal targetConsumed(String status, LeaveRequest approvedLeave) {
        // 出勤与缺勤都消 1 课时；请假是否扣课看审批结果
        if ("PRESENT".equals(status) || "ABSENT".equals(status)) return BigDecimal.ONE;
        if (approvedLeave != null && Integer.valueOf(0).equals(approvedLeave.getDeductHours())) {
            return BigDecimal.ZERO;
        }
        return BigDecimal.ONE;
    }

    // 出勤状态文案：PRESENT 出勤 / LEAVE 请假 / 其他按缺勤
    private String statusText(String status) {
        return switch (status) {
            case "PRESENT" -> "出勤"; case "LEAVE" -> "请假"; default -> "缺勤";
        };
    }

    // 点名备注默认文案，教师未填写时使用
    private String statusNote(String status) {
        return switch (status) {
            case "PRESENT" -> "出勤，自动消课"; case "LEAVE" -> "请假"; default -> "缺勤，按消课处理";
        };
    }

    // 请假通过后同步出勤记录与课时：deductHours=1 扣 1，否则返还
    private void syncLeaveAttendance(LeaveRequest leave, Lesson lesson, LoginUser user) {
        Attendance attendance = attendanceMapper.selectOne(new LambdaQueryWrapper<Attendance>()
                .eq(Attendance::getLessonId, lesson.getId())
                .eq(Attendance::getStudentId, leave.getStudentId()).last("limit 1"));
        // 请假通过后的应消课时；与已有 consumedHours 的差额才真正入账
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

    // 调整课时账户并写流水：change 为负表示扣课，为正表示返还
    private void changeHours(Long studentId, Long lessonId, BigDecimal change, Long operatorId, String reason) {
        HourAccount account = hourAccountMapper.selectOne(new LambdaQueryWrapper<HourAccount>()
                .eq(HourAccount::getStudentId, studentId).last("limit 1"));
        // 账户不存在时先建零余额账户，保证余额与流水始终成对
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
        // change 为负数（扣课）时 used_hours 增加，反之减少
        account.setUsedHours(used.subtract(change));
        hourAccountMapper.updateById(account);
        HourLedger ledger = new HourLedger();
        ledger.setStudentId(studentId);
        ledger.setLessonId(lessonId);
        // 点名/审批产生的流水统一记 CONSUME，具体来源写在 reason
        ledger.setType("CONSUME");
        ledger.setChangeHours(change);
        ledger.setBalanceAfter(afterBalance);
        ledger.setOperatorId(operatorId);
        ledger.setReason(reason);
        hourLedgerMapper.insert(ledger);
    }

    // 是否存在 ACTIVE 的家长作品展示授权
    private boolean hasWorkAuth(Long studentId) {
        return workAuthMapper.selectCount(new LambdaQueryWrapper<WorkAuth>()
                .eq(WorkAuth::getStudentId, studentId)
                // 只认 ACTIVE 授权，已撤销的授权不能用于上传作品
                .eq(WorkAuth::getStatus, "ACTIVE")) > 0;
    }

    // 写一条成功审计日志（点名、审批、上传都要留痕）
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
