package com.hanmo.calligraphy.admin.lesson;

import com.hanmo.calligraphy.admin.lesson.LessonAdminDtos.AddClassStudentRequest;
import com.hanmo.calligraphy.common.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 班级学员名单接口：查询名单、加人、移人。
 *
 * <p>路由前缀 /api/admin/classes。逻辑全部委托 LessonAdminService，本类只做参数绑定与包装；
 * 不直接访问数据库、不写审计、不开事务。</p>
 * 落库表：class_student、class_group、student、hour_account（经 LessonAdminService 落库）
 */
@Tag(name = "管理端-班级学员")
@RestController
@RequestMapping("/api/admin/classes")
@RequiredArgsConstructor
public class ClassStudentAdminController {

    private final LessonAdminService lessonService;

    /**
     * 班级学员名单（含剩余课时与剩余名额）。
     *
     * <p>只读。</p>
     *
     * @param classId 班级 ID
     * @throws BusinessException 404 班级不存在
     * @return classId/className/capacity/count/remaining/students
     */
    @Operation(summary = "班级学员名单")
    @GetMapping("/{classId}/students")
    public ApiResponse<Map<String, Object>> students(@PathVariable Long classId) {
        return ApiResponse.ok(lessonService.classStudents(classId));
    }

    /**
     * 添加学员到班级。
     *
     * <p>副作用：写 class_student 一行；已满员或重复加入会被 service 拒绝。</p>
     *
     * @param classId 班级 ID
     * @param request 学员 ID（@Valid 校验非空）
     * @throws BusinessException 学员/班级不存在、已在班级、名额已满
     */
    @Operation(summary = "添加班级学员")
    @PostMapping("/{classId}/students")
    public ApiResponse<Map<String, Object>> add(@PathVariable Long classId,
                                                @Valid @RequestBody AddClassStudentRequest request) {
        return ApiResponse.ok(lessonService.addClassStudent(classId, request.studentId()));
    }

    /**
     * 从班级移出学员。
     *
     * <p>副作用：物理删除 class_student 关系；不影响报名与课时。</p>
     *
     * @param classId 班级 ID（校验归属）
     * @param linkId  class_student 主键
     * @throws BusinessException 404 关系不存在或不属于该班级
     * @return 移除后的最新班级名单
     */
    @Operation(summary = "移除班级学员")
    @DeleteMapping("/{classId}/students/{linkId}")
    public ApiResponse<Map<String, Object>> remove(@PathVariable Long classId, @PathVariable Long linkId) {
        return ApiResponse.ok(lessonService.removeClassStudent(classId, linkId));
    }
}
