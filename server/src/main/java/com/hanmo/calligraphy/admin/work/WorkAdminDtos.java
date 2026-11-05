package com.hanmo.calligraphy.admin.work;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

/**
 * 作品管理模块的请求 DTO。
 *
 * <p>上传与授权都要求学员/家长存在；authorized 是前端「已取得授权」的确认位，
 * 服务端仍会以 work_auth 的 ACTIVE 记录为准。</p>
 */
public class WorkAdminDtos {

    /**
     * 作品上传请求：studentId、title、imageUrl 必填；classId 为空时由服务端推断；
     * workType 缺省 HARD，authorized 必须为 true 才允许上传。
     */
    public record WorkCreateRequest(
            @NotNull(message = "请选择学员") Long studentId,
            Long classId,
            @NotBlank(message = "请输入作品标题") String title,
            String workType,
            String contentChar,
            @NotBlank(message = "请上传作品图片") String imageUrl,
            String comment,
            Boolean authorized
    ) {}

    /**
     * 审核请求：status 只接受 DISPLAYED / HIDDEN / REJECTED，由服务端二次校验。
     */
    public record ReviewRequest(@NotBlank(message = "请选择审核结果") String status) {}

    /**
     * 家长作品授权补录请求：studentId、parentUserId 必填；scope/method/authorizedAt 为空时用默认值。
     */
    public record WorkAuthRequest(
            @NotNull(message = "请选择学员") Long studentId,
            @NotNull(message = "请选择家长") Long parentUserId,
            String scope,
            String method,
            LocalDate authorizedAt,
            String evidenceUrl
    ) {}
}
