package com.hanmo.calligraphy.admin.student;

import com.hanmo.calligraphy.admin.student.StudentAdminDtos.AdjustHoursRequest;
import com.hanmo.calligraphy.admin.student.StudentAdminDtos.CreateStudentRequest;
import com.hanmo.calligraphy.admin.student.StudentAdminDtos.StudentDetail;
import com.hanmo.calligraphy.admin.student.StudentAdminDtos.StudentOption;
import com.hanmo.calligraphy.admin.student.StudentAdminDtos.StudentRow;
import com.hanmo.calligraphy.admin.student.StudentAdminDtos.UpdateStudentRequest;
import com.hanmo.calligraphy.common.ApiResponse;
import com.hanmo.calligraphy.common.PageResult;
import com.hanmo.calligraphy.security.LoginUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.List;

@Tag(name = "管理端-学员管理")
@RestController
@RequestMapping("/api/admin/students")
@RequiredArgsConstructor
public class StudentAdminController {

    private final StudentAdminService service;

    @Operation(summary = "学员分页")
    @GetMapping("/page")
    public ApiResponse<PageResult<StudentRow>> page(
            @RequestParam(defaultValue = "1") long current,
            @RequestParam(defaultValue = "10") long size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Long classId) {
        return ApiResponse.ok(service.page(current, size, keyword, status, classId));
    }

    @Operation(summary = "学员档案详情")
    @GetMapping("/{id}")
    public ApiResponse<StudentDetail> detail(@PathVariable Long id) {
        return ApiResponse.ok(service.detail(id));
    }

    @Operation(summary = "新建学员")
    @PostMapping
    public ApiResponse<StudentRow> create(@Valid @RequestBody CreateStudentRequest request,
                                          @AuthenticationPrincipal LoginUser operator) {
        return ApiResponse.ok(service.create(request, operator));
    }

    @Operation(summary = "编辑学员")
    @PutMapping("/{id}")
    public ApiResponse<StudentRow> update(@PathVariable Long id,
                                          @RequestBody UpdateStudentRequest request,
                                          @AuthenticationPrincipal LoginUser operator) {
        return ApiResponse.ok(service.update(id, request, operator));
    }

    @Operation(summary = "调整课时")
    @PostMapping("/{id}/adjust-hours")
    public ApiResponse<BigDecimal> adjustHours(@PathVariable Long id,
                                               @Valid @RequestBody AdjustHoursRequest request,
                                               @AuthenticationPrincipal LoginUser operator) {
        return ApiResponse.ok(service.adjustHours(id, request, operator));
    }

    @Operation(summary = "学员下拉")
    @GetMapping("/options")
    public ApiResponse<List<StudentOption>> options() {
        return ApiResponse.ok(service.options());
    }
}
