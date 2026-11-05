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

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 课次与班级学员管理服务：负责课次分页/详情、新增修改、删除，班级名单查询、加人、移人。
 *
 * <p>排课冲突校验覆盖四个维度：班级、任课教师、教室、学员跨班时间重叠（见 validateConflict）。
 * 课次本身不产生课时流水；消课由点名服务负责，本服务只保证课次数据合法。</p>
 *
 * <p>依赖表：lesson、class_group、class_student、classroom、teacher_profile、student、
 * hour_account（名单里展示剩余课时）、attendance（删除前检查是否已点名）。</p>
 */
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

    /**
     * 课次分页，按上课日期倒序、同日按开始时间正序。
     *
     * <p>纯查询，无副作用；每页上限 500。</p>
     *
     * @param current  页码，最小 1
     * @param size     每页条数，限制 1~500
     * @param dateFrom 可选起始上课日期（含），用于周课表按周取数
     * @param dateTo   可选结束上课日期（含）
     * @return 课次分页
     */
    public PageResult<Lesson> page(long current, long size, LocalDate dateFrom, LocalDate dateTo,
                                   Long classId, String status) {
        Page<Lesson> page = new Page<>(Math.max(1, current), Math.min(Math.max(1, size), 500));
        // 日期区间、班级、状态都是可选条件：不传时行为与原实现一致（全量倒序分页）
        lessonMapper.selectPage(page, new LambdaQueryWrapper<Lesson>()
                .ge(dateFrom != null, Lesson::getLessonDate, dateFrom)
                .le(dateTo != null, Lesson::getLessonDate, dateTo)
                .eq(classId != null, Lesson::getClassId, classId)
                .eq(org.springframework.util.StringUtils.hasText(status), Lesson::getStatus, status)
                .orderByDesc(Lesson::getLessonDate).orderByAsc(Lesson::getStartTime));
        return PageResult.of(page.getRecords(), page.getTotal(), page.getCurrent(), page.getSize());
    }

    /**
     * 查询全部课次（不分页），排序同 page。
     *
     * <p>纯查询；课次量大时由前端自行筛选，本方法没有上限保护。</p>
     *
     * @return 课次列表
     */
    public List<Lesson> list() {
        return lessonMapper.selectList(new LambdaQueryWrapper<Lesson>()
                .orderByDesc(Lesson::getLessonDate).orderByAsc(Lesson::getStartTime));
    }

    /**
     * 按 ID 查课次详情。
     *
     * <p>纯查询。</p>
     *
     * @param id 课次 ID
     * @return 课次实体
     * @throws BusinessException 404 课次不存在
     */
    public Lesson get(Long id) {
        Lesson lesson = lessonMapper.selectById(id);
        if (lesson == null) throw new BusinessException(404, "课次不存在");
        return lesson;
    }

    /**
     * 新增或修改课次（id 为空新增，否则修改）。
     *
     * <p>校验链：班级存在 → 结束时间晚于开始时间 → 教师/教室缺省继承班级配置且必须存在 →
     * 同班级课次序号不重复 → 四维排课冲突（validateConflict）。</p>
     *
     * <p>副作用：只写 lesson 一张表（insert 或 updateById），不扣课时、不写流水、不写审计；
     * 事务边界：@Transactional 覆盖校验后的写入。状态缺省 SCHEDULED：新增与编辑都按此兜底，
     * 因此编辑 DONE/CANCELLED 课次时若不传 status，会被重置回 SCHEDULED。</p>
     *
     * @param id      课次 ID，null 表示新增
     * @param request 班级、序号、日期时间、教师/教室（可空则取班级默认）、状态、原课次、备注
     * @return 保存后的课次（含自增 ID）
     * @throws BusinessException 404 班级/课次不存在；400 时间倒挂、教师或教室不存在、序号重复、排课冲突
     */
    @Transactional
    public Lesson save(Long id, LessonRequest request) {
        ClassGroup group = classGroupMapper.selectById(request.classId());
        if (group == null) throw new BusinessException(404, "班级不存在");
        // 时间合法性：结束必须严格晚于开始，跨天课次不在本期模型内
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
        // 同班级课次序号唯一（lesson.uk_class_lesson_no）；编辑时用 ne 排除自身，否则永远重复
        long duplicateNo = lessonMapper.selectCount(new LambdaQueryWrapper<Lesson>()
                .eq(Lesson::getClassId, request.classId())
                .eq(Lesson::getLessonNo, request.lessonNo())
                .ne(id != null, Lesson::getId, id));
        if (duplicateNo > 0) throw new BusinessException(400, "该班级已有相同课次序号");
        // 四维冲突统一入口：班级 / 教师 / 教室 / 学员跨班，细节见 validateConflict
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
        // 状态缺省 SCHEDULED（编辑时同样生效，不传会重置已有状态）；originalLessonId 用于补课指回原课次
        lesson.setStatus(StringUtils.hasText(request.status()) ? request.status() : "SCHEDULED");
        lesson.setOriginalLessonId(request.originalLessonId());
        lesson.setNote(request.note());
        if (id == null) lessonMapper.insert(lesson);
        else lessonMapper.updateById(lesson);
        return lesson;
    }

    /**
     * 删除课次。
     *
     * <p>副作用：物理删除 lesson 一行（该表无 deleted 字段）；已产生 attendance 的课次禁止删除，
     * 防止课时流水与出勤记录失去关联，此时应改为把状态置 CANCELLED。@Transactional 覆盖删除。</p>
     *
     * @param id 课次 ID
     * @throws BusinessException 404 课次不存在；400 已有点名/消课记录，不能删除
     */
    @Transactional
    public void delete(Long id) {
        Lesson lesson = lessonMapper.selectById(id);
        if (lesson == null) throw new BusinessException(404, "课次不存在");
        // 已点名就锁定删除：attendance 与 hour_ledger 都以 lesson_id 关联，删掉会形成孤儿流水
        long attended = attendanceMapper.selectCount(new LambdaQueryWrapper<Attendance>()
                .eq(Attendance::getLessonId, id));
        if (attended > 0) throw new BusinessException(400, "该课次已有点名/消课记录，不能删除；请改为取消");
        lessonMapper.deleteById(id);
    }

    // 排课冲突校验：先按「同一天 + 未取消 + 非自身」缩小候选集，再逐个判时间重叠。
    // 重叠判定用严格不等式（start < other.end && end > other.start），首尾相接（如 10:00-11:00 与 11:00-12:00）不算冲突。
    private void validateConflict(Long id, java.time.LocalDate date, LocalTime start, LocalTime end,
                                  Long teacherId, Long classroomId, ClassGroup group) {
        // 候选集只取同日课次；已取消（CANCELLED）不占用教师/教室/学员，故排除
        List<Lesson> sameDay = lessonMapper.selectList(new LambdaQueryWrapper<Lesson>()
                .eq(Lesson::getLessonDate, date)
                .ne(Lesson::getStatus, "CANCELLED")
                .ne(id != null, Lesson::getId, id));
        for (Lesson other : sameDay) {
            // 时间区间重叠判定：两个半开区间相交才算冲突，相邻不算
            boolean overlap = start.isBefore(other.getEndTime()) && end.isAfter(other.getStartTime());
            if (!overlap) continue;
            // 维度 1：班级冲突——同一班级同一时段不可能上两节课（即便换了教师/教室）
            if (group.getId().equals(other.getClassId())) {
                throw new BusinessException(400, "排课冲突：该班级在同一时段已有课次");
            }
            // 维度 2：教师冲突——同一教师同一时段不能带两个班；报错带出对方班级便于排课调整
            if (teacherId.equals(other.getTeacherId())) {
                throw new BusinessException(400, "排课冲突：该教师在同一时段已有课次（班级 #" + other.getClassId() + "）");
            }
            // 维度 3：教室冲突——教室可为空（线上/户外），为空时不参与比较
            if (classroomId != null && classroomId.equals(other.getClassroomId())) {
                throw new BusinessException(400, "排课冲突：该教室在同一时段已被占用");
            }
        }
        // 学员冲突：同一学员所在两个班级的课次时间重叠
        List<Long> myStudents = classStudentMapper.selectList(new LambdaQueryWrapper<ClassStudent>()
                        .eq(ClassStudent::getClassId, group.getId()).eq(ClassStudent::getStatus, "ACTIVE"))
                .stream().map(ClassStudent::getStudentId).toList();
        // 维度 4：学员跨班冲突——遍历同日其它班级的课次，看两边 ACTIVE 名单是否有交集。
        // 放在最后判，因为需要额外查一次名单，成本最高；同班级课次在上一步已拦掉，这里跳过。
        if (!myStudents.isEmpty()) {
            for (Lesson other : sameDay) {
                if (group.getId().equals(other.getClassId())) continue;
                boolean overlap = start.isBefore(other.getEndTime()) && end.isAfter(other.getStartTime());
                if (!overlap) continue;
                // 用 count 而不是取列表：只需知道「有没有」，同时把冲突人数回显给排课人员
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

    /**
     * 查询班级学员名单（含每人剩余课时），并计算剩余名额。
     *
     * <p>纯查询；逐人查 student 与 hour_account，属于 N+1 查询，班级人数少时可接受。
     * 学员已被逻辑删除时跳过该行，count 按实际展示人数统计。</p>
     *
     * @param classId 班级 ID
     * @return classId/className/capacity/count/remaining/students 组成的 Map
     * @throws BusinessException 404 班级不存在
     */
    public Map<String, Object> classStudents(Long classId) {
        ClassGroup group = classGroupMapper.selectById(classId);
        if (group == null) throw new BusinessException(404, "班级不存在");
        List<ClassStudent> links = classStudentMapper.selectList(new LambdaQueryWrapper<ClassStudent>()
                .eq(ClassStudent::getClassId, classId).orderByAsc(ClassStudent::getId));
        List<Map<String, Object>> rows = new ArrayList<>();
        for (ClassStudent link : links) {
            Student student = studentMapper.selectById(link.getStudentId());
            // 学员档案被逻辑删除后 selectById 返回 null，直接跳过，避免整页 500
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
        // 剩余名额 = 容量 - 实际展示人数；容量未设置时返回 null，不臆造 0
        data.put("remaining", group.getCapacity() == null ? null : Math.max(0, group.getCapacity() - rows.size()));
        data.put("students", rows);
        return data;
    }

    /**
     * 把学员加入班级。
     *
     * <p>副作用：insert class_student 一行（status=ACTIVE、joined_at=今天），不写报名、不充值课时；
     * 校验存在性（唯一键 class_id+student_id）与容量后写入，@Transactional 覆盖。</p>
     *
     * @param classId   班级 ID
     * @param studentId 学员 ID
     * @return 加入后的最新班级名单（结构同 classStudents）
     * @throws BusinessException 404 班级或学员不存在；400 已在班级、名额已满
     */
    @Transactional
    public Map<String, Object> addClassStudent(Long classId, Long studentId) {
        ClassGroup group = classGroupMapper.selectById(classId);
        if (group == null) throw new BusinessException(404, "班级不存在");
        Student student = studentMapper.selectById(studentId);
        if (student == null) throw new BusinessException(404, "学员不存在");
        // 存在性判断不带 status：唯一键约束的是 (class_id, student_id)，重复插入会直接报数据库错误
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

    /**
     * 把学员移出班级。
     *
     * <p>副作用：物理删除 class_student 一行（该表无逻辑删除字段）；不影响报名记录、
     * 课时账户与历史点名。删除后返回最新名单。@Transactional 覆盖。</p>
     *
     * @param classId 班级 ID，用于校验关系归属，防止跨班误删
     * @param linkId  class_student 主键
     * @return 移除后的最新班级名单
     * @throws BusinessException 404 关系不存在或不属于该班级
     */
    @Transactional
    public Map<String, Object> removeClassStudent(Long classId, Long linkId) {
        ClassStudent link = classStudentMapper.selectById(linkId);
        if (link == null || !classId.equals(link.getClassId())) {
            throw new BusinessException(404, "班级学员关系不存在");
        }
        // 物理删除；历史 attendance / hour_ledger 仍保留 student_id，不随名单删除而丢失
        classStudentMapper.deleteById(linkId);
        return classStudents(classId);
    }
}
