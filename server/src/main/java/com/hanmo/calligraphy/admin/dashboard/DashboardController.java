package com.hanmo.calligraphy.admin.dashboard;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.hanmo.calligraphy.common.ApiResponse;
import com.hanmo.calligraphy.domain.entity.Course;
import com.hanmo.calligraphy.domain.entity.Enrollment;
import com.hanmo.calligraphy.domain.entity.HourLedger;
import com.hanmo.calligraphy.domain.entity.Lesson;
import com.hanmo.calligraphy.domain.entity.PaymentRecord;
import com.hanmo.calligraphy.domain.entity.Student;
import com.hanmo.calligraphy.domain.entity.TeacherProfile;
import com.hanmo.calligraphy.domain.mapper.CourseMapper;
import com.hanmo.calligraphy.domain.mapper.EnrollmentMapper;
import com.hanmo.calligraphy.domain.mapper.HourLedgerMapper;
import com.hanmo.calligraphy.domain.mapper.LessonMapper;
import com.hanmo.calligraphy.domain.mapper.PaymentRecordMapper;
import com.hanmo.calligraphy.domain.mapper.StudentMapper;
import com.hanmo.calligraphy.domain.mapper.TeacherProfileMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 管理端数据看板：聚合首屏指标（在读学员、教师、本月消课/实收、待办报名、今日课程）、
 * 近 6 个月消课趋势与最近 5 条报名。
 *
 * <p>本类没有 service 层，直接注入 mapper 做只读聚合；全部接口为查询，不写库、不写审计、
 * 不开事务，也没有按登录人角色做数据范围过滤——看板是「全机构口径」。</p>
 *
 * <p>依赖表：student、teacher_profile、hour_ledger、payment_record、enrollment、lesson、course。</p>
 */
