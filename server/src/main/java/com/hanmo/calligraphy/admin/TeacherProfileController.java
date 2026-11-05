package com.hanmo.calligraphy.admin;

import com.hanmo.calligraphy.admin.teacher.TeacherAdminDtos.TeacherRequest;
import com.hanmo.calligraphy.admin.teacher.TeacherAdminDtos.TeacherView;
import com.hanmo.calligraphy.admin.teacher.TeacherAdminService;
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

import java.util.List;

@Tag(name = "管理端-师资团队")
@RestController
@RequestMapping("/api/admin/teachers")
@RequiredArgsConstructor
public class TeacherProfileController {

    private final TeacherAdminService service;

    @Operation(summary = "教师分页")
    @GetMapping("/page")
    public ApiResponse<PageResult<TeacherView>> page(
            @RequestParam(defaultValue = "1") long current,
            @RequestParam(defaultValue = "10") long size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String status) {
        return ApiResponse.ok(service.page(current, size, keyword, status));
    }

    @Operation(summary = "教师列表")
    @GetMapping("/list")
    public ApiResponse<List<TeacherView>> list() {
        return ApiResponse.ok(service.list());
    }

    @Operation(summary = "新增教师（同时创建教师账号）")
    @PostMapping
    public ApiResponse<TeacherView> create(@Valid @RequestBody TeacherRequest request,
                                           @AuthenticationPrincipal LoginUser operator) {
        return ApiResponse.ok(service.create(request, operator));
    }

    @Operation(summary = "编辑教师")
    @PutMapping("/{id}")
    public ApiResponse<TeacherView> update(@PathVariable Long id,
                                           @Valid @RequestBody TeacherRequest request,
                                           @AuthenticationPrincipal LoginUser operator) {
        return ApiResponse.ok(service.update(id, request, operator));
    }

    @Operation(summary = "启用/停用教师")
    @PostMapping("/{id}/toggle-status")
    public ApiResponse<TeacherView> toggle(@PathVariable Long id,
                                           @AuthenticationPrincipal LoginUser operator) {
        return ApiResponse.ok(service.toggle(id, operator));
    }

    @Operation(summary = "解绑教师微信")
    @PostMapping("/{id}/unbind-wechat")
    public ApiResponse<TeacherView> unbind(@PathVariable Long id,
                                           @AuthenticationPrincipal LoginUser operator) {
        return ApiResponse.ok(service.unbind(id, operator));
    }
}
