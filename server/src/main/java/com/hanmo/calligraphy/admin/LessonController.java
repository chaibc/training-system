package com.hanmo.calligraphy.admin;

import com.hanmo.calligraphy.admin.lesson.LessonAdminDtos.LessonRequest;
import com.hanmo.calligraphy.admin.lesson.LessonAdminService;
import com.hanmo.calligraphy.common.ApiResponse;
import com.hanmo.calligraphy.common.PageResult;
import com.hanmo.calligraphy.domain.entity.Lesson;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "管理端-课次")
@RestController
@RequestMapping("/api/admin/lessons")
@RequiredArgsConstructor
public class LessonController {

    private final LessonAdminService lessonService;

    @Operation(summary = "课次分页")
    @GetMapping("/page")
    public ApiResponse<PageResult<Lesson>> page(@RequestParam(defaultValue = "1") long current,
                                                @RequestParam(defaultValue = "10") long size) {
        return ApiResponse.ok(lessonService.page(current, size));
    }

    @Operation(summary = "课次列表")
    @GetMapping("/list")
    public ApiResponse<List<Lesson>> list() {
        return ApiResponse.ok(lessonService.list());
    }

    @Operation(summary = "课次详情")
    @GetMapping("/{id}")
    public ApiResponse<Lesson> get(@PathVariable Long id) {
        return ApiResponse.ok(lessonService.get(id));
    }

    @Operation(summary = "新建课次（自动校验教师/教室/学员冲突）")
    @PostMapping
    public ApiResponse<Lesson> create(@Valid @RequestBody LessonRequest request) {
        return ApiResponse.ok(lessonService.save(null, request));
    }

    @Operation(summary = "编辑课次（自动校验冲突）")
    @PutMapping("/{id}")
    public ApiResponse<Lesson> update(@PathVariable Long id, @Valid @RequestBody LessonRequest request) {
        return ApiResponse.ok(lessonService.save(id, request));
    }

    @Operation(summary = "删除课次")
    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        lessonService.delete(id);
        return ApiResponse.ok();
    }
}
