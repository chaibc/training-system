package com.hanmo.calligraphy.admin.work;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public class WorkAdminDtos {

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

    public record ReviewRequest(@NotBlank(message = "请选择审核结果") String status) {}

    public record WorkAuthRequest(
            @NotNull(message = "请选择学员") Long studentId,
            @NotNull(message = "请选择家长") Long parentUserId,
            String scope,
            String method,
            LocalDate authorizedAt,
            String evidenceUrl
    ) {}
}
