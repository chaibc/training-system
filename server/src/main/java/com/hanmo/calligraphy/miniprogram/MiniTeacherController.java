package com.hanmo.calligraphy.miniprogram;

import com.hanmo.calligraphy.common.ApiResponse;
import com.hanmo.calligraphy.miniprogram.dto.MiniDtos.ApproveLeaveRequest;
import com.hanmo.calligraphy.miniprogram.dto.MiniDtos.RollcallRequest;
import com.hanmo.calligraphy.miniprogram.dto.MiniDtos.WorkCreateRequest;
import com.hanmo.calligraphy.security.LoginUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Tag(name = "小程序-教师端")
@RestController
@RequestMapping("/api/mini/teacher")
@RequiredArgsConstructor
@PreAuthorize("hasRole('TEACHER')")
public class MiniTeacherController {

    private final MiniTeacherService teacherService;

    @Operation(summary = "教师工作台聚合")
    @GetMapping("/overview")
    public ApiResponse<Map<String, Object>> overview(@AuthenticationPrincipal LoginUser user) {
        return ApiResponse.ok(teacherService.home(user));
    }

    @Operation(summary = "某日课程")
    @GetMapping("/lessons")
    public ApiResponse<List<Map<String, Object>>> lessons(
            @AuthenticationPrincipal LoginUser user,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return ApiResponse.ok(teacherService.lessons(user, date));
    }

    @Operation(summary = "本周课程")
    @GetMapping("/lesson-week")
    public ApiResponse<List<Map<String, Object>>> weekLessons(
            @AuthenticationPrincipal LoginUser user,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate week) {
        return ApiResponse.ok(teacherService.weekLessons(user, week));
    }

    @Operation(summary = "课次详情与点名名单")
    @GetMapping("/lessons/{id}")
    public ApiResponse<Map<String, Object>> lessonDetail(@AuthenticationPrincipal LoginUser user,
                                                         @PathVariable Long id) {
        return ApiResponse.ok(teacherService.lessonDetail(user, id));
    }

    @Operation(summary = "点名并自动消课")
    @PostMapping("/lessons/{id}/rollcall")
    public ApiResponse<Map<String, Object>> rollcall(@AuthenticationPrincipal LoginUser user,
                                                     @PathVariable Long id,
                                                     @RequestBody RollcallRequest request) {
        return ApiResponse.ok(teacherService.rollcall(user, id, request));
    }

    @Operation(summary = "待审批请假")
    @GetMapping("/leaves/pending")
    public ApiResponse<List<Map<String, Object>>> pendingLeaves(@AuthenticationPrincipal LoginUser user) {
        return ApiResponse.ok(teacherService.pendingLeaves(user));
    }

    @Operation(summary = "已处理请假")
    @GetMapping("/leaves/history")
    public ApiResponse<List<Map<String, Object>>> leaveHistory(@AuthenticationPrincipal LoginUser user,
                                                               @RequestParam(required = false) String status) {
        return ApiResponse.ok(teacherService.leaveHistory(user, status));
    }

    @Operation(summary = "审批请假")
    @PostMapping("/leaves/{id}/approve")
    public ApiResponse<Map<String, Object>> approve(@AuthenticationPrincipal LoginUser user,
                                                    @PathVariable Long id,
                                                    @RequestBody ApproveLeaveRequest request) {
        return ApiResponse.ok(teacherService.approveLeave(user, id, request));
    }

    @Operation(summary = "上传作品选项")
    @GetMapping("/work-options")
    public ApiResponse<Map<String, Object>> workOptions(@AuthenticationPrincipal LoginUser user) {
        return ApiResponse.ok(teacherService.workOptions(user));
    }

    @Operation(summary = "我上传的作品")
    @GetMapping("/works")
    public ApiResponse<List<Map<String, Object>>> works(@AuthenticationPrincipal LoginUser user) {
        return ApiResponse.ok(teacherService.works(user));
    }

    @Operation(summary = "上传学员作品（提交审核）")
    @PostMapping("/works")
    public ApiResponse<Map<String, Object>> createWork(@AuthenticationPrincipal LoginUser user,
                                                       @Valid @RequestBody WorkCreateRequest request) {
        return ApiResponse.ok(teacherService.createWork(user, request));
    }

    @Operation(summary = "教师账号信息")
    @GetMapping("/account")
    public ApiResponse<Map<String, Object>> account(@AuthenticationPrincipal LoginUser user) {
        return ApiResponse.ok(teacherService.account(user));
    }
}
