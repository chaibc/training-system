package com.hanmo.calligraphy.admin.work;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.hanmo.calligraphy.admin.work.WorkAdminDtos.WorkAuthRequest;
import com.hanmo.calligraphy.admin.work.WorkAdminDtos.WorkCreateRequest;
import com.hanmo.calligraphy.common.BusinessException;
import com.hanmo.calligraphy.domain.entity.ClassGroup;
import com.hanmo.calligraphy.domain.entity.ClassStudent;
import com.hanmo.calligraphy.domain.entity.Student;
import com.hanmo.calligraphy.domain.entity.Work;
import com.hanmo.calligraphy.domain.entity.WorkAuth;
import com.hanmo.calligraphy.domain.mapper.ClassGroupMapper;
import com.hanmo.calligraphy.domain.mapper.ClassStudentMapper;
import com.hanmo.calligraphy.domain.mapper.StudentMapper;
import com.hanmo.calligraphy.domain.mapper.WorkAuthMapper;
import com.hanmo.calligraphy.domain.mapper.WorkMapper;
import com.hanmo.calligraphy.system.entity.SysUser;
import com.hanmo.calligraphy.system.mapper.SysUserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.List;
import java.util.Map;

/**
 * 作品管理服务：教务代上传作品、作品审核/上下架、家长作品授权补录与撤回。
 *
 * <p>授权是展示的前置条件：上传时要求 work.authorized=1 且存在 ACTIVE 的 work_auth；
 * 审核为 DISPLAYED 时再查一次 ACTIVE 授权（授权可能在审核前被撤回）；撤回授权会把该学员
 * 所有已展示作品批量改为 HIDDEN。</p>
 *
 * <p>依赖表：work、work_auth、student、sys_user（校验家长账号）、class_group、class_student。
 * 本服务不写 sys_audit_log，作品相关操作没有审计留痕。</p>
 */
@Service
@RequiredArgsConstructor
public class WorkAdminService {

    private final WorkMapper workMapper;
    private final WorkAuthMapper authMapper;
    private final StudentMapper studentMapper;
    private final SysUserMapper userMapper;
    private final ClassGroupMapper classGroupMapper;
    private final ClassStudentMapper classStudentMapper;

    /**
     * 教务代上传作品。
     *
     * <p>校验：学员存在 → 前台已勾选 authorized → 该学员存在 ACTIVE 家长授权。
     * 未指定班级时自动取学员任一 ACTIVE 班级，并从班级回填教师。</p>
     *
     * <p>副作用：写 work 一行（authorized=1、status=PENDING、like_count=0），并调用
     * 事务边界：@Transactional；不写审计、不通知家长。作品要经 review 通过后才展示。</p>
     *
     * @param request 学员、可选班级、标题/图片（必填）、类型、内容字、点评、授权确认
     * @return id/title/status/message 的简单结果
     * @throws BusinessException 404 学员不存在；400 未确认授权或该学员无有效授权
     */
    @Transactional
    public Map<String, Object> create(WorkCreateRequest request) {
        Student student = studentMapper.selectById(request.studentId());
        if (student == null) throw new BusinessException(404, "学员不存在");
        // 前台必须明确勾选「已取得授权」；这只是操作确认，真正依据是下面的 ACTIVE 授权记录
        if (!Boolean.TRUE.equals(request.authorized())) {
            throw new BusinessException(400, "请先确认已取得家长作品展示授权");
        }
        // 授权校验以 work_auth 为准：没有 ACTIVE 记录一律不允许上传
        long authCount = authMapper.selectCount(new LambdaQueryWrapper<WorkAuth>()
                .eq(WorkAuth::getStudentId, student.getId()).eq(WorkAuth::getStatus, "ACTIVE"));
        if (authCount == 0) throw new BusinessException(400, "该学员尚无有效家长授权，请先补录授权");
        Long classId = request.classId();
        Long teacherId = null;
        // 未指定班级时取学员任一 ACTIVE 班级（limit 1，不排序），用于回填 class_id 与 teacher_id
        if (classId == null) {
            List<ClassStudent> links = classStudentMapper.selectList(new LambdaQueryWrapper<ClassStudent>()
                    .eq(ClassStudent::getStudentId, student.getId()).eq(ClassStudent::getStatus, "ACTIVE")
                    .last("limit 1"));
            if (!links.isEmpty()) classId = links.get(0).getClassId();
        }
        if (classId != null) {
            ClassGroup group = classGroupMapper.selectById(classId);
            if (group != null) teacherId = group.getTeacherId();
        }
        Work work = new Work();
        work.setStudentId(student.getId());
        work.setClassId(classId);
        work.setTeacherId(teacherId);
        work.setTitle(request.title().trim());
        work.setWorkType(StringUtils.hasText(request.workType()) ? request.workType() : "HARD");
        work.setContentChar(request.contentChar());
        work.setImageUrl(request.imageUrl());
        work.setComment(request.comment());
        work.setAuthorized(1);
        // 新作品一律 PENDING，必须经 review 才能 DISPLAYED 上墙
        work.setStatus("PENDING");
        work.setLikeCount(0);
        workMapper.insert(work);
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("id", work.getId());
        data.put("title", work.getTitle());
        data.put("status", work.getStatus());
        data.put("message", "作品已提交，审核通过后展示");
        return data;
    }