@Tag(name = "管理端-数据看板")
@RestController
@RequestMapping("/api/admin/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final StudentMapper studentMapper;
    private final TeacherProfileMapper teacherMapper;
    private final HourLedgerMapper hourLedgerMapper;
    private final PaymentRecordMapper paymentMapper;
    private final EnrollmentMapper enrollmentMapper;
    private final LessonMapper lessonMapper;
    private final CourseMapper courseMapper;

    /**
     * 看板汇总数据。
     *
     * <p>统计口径（均按服务器本地时区）：
     * ① students = student.status=ACTIVE 的档案数；
     * ② teachers = teacher_profile.status=ACTIVE 的档案数；
     * ③ monthConsumedHours = 本月 hour_ledger 中 change_hours&lt;0 的绝对值之和（只算扣减，
     *    充值与修正为正向不计入）；
     * ④ monthRevenue = 本月 payment_record.paid_at 落在当月的 amount 之和（线下登记口径）；
     * ⑤ pendingEnrollments = status ∈ (PENDING_CONFIRM, PENDING_PAYMENT) 的报名数；
     * ⑥ todayLessons = lesson.lesson_date = 今天的课次数（不区分 DONE/CANCELLED）。</p>
     *
     * <p>副作用：无（纯查询）。返回 Map 的键顺序固定，前端按字段名取值。</p>
     *
     * @return 看板数据：6 个指标 + chart（近 6 月消课）+ recentEnrollments（最近 5 条报名）
     */
    @Operation(summary = "看板数据")
    @GetMapping
    public ApiResponse<Map<String, Object>> dashboard() {
        LocalDate today = LocalDate.now();
        YearMonth month = YearMonth.from(today);
        LocalDateTime monthStart = month.atDay(1).atStartOfDay();
        LocalDateTime monthEnd = month.plusMonths(1).atDay(1).atStartOfDay();

        // 在读学员：只算 ACTIVE，PAUSED/GRADUATED/LEFT 不计入
        long students = studentMapper.selectCount(new LambdaQueryWrapper<Student>().eq(Student::getStatus, "ACTIVE"));
        long teachers = teacherMapper.selectCount(new LambdaQueryWrapper<TeacherProfile>().eq(TeacherProfile::getStatus, "ACTIVE"));
        List<HourLedger> monthLedgers = hourLedgerMapper.selectList(new LambdaQueryWrapper<HourLedger>()
                .ge(HourLedger::getCreatedAt, monthStart).lt(HourLedger::getCreatedAt, monthEnd));
        // 本月消课 = 负向流水绝对值求和；正向（充值/修正加课）不参与，避免把充值算成消课
        BigDecimal consumed = monthLedgers.stream()
                .map(HourLedger::getChangeHours)
                .filter(v -> v != null && v.compareTo(BigDecimal.ZERO) < 0)
                .map(BigDecimal::abs).reduce(BigDecimal.ZERO, BigDecimal::add);
        // 本月实收：按 paid_at 落在当月汇总缴费金额，退款不在本期模型内
        BigDecimal revenue = paymentMapper.selectList(new LambdaQueryWrapper<PaymentRecord>()
                        .ge(PaymentRecord::getPaidAt, monthStart).lt(PaymentRecord::getPaidAt, monthEnd))
                .stream().map(PaymentRecord::getAmount).filter(v -> v != null)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        // 待办报名：待确认 + 待缴费都算「未完成」，是教务每天要清的队列
        long pendingEnroll = enrollmentMapper.selectCount(new LambdaQueryWrapper<Enrollment>()
                .in(Enrollment::getStatus, List.of("PENDING_CONFIRM", "PENDING_PAYMENT")));
        // 今日课程：只按日期统计，未过滤课次状态，因此包含已取消/已完成的课次
        long todayLessons = lessonMapper.selectCount(new LambdaQueryWrapper<Lesson>().eq(Lesson::getLessonDate, today));

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("students", students);
        data.put("teachers", teachers);
        data.put("monthConsumedHours", consumed);
        data.put("monthRevenue", revenue);
        data.put("pendingEnrollments", pendingEnroll);
        data.put("todayLessons", todayLessons);
        data.put("chart", chart());
        data.put("recentEnrollments", recentEnrollments());
        return ApiResponse.ok(data);
    }

    // 近 6 个月消课趋势：i=5 为 5 个月前，i=0 为当月，保证图表从左到右时间递增。
    // 口径与本月消课一致：只统计负向流水并取绝对值。
    private List<Map<String, Object>> chart() {
        YearMonth now = YearMonth.now();
        List<Map<String, Object>> result = new ArrayList<>();
        for (int i = 5; i >= 0; i--) {
            YearMonth ym = now.minusMonths(i);
            LocalDateTime start = ym.atDay(1).atStartOfDay();
            LocalDateTime end = ym.plusMonths(1).atDay(1).atStartOfDay();
            BigDecimal sum = hourLedgerMapper.selectList(new LambdaQueryWrapper<HourLedger>()
                            .ge(HourLedger::getCreatedAt, start).lt(HourLedger::getCreatedAt, end))
                    .stream().map(HourLedger::getChangeHours)
                    .filter(v -> v != null && v.compareTo(BigDecimal.ZERO) < 0)
                    .map(BigDecimal::abs).reduce(BigDecimal.ZERO, BigDecimal::add);
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("month", ym.format(DateTimeFormatter.ofPattern("M 月")));
            item.put("value", sum);
            result.add(item);
        }
        return result;
    }

    // 最近 5 条报名：先按 id 倒序取 5 条，再批量查学员/课程名（避免逐条查询的 N+1）。
    // 学员或课程已被逻辑删除时名称降级为空串。
    private List<Map<String, Object>> recentEnrollments() {
        List<Enrollment> list = enrollmentMapper.selectList(new LambdaQueryWrapper<Enrollment>()
                .orderByDesc(Enrollment::getId).last("limit 5"));
        Map<Long, Student> students = list.isEmpty() ? Map.of() :
                studentMapper.selectBatchIds(list.stream().map(Enrollment::getStudentId).distinct().toList())
                        .stream().collect(Collectors.toMap(Student::getId, s -> s));
        Map<Long, Course> courses = list.isEmpty() ? Map.of() :
                courseMapper.selectBatchIds(list.stream().map(Enrollment::getCourseId).distinct().toList())
                        .stream().collect(Collectors.toMap(Course::getId, c -> c));
        return list.stream().map(e -> {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("id", e.getId());
            item.put("enrollmentNo", e.getEnrollmentNo());
            item.put("studentName", students.get(e.getStudentId()) == null ? "" : students.get(e.getStudentId()).getName());
            item.put("courseName", courses.get(e.getCourseId()) == null ? "" : courses.get(e.getCourseId()).getName());
            item.put("amount", e.getAmount());
            item.put("status", e.getStatus());
            item.put("submittedAt", e.getSubmittedAt());
            return item;
        }).toList();
    }
}
