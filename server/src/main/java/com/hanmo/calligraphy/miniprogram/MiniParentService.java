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

/**
 * 家长端业务服务：家长工作台、子女档案与绑定、课表/课次、请假、补课、报名、课时、出勤、
 * 成长相册与作品留言。
 *
 * <p><b>数据范围（越权防线）</b>：所有按 {@code studentId} 访问的方法入口都调用
 * {@link #requireChild} 或 {@link #requireBinding}，后者要求
 * {@code parent_student.parent_user_id=当前登录人 且 status=ACTIVE}，不满足直接抛 403。
 * 因此“知道学员 ID”并不等于能看该学员数据。</p>
 *
 * <p><b>绑定状态机</b>：新申请 PENDING → 教务审核 ACTIVE / REJECTED；撤销为 REVOKED。
 * 解绑不是立刻断开：{@code source=UNBIND} 且仍 ACTIVE 表示“解绑待审核”，
 * 审核期间家长仍可查看学员（见 {@link #requestUnbind}）。</p>
 *
 * <p><b>课时规则</b>：请假是否扣课由 {@code business_rule.leave_cutoff_hours}（默认 4 小时）决定，
 * 距开课不足该时长按扣 1 课时；补课与留言开关同样读 business_rule。</p>
 *
 * <p>本类所有写操作都会写 {@code sys_audit_log}，失败时抛 {@link BusinessException}。</p>
 */
@Service
@RequiredArgsConstructor
public class MiniParentService {

    // 关系、报名、请假状态码到中文文案的映射，前端直接展示 statusText
    private static final Map<String, String> RELATION_TEXT = Map.of(
            "MOTHER", "母亲", "FATHER", "父亲", "GRANDPARENT", "祖辈", "OTHER", "其他");
    // 报名状态文案；未知状态原样返回状态码
    private static final Map<String, String> ENROLL_TEXT = Map.of(
            "PENDING_CONFIRM", "待确认", "PENDING_PAYMENT", "待缴费",
            "ENROLLED", "已入班", "CANCELLED", "已取消");
    // 请假状态文案；未知状态原样返回状态码
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

    /**
     * 家长工作台聚合数据。
     *
     * <p>复用 {@link #children} 的子女卡片，并汇总各子女的待审批请假数，避免前端多次请求。</p>
     *
     * @param user 当前登录家长
     * @return parentName/children/notices/pendingBindings/pendingLeaves
     */
    public Map<String, Object> overview(LoginUser user) {
        // 工作台直接复用子女卡片，不再单独组装
        List<Map<String, Object>> children = children(user.getId());
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("parentName", user.getName());
        data.put("children", children);
        data.put("notices", noticeMapper.selectList(new LambdaQueryWrapper<Notice>()
                        .eq(Notice::getStatus, "PUBLISHED").orderByDesc(Notice::getPublishDate).last("limit 3"))
                .stream().map(publicService::noticeMap).toList());
        data.put("pendingBindings", bindingMapper.selectCount(new LambdaQueryWrapper<ParentStudent>()
                .eq(ParentStudent::getParentUserId, user.getId()).eq(ParentStudent::getStatus, "PENDING")));
        // 待审批请假数按子女汇总，供工作台红点使用
        long pendingLeave = 0;
        for (Map<String, Object> child : children) {
            pendingLeave += ((Number) child.getOrDefault("pendingLeaves", 0)).longValue();
        }
        data.put("pendingLeaves", pendingLeave);
        return data;
    }

    /**
     * 已绑定子女卡片列表。
     *
     * @param parentId 家长账号 ID
     * @return 子女卡片数组；绑定非 ACTIVE 的学员不会出现
     */
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

    /**
     * 学员档案详情。
     *
     * <p>在子女卡片基础上补充关系、主联系人标记、学校/年级/生日/性别、班级、课时账户
     * 与最近 3 条出勤。</p>
     *
     * @param user      当前登录家长
     * @param studentId 学员 ID
     * @return 档案 Map
     * @throws BusinessException 403 未绑定或绑定待审核；404 学员不存在
     */
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

