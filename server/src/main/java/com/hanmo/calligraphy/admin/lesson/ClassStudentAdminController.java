package com.hanmo.calligraphy.admin.lesson;

import com.hanmo.calligraphy.admin.lesson.LessonAdminDtos.AddClassStudentRequest;
import com.hanmo.calligraphy.common.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Tag(name = "管理端-班级学员")
@RestController
@RequestMapping("/api/admin/classes")
@RequiredArgsConstructor
public class ClassStudentAdminController {

    private final LessonAdminService lessonService;

    @Operation(summary = "班级学员名单")
    @GetMapping("/{classId}/students")
    public ApiResponse<Map<String, Object>> students(@PathVariable Long classId) {
        return ApiResponse.ok(lessonService.classStudents(classId));
    }

    @Operation(summary = "添加班级学员")
    @PostMapping("/{classId}/students")
    public ApiResponse<Map<String, Object>> add(@PathVariable Long classId,
                                                @Valid @RequestBody AddClassStudentRequest request) {
        return ApiResponse.ok(lessonService.addClassStudent(classId, request.studentId()));
    }

    @Operation(summary = "移除班级学员")
    @DeleteMapping("/{classId}/students/{linkId}")
    public ApiResponse<Map<String, Object>> remove(@PathVariable Long classId, @PathVariable Long linkId) {
        return ApiResponse.ok(lessonService.removeClassStudent(classId, linkId));
    }
}
