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

@Service
@RequiredArgsConstructor
public class WorkAdminService {

    private final WorkMapper workMapper;
    private final WorkAuthMapper authMapper;
    private final StudentMapper studentMapper;
    private final SysUserMapper userMapper;
    private final ClassGroupMapper classGroupMapper;
    private final ClassStudentMapper classStudentMapper;

    @Transactional
    public Map<String, Object> create(WorkCreateRequest request) {
        Student student = studentMapper.selectById(request.studentId());
        if (student == null) throw new BusinessException(404, "学员不存在");
        if (!Boolean.TRUE.equals(request.authorized())) {
            throw new BusinessException(400, "请先确认已取得家长作品展示授权");
        }
        long authCount = authMapper.selectCount(new LambdaQueryWrapper<WorkAuth>()
                .eq(WorkAuth::getStudentId, student.getId()).eq(WorkAuth::getStatus, "ACTIVE"));
        if (authCount == 0) throw new BusinessException(400, "该学员尚无有效家长授权，请先补录授权");
        Long classId = request.classId();
        Long teacherId = null;
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

    @Transactional
    public Work review(Long id, String status) {
        Work work = workMapper.selectById(id);
        if (work == null) throw new BusinessException(404, "作品不存在");
        if (!List.of("DISPLAYED", "HIDDEN", "REJECTED").contains(status)) {
            throw new BusinessException(400, "审核状态不正确");
        }
        if ("DISPLAYED".equals(status)) {
            if (!Integer.valueOf(1).equals(work.getAuthorized())) {
                throw new BusinessException(400, "缺少家长授权，不能展示");
            }
            long activeAuth = authMapper.selectCount(new LambdaQueryWrapper<WorkAuth>()
                    .eq(WorkAuth::getStudentId, work.getStudentId())
                    .eq(WorkAuth::getStatus, "ACTIVE"));
            if (activeAuth == 0) throw new BusinessException(400, "家长授权已撤回或不存在，不能展示");
        }
        work.setStatus(status);
        workMapper.updateById(work);
        return work;
    }

    @Transactional
    public WorkAuth revokeAuth(Long id) {
        WorkAuth auth = authMapper.selectById(id);
        if (auth == null) throw new BusinessException(404, "授权记录不存在");
        auth.setStatus("REVOKED");
        authMapper.updateById(auth);
        // 撤回授权后，该学员已展示作品立即下架
        workMapper.update(null, new LambdaUpdateWrapper<Work>()
                .eq(Work::getStudentId, auth.getStudentId())
                .eq(Work::getStatus, "DISPLAYED")
                .set(Work::getStatus, "HIDDEN"));
        return auth;
    }

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