    /**
     * 修改学员基础资料（性别、出生日期、学校、年级）。
     *
     * <p>只改动请求中非 null 的字段；副作用：更新 {@code student} 并写审计日志。
     * 姓名、学号等关键字段不在此开放，需联系教务。</p>
     *
     * @param user      当前登录家长
     * @param studentId 学员 ID
     * @param request   待修改字段，可为 null（此时不写库）
     * @return 修改后的档案详情
     * @throws BusinessException 403 未绑定；404 学员不存在
     */
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

    /**
     * 当前家长的全部绑定记录（含 PENDING/REJECTED/REVOKED 与解绑待审核）。
     *
     * @param user 当前登录家长
     * @return 绑定记录数组，按 id 倒序
     */
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

    /**
     * 提交家长-学员绑定申请。
     *
     * <p>两种来源：已有学员需“学员编号 + 主监护人手机号后四位”双重校验；
     * 新建学员则先建 {@code student}（状态 ACTIVE）再提交绑定。两种情况都落 PENDING 等教务审核。
     * 一个学员最多 2 位 ACTIVE 监护人，第一位自动为主联系人。</p>
     *
     * <p>副作用：可能插入 {@code student}；插入或复用 {@code parent_student}；写审计日志。</p>
     *
     * @param user    当前登录家长
     * @param request 绑定参数
     * @return studentId/studentName/studentNo/status/statusText/message
     * @throws BusinessException 400 参数缺失、编号不匹配、后四位错误、重复绑定或监护人已满；
     *         404 学员不存在
     */
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
            // 家长端新建的学员先落 ACTIVE，但绑定仍是 PENDING，审核通过后才可见课表与课时
            student.setRemark("家长端新建，待教务审核绑定");
            studentMapper.insert(student);
        } else if (request.studentId() != null || StringUtils.hasText(request.studentNo())) {
            if (request.studentId() != null) {
                student = studentMapper.selectById(request.studentId());
                if (student == null) throw new BusinessException(404, "学员档案不存在");
                if (StringUtils.hasText(request.studentNo())
                        // 用请求侧做 equalsIgnoreCase：档案里 student_no 为 null 时也不抛 NPE
                        && !request.studentNo().trim().equalsIgnoreCase(student.getStudentNo())) {
                    throw new BusinessException(400, "学员编号与档案不匹配");
                }
            } else {
                student = studentMapper.selectOne(new LambdaQueryWrapper<Student>()
                        .eq(Student::getStudentNo, request.studentNo().trim()).last("limit 1"));
                if (student == null) throw new BusinessException(404, "未找到该学员编号，请核对或联系教务");
            }
            // 绑定已有学员需同时校验学员编号与主监护人手机号后四位，防止猜编号盗绑
            if (!StringUtils.hasText(request.phoneTail())
                    || !request.phoneTail().trim().matches("\\d{4}")) {
                throw new BusinessException(400, "请填写监护人手机号后四位");
            }
            // 主监护人取学员档案上的 primary_parent_user_id
            SysUser primary = student.getPrimaryParentUserId() == null ? null
                    : userMapper.selectById(student.getPrimaryParentUserId());
            String phone = primary == null ? null : primary.getPhone();
            // 只比手机号后四位，兼顾安全与最小必要信息
            if (phone == null || !phone.endsWith(request.phoneTail().trim())) {
                throw new BusinessException(400, "监护人手机号后四位不正确");
            }
        } else {
            throw new BusinessException(400, "请选择学员或填写新学员信息");
        }

        ParentStudent exists = bindingMapper.selectOne(new LambdaQueryWrapper<ParentStudent>()
                .eq(ParentStudent::getParentUserId, user.getId())
                .eq(ParentStudent::getStudentId, student.getId()).last("limit 1"));
        // 已有 PENDING/ACTIVE 绑定不允许重复申请；被驳回/已解绑的记录可复用
        if (exists != null && !"REJECTED".equals(exists.getStatus()) && !"REVOKED".equals(exists.getStatus())) {
            throw new BusinessException(400, "该学员已绑定或正在审核中");
        }
        long guardians = bindingMapper.selectCount(new LambdaQueryWrapper<ParentStudent>()
                .eq(ParentStudent::getStudentId, student.getId()).eq(ParentStudent::getStatus, "ACTIVE"));
        // 一个学员最多 2 位 ACTIVE 监护人
        if (guardians >= 2) throw new BusinessException(400, "该学员已有 2 位有效监护人，请联系教务");
        // 当前没有有效监护人时，新绑定即为主联系人
        boolean first = guardians == 0;
        // 命中被驳回/已解绑的旧记录时复用并重置为 PENDING，避免同一对家长-学员产生多条
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

    /**
     * 申请解绑学员（不立即生效）。
     *
     * <p>只把 {@code source} 置为 UNBIND，{@code status} 仍保持 ACTIVE：
     * 教务审核期间家长仍可查看学员，审核通过后才 REVOKED。重复申请直接拒绝。</p>
     *
     * <p>副作用：更新绑定记录 + 写审计日志。</p>
     *
     * @param user      当前登录家长
     * @param bindingId 绑定记录 ID
     * @return status=UNBIND_PENDING、statusText
     * @throws BusinessException 400 绑定非 ACTIVE 或已申请解绑；404 绑定不存在/非本人
     */
    @Transactional
    public Map<String, Object> requestUnbind(LoginUser user, Long bindingId) {
        ParentStudent binding = bindingMapper.selectById(bindingId);
        if (binding == null || !user.getId().equals(binding.getParentUserId())) {
            throw new BusinessException(404, "绑定关系不存在");
        }
        if (!"ACTIVE".equals(binding.getStatus())) throw new BusinessException(400, "当前绑定状态不能申请解绑");
        // ACTIVE + source=UNBIND 就是“解绑待审核”，重复申请直接拒绝
        if ("UNBIND".equals(binding.getSource())) throw new BusinessException(400, "已提交解绑申请，请等待教务审核");
        // 审核期间保持 ACTIVE，家长仍可查看学员；source=UNBIND 作为待审核标记
        binding.setSource("UNBIND");
        bindingMapper.updateById(binding);
        audit(user, "解绑申请", "家长-学员", binding.getId(), "家长申请解除绑定");
        return Map.of("status", "UNBIND_PENDING", "statusText", "解绑待审核");
    }

    /**
     * 某周课表（周一至周日）。
     *
     * <p>按学员当前在读班级（{@code class_student.status=ACTIVE}）取该周课次，
     * 返回课次状态与该学员的出勤/请假状态。</p>
     *
     * @param user      当前登录家长
     * @param studentId 学员 ID
     * @param anyDay    该周任意日期，null 表示本周
     * @return child/hours/weekStart/weekEnd/lessons/today
     * @throws BusinessException 403 未绑定该学员
     */
    public Map<String, Object> schedule(LoginUser user, Long studentId, LocalDate anyDay) {
        Student student = requireChild(user.getId(), studentId);
        LocalDate base = anyDay == null ? LocalDate.now() : anyDay;
        // 以任意日期定位到所在周的周一（中国习惯周一为一周起点）
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

    /**
     * 课次详情：课次、学员出勤与请假、能否请假提示。
     *
     * <p>{@code canLeave} 为真需同时满足：课次 SCHEDULED、尚未开始、没有 PENDING/APPROVED 请假。</p>
     *
     * @param user      当前登录家长
     * @param studentId 学员 ID
     * @param lessonId  课次 ID
     * @return 课次 Map 追加 child/leave/canLeave/leaveHint
     * @throws BusinessException 403 课次不属于该学员；404 课次不存在
     */
    public Map<String, Object> lessonDetail(LoginUser user, Long studentId, Long lessonId) {
        Student student = requireChild(user.getId(), studentId);
        Lesson lesson = lessonMapper.selectById(lessonId);
        if (lesson == null) throw new BusinessException(404, "课次不存在");
        // 课次须属于该学员在读班级；已退班但留有该课次出勤记录的也放行查看
        boolean belongs = activeClassIds(studentId).contains(lesson.getClassId());
        if (!belongs && attendanceMapper.selectCount(new LambdaQueryWrapper<Attendance>()
                .eq(Attendance::getLessonId, lessonId).eq(Attendance::getStudentId, studentId)) == 0) {
            throw new BusinessException(403, "无权查看该课次");
        }
        Map<String, Object> data = lessonMap(lesson, studentId);
        data.put("child", childSummary(student, requireBinding(user.getId(), studentId)));
        // 同一课次的请假只查一次，下面判断 canLeave 还要用
        LeaveRequest leave = leaveOfLesson(studentId, lessonId);
        if (leave != null) data.put("leave", leave);
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

    /**
     * 请假申请选项：可请假课次 + 原因字典 + 截止小时数与规则文案。
     *
     * @param user      当前登录家长
     * @param studentId 学员 ID
     * @return child/lessons/reasons/cutoffHours/ruleText
     * @throws BusinessException 403 未绑定该学员
     */
    public Map<String, Object> leaveOptions(LoginUser user, Long studentId) {
        Student student = requireChild(user.getId(), studentId);
        List<Long> classIds = activeClassIds(studentId);
        List<Map<String, Object>> lessons = classIds.isEmpty() ? List.of()
                : lessonMapper.selectList(new LambdaQueryWrapper<Lesson>()
                        .in(Lesson::getClassId, classIds)
                        .eq(Lesson::getStatus, "SCHEDULED")
                        .ge(Lesson::getLessonDate, LocalDate.now())
                        .orderByAsc(Lesson::getLessonDate).orderByAsc(Lesson::getStartTime))
                // 只保留尚未开始的课次；已有 PENDING/APPROVED 请假的课次也不出现在可选项里
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

    /**
     * 提交请假申请。
     *
     * <p>校验：课次属于该学员、状态 SCHEDULED、尚未开始、无 PENDING/APPROVED 请假。
     * 按 {@code leave_cutoff_hours} 判断是否扣课：距开课不足该小时数则 deductHours=1。</p>
     *
     * <p>副作用：插入 {@code leave_request}（PENDING），写审计日志；审批由教师端完成。</p>
     *
     * @param user    当前登录家长
     * @param request 学员、课次、原因
     * @return id/leaveNo/status/statusText/deductHours/deductText/lesson
     * @throws BusinessException 400 课次不属于该学员、不可请假、已开始或已有申请；404 课次不存在
     */
    @Transactional
    public Map<String, Object> createLeave(LoginUser user, LeaveCreateRequest request) {
        Student student = requireChild(user.getId(), request.studentId());
        Lesson lesson = lessonMapper.selectById(request.lessonId());
        if (lesson == null) throw new BusinessException(404, "课次不存在");
        // 防止家长拿不属于自己子女的课次 ID 请假
        if (!activeClassIds(student.getId()).contains(lesson.getClassId())) {
            throw new BusinessException(400, "该课次不属于当前学员");
        }
        if (!"SCHEDULED".equals(lesson.getStatus())) throw new BusinessException(400, "该课次不可请假");
        LocalDateTime start = LocalDateTime.of(lesson.getLessonDate(), lesson.getStartTime());
        if (!start.isAfter(LocalDateTime.now())) throw new BusinessException(400, "课次已开始，不能请假");
        LeaveRequest exists = leaveOfLesson(student.getId(), lesson.getId());
        if (exists != null) throw new BusinessException(400, "该课次已有待审批或已通过的请假申请");
        long cutoff = ruleInt("leave_cutoff_hours", 4);
        // 距开课不足 cutoff 小时才扣课时；toHours() 向下取整，正好等于 cutoff 视为不扣
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

    /**
     * 请假记录查询。
     *
     * @param user      当前登录家长
     * @param studentId 学员 ID
     * @param status    状态过滤，可选
     * @return 请假记录数组，按 id 倒序
     * @throws BusinessException 403 未绑定该学员
     */
    public List<Map<String, Object>> leaveRecords(LoginUser user, Long studentId, String status) {
        Student student = requireChild(user.getId(), studentId);
        LambdaQueryWrapper<LeaveRequest> qw = new LambdaQueryWrapper<LeaveRequest>()
                .eq(LeaveRequest::getStudentId, studentId);
        if (StringUtils.hasText(status)) {
            // 防御：前端可能把绑定状态 UNBIND 误当请假状态传入，直接返回空列表
            if ("UNBIND".equals(status)) return List.of();
            qw.eq(LeaveRequest::getStatus, status);
        }
        qw.orderByDesc(LeaveRequest::getId);
        return leaveMapper.selectList(qw).stream().map(l -> leaveMap(l, student)).toList();
    }

    /**
     * 请假通过后申请补课。
     *
     * <p>副作用：{@code leave_request.makeup_status} 置为 PENDING，写审计日志；
     * 是否开放由 {@code business_rule.makeup_enabled} 控制。</p>
     *
     * @param user    当前登录家长
     * @param leaveId 请假记录 ID
     * @return 更新后的请假记录
     * @throws BusinessException 400 未通过审批、未开放或已申请；404 请假不存在
     */
    @Transactional
    public Map<String, Object> applyMakeup(LoginUser user, Long leaveId) {
        LeaveRequest leave = leaveMapper.selectById(leaveId);
        if (leave == null) throw new BusinessException(404, "请假记录不存在");
        Student student = requireChild(user.getId(), leave.getStudentId());
        if (!"APPROVED".equals(leave.getStatus())) throw new BusinessException(400, "请假通过后才能申请补课");
        // 补课开关由 business_rule.makeup_enabled 控制，默认开启
        if (ruleInt("makeup_enabled", 1) != 1) throw new BusinessException(400, "当前未开放补课申请");
        if ("PENDING".equals(leave.getMakeupStatus()) || "ARRANGED".equals(leave.getMakeupStatus())) {
            throw new BusinessException(400, "补课已申请或已安排");
        }
        leave.setMakeupStatus("PENDING");
        leaveMapper.updateById(leave);
        audit(user, "补课申请", "请假", leave.getId(), "家长申请补课 " + leave.getLeaveNo());
        return leaveMap(leave, student);
    }

    /**
     * 该学员的报名记录。
     *
     * @param user      当前登录家长
     * @param studentId 学员 ID
     * @return 报名记录数组，按 id 倒序
     * @throws BusinessException 403 未绑定该学员
     */
    public List<Map<String, Object>> enrollments(LoginUser user, Long studentId) {
        Student student = requireChild(user.getId(), studentId);
        return enrollmentMapper.selectList(new LambdaQueryWrapper<Enrollment>()
                        .eq(Enrollment::getStudentId, student.getId()).orderByDesc(Enrollment::getId))
                .stream().map(this::enrollmentMap).toList();
    }

    /**
     * 报名选项：上架课程 + 可报名班级（含实时人数与最低价）。
     *
     * @param user      当前登录家长
     * @param studentId 学员 ID
     * @return child/courses/classes
     * @throws BusinessException 403 未绑定该学员
     */
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

    /**
     * 提交报名申请。
     *
     * <p>校验：课程上架、班级与课程匹配、班级 ACTIVE、名额按 ACTIVE 学员数实时判断、
     * 同课程无 PENDING_CONFIRM/PENDING_PAYMENT/ENROLLED 记录。</p>
     *
     * <p>副作用：插入 {@code enrollment}（PENDING_CONFIRM，金额取最低价，source=MINI），
     * 写审计日志；后续由教务确认并收费。</p>
     *
     * @param user    当前登录家长
     * @param request 学员、课程、班级
     * @return 报名记录追加 message
     * @throws BusinessException 400 课程/班级不合法、名额已满或重复报名
     */
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
            // 名额按 class_student 中 ACTIVE 人数实时计算，未做缓存
            if (group.getCapacity() != null && count >= group.getCapacity()) {
                throw new BusinessException(400, "该班级名额已满");
            }
        }
        // 同课程已有进行中的报名时不允许重复提交
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

    /**
     * 取消尚未确认的报名。
     *
     * <p>副作用：状态置为 CANCELLED，写审计日志。报名阶段不占班级名额，
     * 因此取消也无需回退 class_student。</p>
     *
     * @param user 当前登录家长
     * @param id   报名 ID
     * @return 取消后的报名记录
     * @throws BusinessException 400 状态不允许取消；404 报名不存在或非本人
     */
    @Transactional
    public Map<String, Object> cancelEnrollment(LoginUser user, Long id) {
        Enrollment enrollment = enrollmentMapper.selectById(id);
        // 只能取消自己提交的报名；他人的报名按不存在处理，避免探测
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

    /**
     * 课时账户与流水。
     *
     * @param user      当前登录家长
     * @param studentId 学员 ID
     * @return child/account/ledger/renewalHint
     * @throws BusinessException 403 未绑定该学员
     */
    public Map<String, Object> hours(LoginUser user, Long studentId) {
        Student student = requireChild(user.getId(), studentId);
        Map<String, Object> account = hourAccount(studentId);
        List<Map<String, Object>> ledger = hourLedgerMapper.selectList(new LambdaQueryWrapper<HourLedger>()
                        .eq(HourLedger::getStudentId, studentId).orderByDesc(HourLedger::getId))
                .stream().map(l -> {
                    Map<String, Object> item = new LinkedHashMap<>();
                    item.put("id", l.getId());
                    // 流水类型：PURCHASE 充值 / CONSUME 消课，其余一律按“修正”展示
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

    /**
     * 按学员取上课记录（内部复用方法）。
     *
     * <p><b>本方法不做数据范围校验</b>：调用方必须已通过 {@code requireChild/requireBinding}，
     * 目前只有 {@link #attendance} 与 {@link #childDetail} 调用它。</p>
     *
     * @param studentId 学员 ID
     * @param status    出勤状态过滤，可选
     * @return 上课记录数组，按 id 倒序
     */
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

    /**
     * 上课记录。
     *
     * @param user      当前登录家长
     * @param studentId 学员 ID
     * @param status    出勤状态过滤，可选
     * @return child/records
     * @throws BusinessException 403 未绑定该学员
     */
    public Map<String, Object> attendance(LoginUser user, Long studentId, String status) {
        Student student = requireChild(user.getId(), studentId);
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("child", childSummary(student, requireBinding(user.getId(), studentId)));
        data.put("records", attendanceRecords(studentId, status));
        return data;
    }

    /**
     * 成长相册。
     *
     * <p>只取 {@code status=DISPLAYED 且 authorized=1} 的作品，保证家长未授权的作品不会展示；
     * 按创建月份分组，取该月最新一条作品点评作为月度点评。</p>
     *
     * @param user      当前登录家长
     * @param studentId 学员 ID
     * @return child/timeline/workCount
     * @throws BusinessException 403 未绑定该学员
     */
    public Map<String, Object> growth(LoginUser user, Long studentId) {
        Student student = requireChild(user.getId(), studentId);
        List<Work> works = workMapper.selectList(new LambdaQueryWrapper<Work>()
                // 成长相册只收“已展示 + 家长已授权”的作品，两者缺一不可
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

    /**
     * 提交作品留言。
     *
     * <p>校验作品已展示、留言开关开启、正文 1–300 字；留言落 PENDING，教务审核后才公开。</p>
     *
     * <p>副作用：插入 {@code work_comment}（PENDING），不写审计日志。</p>
     *
     * @param user    当前登录家长
     * @param workId  作品 ID
     * @param content 留言正文
     * @return id/status/message
     * @throws BusinessException 400 未开放、内容为空或超长；404 作品不存在或未展示
     */
    @Transactional
    public Map<String, Object> comment(LoginUser user, Long workId, String content) {
        Work work = workMapper.selectById(workId);
        if (work == null || !"DISPLAYED".equals(work.getStatus())) {
            throw new BusinessException(404, "作品不存在或未展示");
        }
        if (ruleInt("comment_enabled", 1) != 1) throw new BusinessException(400, "当前未开放作品留言");
        String text = content == null ? "" : content.trim();
        if (text.isEmpty()) throw new BusinessException(400, "请输入留言内容");
        // 与前端提示保持一致：留言上限 300 字
        if (text.length() > 300) throw new BusinessException(400, "留言不超过 300 字");
        WorkComment comment = new WorkComment();
        comment.setWorkId(workId);
        comment.setParentUserId(user.getId());
        comment.setContent(text);
        comment.setStatus("PENDING");
        commentMapper.insert(comment);
        return Map.of("id", comment.getId(), "status", "PENDING", "message", "留言已提交，审核通过后展示");
    }

    /**
     * 家长账号信息。
     *
     * <p>手机号经 {@link #mask} 脱敏后返回，避免小程序端缓存明文号码。</p>
     *
     * @param user 当前登录家长
     * @return 账号、微信绑定状态、身份、绑定关系与子女简要列表
     * @throws BusinessException 401 账号不存在
     */
    public Map<String, Object> account(LoginUser user) {
        SysUser sysUser = userMapper.selectById(user.getId());
        if (sysUser == null) throw new BusinessException(401, "账号不存在");
        ParentProfile profile = parentProfileMapper.selectOne(new LambdaQueryWrapper<ParentProfile>()
                .eq(ParentProfile::getUserId, user.getId()).last("limit 1"));
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("name", sysUser.getName());
        // 账号页手机号脱敏，只保留前 3 位与后 4 位
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

    // 子女卡片：学员基础信息 + 头像字/色 + 班级 + 课时 + 待审批请假、报名、出勤计数
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
        // 头像用姓名首字，缺姓名时兜底“学”；颜色按 ID 固定取色
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

    // 课次展示 Map：班级/课程/教师/教室/状态，以及该学员的出勤与请假
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

    // 请假记录展示 Map：单号、课次、原因、状态、扣课、审批人、补课状态
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

    // 报名记录展示 Map：课程/班级名、金额、状态、时间
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

    // 报名可选班级：含实时已报人数、剩余名额与课程最低价
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

    // 课时账户：总数/已消/剩余/有效期/续费预警；无账户时返回全 0
    private Map<String, Object> hourAccount(Long studentId) {
        HourAccount account = hourAccountMapper.selectOne(new LambdaQueryWrapper<HourAccount>()
                .eq(HourAccount::getStudentId, studentId).last("limit 1"));
        Map<String, Object> map = new LinkedHashMap<>();
        // 没有课时账户时返回 0 并标记需要续费，避免前端出现 null
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

    // 按在读班级 ID 批量取班级实体
    private List<ClassGroup> classesOfStudent(Long studentId) {
        List<Long> ids = activeClassIds(studentId);
        return ids.isEmpty() ? List.of() : classGroupMapper.selectBatchIds(ids);
    }

    // 学员当前在读（ACTIVE）的班级 ID，课表与请假范围都以它为准
    private List<Long> activeClassIds(Long studentId) {
        return classStudentMapper.selectList(new LambdaQueryWrapper<ClassStudent>()
                        .eq(ClassStudent::getStudentId, studentId).eq(ClassStudent::getStatus, "ACTIVE"))
                .stream().map(ClassStudent::getClassId).toList();
    }

    // 取学员并校验绑定，是所有按 studentId 操作的统一入口
    private Student requireChild(Long parentId, Long studentId) {
        // 先校验绑定关系再取档案，避免泄露“学员是否存在”
        requireBinding(parentId, studentId);
        Student student = studentMapper.selectById(studentId);
        if (student == null) throw new BusinessException(404, "学员不存在");
        return student;
    }

    // 数据范围校验：家长与学员必须存在 ACTIVE 绑定，否则 403
    private ParentStudent requireBinding(Long parentId, Long studentId) {
        ParentStudent binding = bindingMapper.selectOne(new LambdaQueryWrapper<ParentStudent>()
                .eq(ParentStudent::getParentUserId, parentId)
                .eq(ParentStudent::getStudentId, studentId)
                .eq(ParentStudent::getStatus, "ACTIVE").last("limit 1"));
        // 未绑定或绑定待审核一律 403，本方法就是家长端越权访问的总闸口
        if (binding == null) throw new BusinessException(403, "未绑定该学员或绑定仍在审核中");
        return binding;
    }

    // 家长的 ACTIVE 绑定，主联系人排前面
    private List<ParentStudent> activeBindings(Long parentId) {
        return bindingMapper.selectList(new LambdaQueryWrapper<ParentStudent>()
                .eq(ParentStudent::getParentUserId, parentId)
                // 只取 ACTIVE；解绑待审核仍是 ACTIVE，因此仍会出现在子女列表
                .eq(ParentStudent::getStatus, "ACTIVE").orderByDesc(ParentStudent::getIsPrimary));
    }

    // 取该学员该课次的有效请假（PENDING/APPROVED），没有返回 null
    private LeaveRequest leaveOfLesson(Long studentId, Long lessonId) {
        return leaveMapper.selectOne(new LambdaQueryWrapper<LeaveRequest>()
                .eq(LeaveRequest::getStudentId, studentId)
                .eq(LeaveRequest::getLessonId, lessonId)
                // 已驳回/已取消不占用“同一课次只能请假一次”的名额
                .in(LeaveRequest::getStatus, "PENDING", "APPROVED")
                .orderByDesc(LeaveRequest::getId).last("limit 1"));
    }

    // 教师名缺失返回空串，避免前端渲染 null
    private String teacherName(Long teacherId) {
        TeacherProfile t = teacherId == null ? null : teacherMapper.selectById(teacherId);
        return t == null ? "" : t.getName();
    }

    // 教室名缺失返回空串（课次可能未指定教室）
    private String classroomName(Long classroomId) {
        Classroom c = classroomId == null ? null : classroomMapper.selectById(classroomId);
        return c == null ? "" : c.getName();
    }

    // 审批人等账号名缺失返回空串
    private String userName(Long userId) {
        SysUser user = userId == null ? null : userMapper.selectById(userId);
        return user == null ? "" : user.getName();
    }

    // 请假原因名缺失时用“请假”兜底（家长未填说明也没选原因）
    private String reasonName(Long id) {
        LeaveReason r = id == null ? null : leaveReasonMapper.selectById(id);
        return r == null ? "请假" : r.getName();
    }

    // 关系值白名单校验，非法值统一落库为 OTHER
    private String normalizeRelation(String relation) {
        if (!StringUtils.hasText(relation)) return "OTHER";
        return RELATION_TEXT.containsKey(relation) ? relation : "OTHER";
    }

    // 绑定状态文案；ACTIVE + source=UNBIND 显示“解绑待审核”
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

    // 生成学员编号：HX + 年份 + 4 位随机数，查重后返回
    private String nextStudentNo() {
        String prefix = "HX" + LocalDate.now().getYear();
        // 最多重试 20 次，仍冲突则报错让用户重试
        for (int i = 0; i < 20; i++) {
            String no = prefix + String.format("%04d", ThreadLocalRandom.current().nextInt(10000));
            if (studentMapper.selectCount(new LambdaQueryWrapper<Student>().eq(Student::getStudentNo, no)) == 0) {
                return no;
            }
        }
        throw new BusinessException(500, "学员编号生成失败，请重试");
    }

    // 课程最低有效价格；没有价格时返回 0，作为报名金额兜底
    private BigDecimal minPrice(Long courseId) {
        List<CoursePrice> prices = priceMapper.selectList(new LambdaQueryWrapper<CoursePrice>()
                .eq(CoursePrice::getCourseId, courseId).eq(CoursePrice::getStatus, "ON"));
        return prices.stream().filter(p -> p.getPrice() != null)
                .min(Comparator.comparing(CoursePrice::getPrice))
                .map(CoursePrice::getPrice).orElse(BigDecimal.ZERO);
    }

    // 读取整数型业务规则，缺失或非法时回退到 fallback
    private int ruleInt(String key, int fallback) {
        BusinessRule rule = ruleMapper.selectOne(new LambdaQueryWrapper<BusinessRule>()
                .eq(BusinessRule::getRuleKey, key).last("limit 1"));
        // 配置缺失或为空时用代码默认值，保证业务不因配置缺失中断
        if (rule == null || !StringUtils.hasText(rule.getRuleValue())) return fallback;
        try {
            return Integer.parseInt(rule.getRuleValue().trim());
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    // 按 ID 取模选固定色板，保证同一学员头像颜色稳定
    private String avatarColor(Long id) {
        String[] colors = {"#8A6A49", "#7C5B6B", "#4C6455", "#92664A", "#4A5C72"};
        return colors[(int) Math.floorMod(id == null ? 0 : id, colors.length)];
    }

    // 手机号脱敏：前 3 位 + 4 个星号 + 后 4 位
    private String mask(String phone) {
        // 号码过短时原样返回，避免 substring 越界
        if (phone == null || phone.length() < 7) return phone;
        return phone.substring(0, 3) + "****" + phone.substring(phone.length() - 4);
    }

    // 写一条成功审计日志（家长端写操作留痕）
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
