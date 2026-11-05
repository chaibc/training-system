package com.hanmo.calligraphy.admin.work;

import com.hanmo.calligraphy.admin.work.WorkAdminDtos.ReviewRequest;
import com.hanmo.calligraphy.admin.work.WorkAdminDtos.WorkAuthRequest;
import com.hanmo.calligraphy.admin.work.WorkAdminDtos.WorkCreateRequest;
import com.hanmo.calligraphy.common.ApiResponse;
import com.hanmo.calligraphy.domain.entity.Work;
import com.hanmo.calligraphy.domain.entity.WorkAuth;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Tag(name = "管理端-作品上传与授权补录")
@RestController
@RequiredArgsConstructor
public class WorkAdminController {

    private final WorkAdminService workService;

    @Operation(summary = "教务上传作品（需家长授权）")
    @PostMapping("/api/admin/works/upload")
    public ApiResponse<Map<String, Object>> uploadWork(@Valid @RequestBody WorkCreateRequest request) {
        return ApiResponse.ok(workService.create(request));
    }

    @Operation(summary = "作品审核/上下架（校验家长授权）")
    @PostMapping("/api/admin/works/{id}/review")
    public ApiResponse<Work> review(@PathVariable Long id, @Valid @RequestBody ReviewRequest request) {
        return ApiResponse.ok(workService.review(id, request.status()));
    }

    @Operation(summary = "撤回家长授权并下架该学员作品")
    @PostMapping("/api/admin/work-auths/{id}/revoke")
    public ApiResponse<WorkAuth> revokeAuth(@PathVariable Long id) {
        return ApiResponse.ok(workService.revokeAuth(id));
    }

    @Operation(summary = "补录/更新家长作品授权")
    @PostMapping("/api/admin/work-auths/record")
    public ApiResponse<WorkAuth> recordAuth(@Valid @RequestBody WorkAuthRequest request) {
        return ApiResponse.ok(workService.recordAuth(request));
    }
}
