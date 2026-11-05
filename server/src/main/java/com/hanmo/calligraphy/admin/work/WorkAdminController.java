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

/**
 * 作品管理接口：教务代上传、作品审核上下架、家长授权补录与撤回。
 *
 * <p>路由不设类级前缀，四个接口分别挂在 /api/admin/works 与 /api/admin/work-auths 下。
 * 逻辑全部在 WorkAdminService；本类不直接访问数据库、不写审计。</p>
 * 落库表：work、work_auth、student、sys_user（经 service 落库）
 */
@Tag(name = "管理端-作品上传与授权补录")
@RestController
@RequiredArgsConstructor
public class WorkAdminController {

    private final WorkAdminService workService;

    /**
     * 上传作品（需已有家长授权）。
     *
     * <p>副作用：写 work，初始 PENDING；未勾选授权或无 ACTIVE 授权会被拒绝。</p>
     *
     * @throws BusinessException 学员不存在、未确认授权、无有效授权
     * @return id/title/status/message
     */
    @Operation(summary = "教务上传作品（需家长授权）")
    @PostMapping("/api/admin/works/upload")
    public ApiResponse<Map<String, Object>> uploadWork(@Valid @RequestBody WorkCreateRequest request) {
        return ApiResponse.ok(workService.create(request));
    }

    /**
     * 作品审核 / 上下架。
     *
     * <p>副作用：更新 work.status；置 DISPLAYED 时再次校验家长授权是否仍为 ACTIVE。</p>
     *
     * @throws BusinessException 作品不存在、状态非法、授权缺失或已撤回
     * @return 更新后的作品
     */
    @Operation(summary = "作品审核/上下架（校验家长授权）")
    @PostMapping("/api/admin/works/{id}/review")
    public ApiResponse<Work> review(@PathVariable Long id, @Valid @RequestBody ReviewRequest request) {
        return ApiResponse.ok(workService.review(id, request.status()));
    }

    /**
     * 撤回家长授权并下架该学员已展示作品。
     *
     * <p>副作用：授权置 REVOKED + 该学员 DISPLAYED 作品批量 HIDDEN，同一事务。</p>
     *
     * @throws BusinessException 授权记录不存在
     * @return 撤回后的授权记录
     */
    @Operation(summary = "撤回家长授权并下架该学员作品")
    @PostMapping("/api/admin/work-auths/{id}/revoke")
    public ApiResponse<WorkAuth> revokeAuth(@PathVariable Long id) {
        return ApiResponse.ok(workService.revokeAuth(id));
    }

    /**
     * 补录/更新家长作品授权。
     *
     * <p>副作用：插入或复用 work_auth 行并置 ACTIVE（含恢复已撤回的授权）。</p>
     *
     * @throws BusinessException 学员不存在、家长账号非法
     * @return 补录后的授权记录
     */
    @Operation(summary = "补录/更新家长作品授权")
    @PostMapping("/api/admin/work-auths/record")
    public ApiResponse<WorkAuth> recordAuth(@Valid @RequestBody WorkAuthRequest request) {
        return ApiResponse.ok(workService.recordAuth(request));
    }
}
