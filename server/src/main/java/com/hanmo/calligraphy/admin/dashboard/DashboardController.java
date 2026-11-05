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

    @Operation(summary = "看板数据")
    @GetMapping
    public ApiResponse<Map<String, Object>> dashboard() {
        LocalDate today = LocalDate.now();
        YearMonth month = YearMonth.from(today);
        LocalDateTime monthStart = month.atDay(1).atStartOfDay();
        LocalDateTime monthEnd = month.plusMonths(1).atDay(1).atStartOfDay();

        long students = studentMapper.selectCount(new LambdaQueryWrapper<Student>().eq(Student::getStatus, "ACTIVE"));
        long teachers = teacherMapper.selectCount(new LambdaQueryWrapper<TeacherProfile>().eq(TeacherProfile::getStatus, "ACTIVE"));
        List<HourLedger> monthLedgers = hourLedgerMapper.selectList(new LambdaQueryWrapper<HourLedger>()
                .ge(HourLedger::getCreatedAt, monthStart).lt(HourLedger::getCreatedAt, monthEnd));
        BigDecimal consumed = monthLedgers.stream()
                .map(HourLedger::getChangeHours)
                .filter(v -> v != null && v.compareTo(BigDecimal.ZERO) < 0)
                .map(BigDecimal::abs).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal revenue = paymentMapper.selectList(new LambdaQueryWrapper<PaymentRecord>()
                        .ge(PaymentRecord::getPaidAt, monthStart).lt(PaymentRecord::getPaidAt, monthEnd))
                .stream().map(PaymentRecord::getAmount).filter(v -> v != null)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        long pendingEnroll = enrollmentMapper.selectCount(new LambdaQueryWrapper<Enrollment>()
                .in(Enrollment::getStatus, List.of("PENDING_CONFIRM", "PENDING_PAYMENT")));
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
