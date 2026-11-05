package com.hanmo.calligraphy.miniprogram;

import com.hanmo.calligraphy.common.ApiResponse;
import com.hanmo.calligraphy.miniprogram.dto.MiniDtos.BindingRequest;
import com.hanmo.calligraphy.miniprogram.dto.MiniDtos.ChildProfileRequest;
import com.hanmo.calligraphy.miniprogram.dto.MiniDtos.CommentRequest;
import com.hanmo.calligraphy.miniprogram.dto.MiniDtos.EnrollmentCreateRequest;
import com.hanmo.calligraphy.miniprogram.dto.MiniDtos.LeaveCreateRequest;
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

@Tag(name = "小程序-家长端")
@RestController
@RequestMapping("/api/mini/parent")
@RequiredArgsConstructor
@PreAuthorize("hasRole('PARENT')")
public class MiniParentController {

    private final MiniParentService parentService;

    @Operation(summary = "家长工作台聚合")
    @GetMapping("/overview")
    public ApiResponse<Map<String, Object>> overview(@AuthenticationPrincipal LoginUser user) {
        return ApiResponse.ok(parentService.overview(user));
    }

    @Operation(summary = "已绑定子女列表")
    @GetMapping("/children")
    public ApiResponse<List<Map<String, Object>>> children(@AuthenticationPrincipal LoginUser user) {
        return ApiResponse.ok(parentService.children(user.getId()));
    }

    @Operation(summary = "学员档案详情")
    @GetMapping("/children/{studentId}")
    public ApiResponse<Map<String, Object>> childDetail(@AuthenticationPrincipal LoginUser user,
                                                        @PathVariable Long studentId) {
        return ApiResponse.ok(parentService.childDetail(user, studentId));
    }

    @Operation(summary = "修改学员基础资料")
    @PutMapping("/children/{studentId}")
    public ApiResponse<Map<String, Object>> updateChild(@AuthenticationPrincipal LoginUser user,
                                                        @PathVariable Long studentId,
                                                        @RequestBody ChildProfileRequest request) {
        return ApiResponse.ok(parentService.updateChild(user, studentId, request));
    }

    @Operation(summary = "我的绑定关系")
    @GetMapping("/bindings")
    public ApiResponse<List<Map<String, Object>>> bindings(@AuthenticationPrincipal LoginUser user) {
        return ApiResponse.ok(parentService.bindings(user));
    }

    @Operation(summary = "提交学员绑定申请")
    @PostMapping("/bindings")
    public ApiResponse<Map<String, Object>> createBinding(@AuthenticationPrincipal LoginUser user,
                                                          @RequestBody BindingRequest request) {
        return ApiResponse.ok(parentService.createBinding(user, request));
    }

    @Operation(summary = "申请解绑学员（需教务审核）")
    @PostMapping("/bindings/{id}/unbind")
    public ApiResponse<Map<String, Object>> unbind(@AuthenticationPrincipal LoginUser user,
                                                   @PathVariable Long id) {
        return ApiResponse.ok(parentService.requestUnbind(user, id));
    }

    @Operation(summary = "我的课表")
    @GetMapping("/schedule")
    public ApiResponse<Map<String, Object>> schedule(@AuthenticationPrincipal LoginUser user,
                                                     @RequestParam Long studentId,
                                                     @RequestParam(required = false)
                                                     @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate week) {
        return ApiResponse.ok(parentService.schedule(user, studentId, week));
    }

    @Operation(summary = "课次详情")
    @GetMapping("/lessons/{lessonId}")
    public ApiResponse<Map<String, Object>> lessonDetail(@AuthenticationPrincipal LoginUser user,
                                                         @PathVariable Long lessonId,
                                                         @RequestParam Long studentId) {
        return ApiResponse.ok(parentService.lessonDetail(user, studentId, lessonId));
    }

    @Operation(summary = "请假申请选项")
    @GetMapping("/leave-options")
    public ApiResponse<Map<String, Object>> leaveOptions(@AuthenticationPrincipal LoginUser user,
                                                         @RequestParam Long studentId) {
        return ApiResponse.ok(parentService.leaveOptions(user, studentId));
    }