    /**
     * 作品审核与上下架。
     *
     * <p>只接受 DISPLAYED / HIDDEN / REJECTED。置 DISPLAYED 前做双重校验：
     * work.authorized=1（上传时点）且该学员仍有 ACTIVE 的 work_auth（当前时点）。</p>
     *
     * <p>副作用：更新 work.status，@Transactional 覆盖；不写审计、不发通知。可重复审核，
     * 已展示作品可直接改为 HIDDEN 下架。</p>
     *
     * @param id     作品 ID
     * @param status 目标状态
     * @return 更新后的作品
     * @throws BusinessException 404 作品不存在；400 状态非法、缺授权或授权已撤回
     */
    @Transactional
    public Work review(Long id, String status) {
        Work work = workMapper.selectById(id);
        if (work == null) throw new BusinessException(404, "作品不存在");
        if (!List.of("DISPLAYED", "HIDDEN", "REJECTED").contains(status)) {
            throw new BusinessException(400, "审核状态不正确");
        }
        // 展示前双重校验：authorized=1 只是上传时点，授权可能在这之后被撤回，必须再查 ACTIVE
        if ("DISPLAYED".equals(status)) {
            if (!Integer.valueOf(1).equals(work.getAuthorized())) {
                throw new BusinessException(400, "缺少家长授权，不能展示");
            }
            // 该学员任一有效授权即可展示（授权按家长维度记录，不细分到作品）
            long activeAuth = authMapper.selectCount(new LambdaQueryWrapper<WorkAuth>()
                    .eq(WorkAuth::getStudentId, work.getStudentId())
                    .eq(WorkAuth::getStatus, "ACTIVE"));
            if (activeAuth == 0) throw new BusinessException(400, "家长授权已撤回或不存在，不能展示");
        }
        work.setStatus(status);
        workMapper.updateById(work);
        return work;
    }

    /**
     * 撤回家长作品授权，并联动下架该学员已展示作品。
     *
     * <p>副作用（@Transactional 内，两步）：① work_auth.status 置 REVOKED；
     * ② 该学员所有 status=DISPLAYED 的作品批量置 HIDDEN。不写审计。
     * 注意下架范围是「该学员全部已展示作品」，不区分是否由本次被撤回的家长授权。</p>
     *
     * @param id 授权记录 ID
     * @return 撤回后的授权记录
     * @throws BusinessException 404 授权记录不存在
     */
    @Transactional
    public WorkAuth revokeAuth(Long id) {
        WorkAuth auth = authMapper.selectById(id);
        if (auth == null) throw new BusinessException(404, "授权记录不存在");
        // ① 授权置 REVOKED；随后批量下架该学员所有 DISPLAYED 作品
        auth.setStatus("REVOKED");
        authMapper.updateById(auth);
        // 撤回授权后，该学员已展示作品立即下架
        workMapper.update(null, new LambdaUpdateWrapper<Work>()
                .eq(Work::getStudentId, auth.getStudentId())
                .eq(Work::getStatus, "DISPLAYED")
                .set(Work::getStatus, "HIDDEN"));
        return auth;
    }

    /**
     * 补录或更新家长作品授权。
     *
     * <p>校验学员存在、家长账号存在且 user_type=PARENT。同一 (student, parent) 因唯一键只能有一行，
     * 已存在（含已 REVOKED）时复用并重新置 ACTIVE，等价于「恢复授权」。</p>
     *
     * <p>副作用：insert 或 update work_auth（status=ACTIVE，scope 缺省「作品展示」、
     * method 缺省 ELECTRONIC、authorizedAt 缺省今天），@Transactional 覆盖；不写审计。</p>
     *
     * @param request 学员、家长（必填）、范围、方式、授权日期、凭证 URL
     * @return 补录后的授权记录
     * @throws BusinessException 404 学员不存在；400 家长账号不存在或不是 PARENT
     */
    @Transactional
    public WorkAuth recordAuth(WorkAuthRequest request) {
        Student student = studentMapper.selectById(request.studentId());
        if (student == null) throw new BusinessException(404, "学员不存在");
        SysUser parent = userMapper.selectById(request.parentUserId());
        if (parent == null || !"PARENT".equals(parent.getUserType())) {
            throw new BusinessException(400, "请选择家长账号");
        }
        WorkAuth auth = authMapper.selectOne(new LambdaQueryWrapper<WorkAuth>()
                .eq(WorkAuth::getStudentId, student.getId())
                .eq(WorkAuth::getParentUserId, parent.getId()).last("limit 1"));
        if (auth == null) {
            auth = new WorkAuth();
            auth.setStudentId(student.getId());
            auth.setParentUserId(parent.getId());
            auth.setStatus("ACTIVE");
            auth.setAuthorizedAt(request.authorizedAt() == null ? LocalDate.now() : request.authorizedAt());
            auth.setScope(StringUtils.hasText(request.scope()) ? request.scope() : "作品展示");
            auth.setMethod(StringUtils.hasText(request.method()) ? request.method() : "ELECTRONIC");
            auth.setEvidenceUrl(request.evidenceUrl());
            authMapper.insert(auth);
        } else {
            auth.setStatus("ACTIVE");
            auth.setAuthorizedAt(request.authorizedAt() == null ? LocalDate.now() : request.authorizedAt());
            auth.setScope(StringUtils.hasText(request.scope()) ? request.scope() : "作品展示");
            auth.setMethod(StringUtils.hasText(request.method()) ? request.method() : "ELECTRONIC");
            auth.setEvidenceUrl(request.evidenceUrl());
            authMapper.updateById(auth);
        }
        return auth;
    }
}
