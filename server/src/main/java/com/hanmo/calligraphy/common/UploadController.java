package com.hanmo.calligraphy.common;

import com.hanmo.calligraphy.domain.entity.UploadFile;
import com.hanmo.calligraphy.domain.mapper.UploadFileMapper;
import com.hanmo.calligraphy.security.LoginUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Tag(name = "文件上传")
@RestController
@RequestMapping("/api/upload")
@RequiredArgsConstructor
public class UploadController {

    private static final Set<String> IMAGE_EXT = Set.of("jpg", "jpeg", "png", "webp", "gif");

    private final UploadFileMapper uploadFileMapper;

    @Value("${app.upload.dir:./uploads}")
    private String uploadDir;

    @Value("${app.upload.url-prefix:/uploads}")
    private String urlPrefix;

    @Operation(summary = "上传图片（作品 / 资质 / 机构图片）")
    @PreAuthorize("hasAnyRole('ADMIN','ACADEMIC','TEACHER')")
    @PostMapping
    public ApiResponse<Map<String, Object>> upload(@RequestParam("file") MultipartFile file,
                                                   @RequestParam(required = false) String bizType,
                                                   @AuthenticationPrincipal LoginUser user) throws IOException {
        if (file == null || file.isEmpty()) throw new BusinessException(400, "请选择文件");
        String original = file.getOriginalFilename() == null ? "image" : file.getOriginalFilename();
        String ext = original.contains(".")
                ? original.substring(original.lastIndexOf('.') + 1).toLowerCase(Locale.ROOT) : "";
        if (!IMAGE_EXT.contains(ext)) throw new BusinessException(400, "仅支持 JPG、PNG、WEBP、GIF 图片");
        if (file.getSize() > 10 * 1024 * 1024) throw new BusinessException(400, "图片不能超过 10MB");

        String datePath = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy/MM"));
        String storedName = UUID.randomUUID().toString().replace("-", "") + "." + ext;
        Path base = Paths.get(uploadDir).toAbsolutePath().normalize();
        Path targetDir = base.resolve(datePath).normalize();
        if (!targetDir.startsWith(base)) throw new BusinessException(400, "非法路径");
        Files.createDirectories(targetDir);
        Path target = targetDir.resolve(storedName);
        file.transferTo(target.toFile());

        String url = urlPrefix + "/" + datePath + "/" + storedName;
        UploadFile record = new UploadFile();
        record.setOriginalName(original);
        record.setStoredName(storedName);
        record.setFilePath(target.toString());
        record.setUrl(url);
        record.setMimeType(file.getContentType());
        record.setSize(file.getSize());
        record.setBizType(bizType);
        record.setUploadedBy(user == null ? null : user.getId());
        uploadFileMapper.insert(record);

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("id", record.getId());
        data.put("url", url);
        data.put("name", original);
        data.put("size", file.getSize());
        return ApiResponse.ok(data);
    }
}