    @Operation(summary = "提交请假申请")
    @PostMapping("/leaves")
    public ApiResponse<Map<String, Object>> createLeave(@AuthenticationPrincipal LoginUser user,
                                                        @Valid @RequestBody LeaveCreateRequest request) {
        return ApiResponse.ok(parentService.createLeave(user, request));
    }

    @Operation(summary = "请假记录")
    @GetMapping("/leave-records")
    public ApiResponse<List<Map<String, Object>>> leaveRecords(@AuthenticationPrincipal LoginUser user,
                                                               @RequestParam Long studentId,
                                                               @RequestParam(required = false) String status) {
        return ApiResponse.ok(parentService.leaveRecords(user, studentId, status));
    }

    @Operation(summary = "申请补课")
    @PostMapping("/leaves/{id}/makeup")
    public ApiResponse<Map<String, Object>> makeup(@AuthenticationPrincipal LoginUser user,
                                                   @PathVariable Long id) {
        return ApiResponse.ok(parentService.applyMakeup(user, id));
    }

    @Operation(summary = "报名选项（课程与班级）")
    @GetMapping("/enroll/options")
    public ApiResponse<Map<String, Object>> enrollmentOptions(@AuthenticationPrincipal LoginUser user,
                                                              @RequestParam Long studentId) {
        return ApiResponse.ok(parentService.enrollmentOptions(user, studentId));
    }

    @Operation(summary = "报名记录")
    @GetMapping("/enrollments")
    public ApiResponse<List<Map<String, Object>>> enrollments(@AuthenticationPrincipal LoginUser user,
                                                              @RequestParam Long studentId) {
        return ApiResponse.ok(parentService.enrollments(user, studentId));
    }

    @Operation(summary = "提交报名申请")
    @PostMapping("/enrollments")
    public ApiResponse<Map<String, Object>> createEnrollment(@AuthenticationPrincipal LoginUser user,
                                                             @Valid @RequestBody EnrollmentCreateRequest request) {
        return ApiResponse.ok(parentService.createEnrollment(user, request));
    }

    @Operation(summary = "家长取消未确认报名")
    @PostMapping("/enrollments/{id}/cancel")
    public ApiResponse<Map<String, Object>> cancelEnrollment(@AuthenticationPrincipal LoginUser user,
                                                             @PathVariable Long id) {
        return ApiResponse.ok(parentService.cancelEnrollment(user, id));
    }

    @Operation(summary = "课时账户与流水")
    @GetMapping("/hours")
    public ApiResponse<Map<String, Object>> hours(@AuthenticationPrincipal LoginUser user,
                                                  @RequestParam Long studentId) {
        return ApiResponse.ok(parentService.hours(user, studentId));
    }

    @Operation(summary = "上课记录")
    @GetMapping("/attendance")
    public ApiResponse<Map<String, Object>> attendance(@AuthenticationPrincipal LoginUser user,
                                                       @RequestParam Long studentId,
                                                       @RequestParam(required = false) String status) {
        return ApiResponse.ok(parentService.attendance(user, studentId, status));
    }

    @Operation(summary = "成长相册")
    @GetMapping("/growth")
    public ApiResponse<Map<String, Object>> growth(@AuthenticationPrincipal LoginUser user,
                                                   @RequestParam Long studentId) {
        return ApiResponse.ok(parentService.growth(user, studentId));
    }

    @Operation(summary = "账号与绑定信息")
    @GetMapping("/account")
    public ApiResponse<Map<String, Object>> account(@AuthenticationPrincipal LoginUser user) {
        return ApiResponse.ok(parentService.account(user));
    }

    @Operation(summary = "作品留言（审核后展示）")
    @PostMapping("/works/{workId}/comments")
    public ApiResponse<Map<String, Object>> comment(@AuthenticationPrincipal LoginUser user,
                                                    @PathVariable Long workId,
                                                    @Valid @RequestBody CommentRequest request) {
        return ApiResponse.ok(parentService.comment(user, workId, request.content()));
    }
}
