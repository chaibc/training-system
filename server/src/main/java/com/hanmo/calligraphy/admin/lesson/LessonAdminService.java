package com.hanmo.calligraphy.admin.lesson;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.hanmo.calligraphy.admin.lesson.LessonAdminDtos.LessonRequest;
import com.hanmo.calligraphy.common.BusinessException;
import com.hanmo.calligraphy.common.PageResult;
import com.hanmo.calligraphy.domain.entity.ClassGroup;
import com.hanmo.calligraphy.domain.entity.ClassStudent;
import com.hanmo.calligraphy.domain.entity.Classroom;
import com.hanmo.calligraphy.domain.entity.HourAccount;
import com.hanmo.calligraphy.domain.entity.Lesson;
import com.hanmo.calligraphy.domain.entity.Student;
import com.hanmo.calligraphy.domain.entity.TeacherProfile;
import com.hanmo.calligraphy.domain.entity.Attendance;
import com.hanmo.calligraphy.domain.mapper.AttendanceMapper;
import com.hanmo.calligraphy.domain.mapper.ClassGroupMapper;
import com.hanmo.calligraphy.domain.mapper.ClassStudentMapper;
import com.hanmo.calligraphy.domain.mapper.ClassroomMapper;
import com.hanmo.calligraphy.domain.mapper.HourAccountMapper;
import com.hanmo.calligraphy.domain.mapper.LessonMapper;
import com.hanmo.calligraphy.domain.mapper.StudentMapper;
import com.hanmo.calligraphy.domain.mapper.TeacherProfileMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class LessonAdminService {

    private final LessonMapper lessonMapper;
    private final ClassGroupMapper classGroupMapper;
    private final ClassStudentMapper classStudentMapper;
    private final ClassroomMapper classroomMapper;
    private final TeacherProfileMapper teacherMapper;
    private final StudentMapper studentMapper;
    private final HourAccountMapper hourAccountMapper;
    private final AttendanceMapper attendanceMapper;

    public PageResult<Lesson> page(long current, long size) {
        Page<Lesson> page = new Page<>(Math.max(1, current), Math.min(Math.max(1, size), 500));
        lessonMapper.selectPage(page, new LambdaQueryWrapper<Lesson>()
                .orderByDesc(Lesson::getLessonDate).orderByAsc(Lesson::getStartTime));
        return PageResult.of(page.getRecords(), page.getTotal(), page.getCurrent(), page.getSize());
    }

    public List<Lesson> list() {
        return lessonMapper.selectList(new LambdaQueryWrapper<Lesson>()
                .orderByDesc(Lesson::getLessonDate).orderByAsc(Lesson::getStartTime));
    }

    public Lesson get(Long id) {
        Lesson lesson = lessonMapper.selectById(id);
        if (lesson == null) throw new BusinessException(404, "课次不存在");
        return lesson;
    }

    @Transactional
    public Lesson save(Long id, LessonRequest request) {
        ClassGroup group = classGroupMapper.selectById(request.classId());
        if (group == null) throw new BusinessException(404, "班级不存在");
        if (!request.startTime().isBefore(request.endTime())) {
            throw new BusinessException(400, "结束时间必须晚于开始时间");
        }
        Long teacherId = request.teacherId() != null ? request.teacherId() : group.getTeacherId();
        Long classroomId = request.classroomId() != null ? request.classroomId() : group.getClassroomId();
        if (teacherId == null) throw new BusinessException(400, "请选择任课教师");
        if (teacherMapper.selectById(teacherId) == null) throw new BusinessException(400, "任课教师不存在");
        if (classroomId != null && classroomMapper.selectById(classroomId) == null) {
            throw new BusinessException(400, "教室不存在");
        }
        long duplicateNo = lessonMapper.selectCount(new LambdaQueryWrapper<Lesson>()
                .eq(Lesson::getClassId, request.classId())
                .eq(Lesson::getLessonNo, request.lessonNo())
                .ne(id != null, Lesson::getId, id));
        if (duplicateNo > 0) throw new BusinessException(400, "该班级已有相同课次序号");
        validateConflict(id, request.lessonDate(), request.startTime(), request.endTime(), teacherId, classroomId, group);

        Lesson lesson = id == null ? new Lesson() : lessonMapper.selectById(id);
        if (lesson == null) throw new BusinessException(404, "课次不存在");
        lesson.setClassId(request.classId());
        lesson.setLessonNo(request.lessonNo());
        lesson.setLessonDate(request.lessonDate());
        lesson.setStartTime(request.startTime());
        lesson.setEndTime(request.endTime());
        lesson.setTeacherId(teacherId);
        lesson.setClassroomId(classroomId);
        lesson.setStatus(StringUtils.hasText(request.status()) ? request.status() : "SCHEDULED");
        lesson.setOriginalLessonId(request.originalLessonId());
        lesson.setNote(request.note());
        if (id == null) lessonMapper.insert(lesson);
        else lessonMapper.updateById(lesson);
        return lesson;
    }

    @Transactional
    public void delete(Long id) {
        Lesson lesson = lessonMapper.selectById(id);
        if (lesson == null) throw new BusinessException(404, "课次不存在");
        long attended = attendanceMapper.selectCount(new LambdaQueryWrapper<Attendance>()
                .eq(Attendance::getLessonId, id));
        if (attended > 0) throw new BusinessException(400, "该课次已有点名/消课记录，不能删除；请改为取消");
        lessonMapper.deleteById(id);
    }

    private void validateConflict(Long id, java.time.LocalDate date, LocalTime start, LocalTime end,
                                  Long teacherId, Long classroomId, ClassGroup group) {
        List<Lesson> sameDay = lessonMapper.selectList(new LambdaQueryWrapper<Lesson>()
                .eq(Lesson::getLessonDate, date)
                .ne(Lesson::getStatus, "CANCELLED")
                .ne(id != null, Lesson::getId, id));
        for (Lesson other : sameDay) {
            boolean overlap = start.isBefore(other.getEndTime()) && end.isAfter(other.getStartTime());
            if (!overlap) continue;
            if (group.getId().equals(other.getClassId())) {
                throw new BusinessException(400, "排课冲突：该班级在同一时段已有课次");
            }
            if (teacherId.equals(other.getTeacherId())) {
                throw new BusinessException(400, "排课冲突：该教师在同一时段已有课次（班级 #" + other.getClassId() + "）");
            }
            if (classroomId != null && classroomId.equals(other.getClassroomId())) {
                throw new BusinessException(400, "排课冲突：该教室在同一时段已被占用");
            }
        }
        // 学员冲突：同一学员所在两个班级的课次时间重叠
        List<Long> myStudents = classStudentMapper.selectList(new LambdaQueryWrapper<ClassStudent>()
                        .eq(ClassStudent::getClassId, group.getId()).eq(ClassStudent::getStatus, "ACTIVE"))
                .stream().map(ClassStudent::getStudentId).toList();
        if (!myStudents.isEmpty()) {
            for (Lesson other : sameDay) {
                if (group.getId().equals(other.getClassId())) continue;
                boolean overlap = start.isBefore(other.getEndTime()) && end.isAfter(other.getStartTime());
                if (!overlap) continue;
                long conflictStudents = classStudentMapper.selectCount(new LambdaQueryWrapper<ClassStudent>()
                        .eq(ClassStudent::getClassId, other.getClassId())
                        .eq(ClassStudent::getStatus, "ACTIVE")
                        .in(ClassStudent::getStudentId, myStudents));
                if (conflictStudents > 0) {
                    throw new BusinessException(400, "排课冲突：该班级有 " + conflictStudents
                            + " 名学员在同一时段已有其他课次");
                }
            }
        }
    }

    public Map<String, Object> classStudents(Long classId) {
        ClassGroup group = classGroupMapper.selectById(classId);
        if (group == null) throw new BusinessException(404, "班级不存在");
        List<ClassStudent> links = classStudentMapper.selectList(new LambdaQueryWrapper<ClassStudent>()
                .eq(ClassStudent::getClassId, classId).orderByAsc(ClassStudent::getId));
        List<Map<String, Object>> rows = new ArrayList<>();
        for (ClassStudent link : links) {
            Student student = studentMapper.selectById(link.getStudentId());
            if (student == null) continue;
            HourAccount account = hourAccountMapper.selectOne(new LambdaQueryWrapper<HourAccount>()
                    .eq(HourAccount::getStudentId, student.getId()).last("limit 1"));
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("id", link.getId());
            row.put("studentId", student.getId());
            row.put("studentNo", student.getStudentNo());
            row.put("name", student.getName());
            row.put("gender", student.getGender());
            row.put("school", student.getSchool());
            row.put("grade", student.getGrade());
            row.put("status", link.getStatus());
            row.put("joinedAt", link.getJoinedAt());
            row.put("remainingHours", account == null ? 0 : account.getRemainingHours());
            rows.add(row);
        }
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("classId", group.getId());
        data.put("className", group.getName());
        data.put("capacity", group.getCapacity());
        data.put("count", rows.size());
        data.put("remaining", group.getCapacity() == null ? null : Math.max(0, group.getCapacity() - rows.size()));
        data.put("students", rows);
        return data;
    }

    @Transactional
    public Map<String, Object> addClassStudent(Long classId, Long studentId) {
        ClassGroup group = classGroupMapper.selectById(classId);
        if (group == null) throw new BusinessException(404, "班级不存在");
        Student student = studentMapper.selectById(studentId);
        if (student == null) throw new BusinessException(404, "学员不存在");
        long exists = classStudentMapper.selectCount(new LambdaQueryWrapper<ClassStudent>()
                .eq(ClassStudent::getClassId, classId).eq(ClassStudent::getStudentId, studentId));
        if (exists > 0) throw new BusinessException(400, "该学员已在此班级");
        long count = classStudentMapper.selectCount(new LambdaQueryWrapper<ClassStudent>()
                .eq(ClassStudent::getClassId, classId).eq(ClassStudent::getStatus, "ACTIVE"));
        if (group.getCapacity() != null && count >= group.getCapacity()) {
            throw new BusinessException(400, "班级名额已满");
        }
        ClassStudent link = new ClassStudent();
        link.setClassId(classId);
        link.setStudentId(studentId);
        link.setJoinedAt(java.time.LocalDate.now());
        link.setStatus("ACTIVE");
        classStudentMapper.insert(link);
        return classStudents(classId);
    }

    @Transactional
    public Map<String, Object> removeClassStudent(Long classId, Long linkId) {
        ClassStudent link = classStudentMapper.selectById(linkId);
        if (link == null || !classId.equals(link.getClassId())) {
            throw new BusinessException(404, "班级学员关系不存在");
        }
        classStudentMapper.deleteById(linkId);
        return classStudents(classId);
    }
}
