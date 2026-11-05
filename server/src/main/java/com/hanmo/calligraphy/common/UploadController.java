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

/**
 * 文件上传接口。
 *
 * <p>只允许管理员 / 教务 / 教师上传图片；文件落盘到 app.upload.dir，按 yyyy/MM 分目录，
 * 并把元数据写入 upload_file 表。上传目录由 WebMvcConfig 映射为 app.upload.url-prefix 静态资源。</p>
 */
@Tag(name = "文件上传")
@RestController
@RequestMapping("/api/upload")
@RequiredArgsConstructor
public class UploadController {

    // 允许上传的图片扩展名白名单
    private static final Set<String> IMAGE_EXT = Set.of("jpg", "jpeg", "png", "webp", "gif");

    // 上传记录落库
    private final UploadFileMapper uploadFileMapper;

    // 本地上传根目录（app.upload.dir，默认 ./uploads）
    @Value("${app.upload.dir:./uploads}")
    private String uploadDir;

    // 对外访问前缀（app.upload.url-prefix，默认 /uploads）
    @Value("${app.upload.url-prefix:/uploads}")
    private String urlPrefix;

    /**
     * 上传图片并登记文件记录。
     *
     * <p>校验扩展名白名单与 10MB 大小上限，按 yyyy/MM 分目录落盘后写入 upload_file。</p>
     *
     * <p>副作用：写磁盘文件 + 插入 upload_file 一行。</p>
     *
     * @param file    上传的图片
     * @param bizType 业务类型标记（作品 / 资质 / 机构图片），可为空
     * @param user    当前登录用户，用于记录上传人
     * @return id、url、原始文件名与大小
     * @throws BusinessException 未选择文件、类型不支持、超过 10MB 或路径非法（400）
     * @throws IOException       磁盘写入失败
     */
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
        // 扩展名与大小都校验，避免把非图片文件或超大文件写入上传目录
        if (!IMAGE_EXT.contains(ext)) throw new BusinessException(400, "仅支持 JPG、PNG、WEBP、GIF 图片");
        if (file.getSize() > 10 * 1024 * 1024) throw new BusinessException(400, "图片不能超过 10MB");
        // 扩展名可伪造，再按文件头 magic bytes 校验一次真实类型
        if (!looksLikeImage(file)) throw new BusinessException(400, "文件内容不是有效的图片");

        String datePath = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy/MM"));
        String storedName = UUID.randomUUID().toString().replace("-", "") + "." + ext;
        Path base = Paths.get(uploadDir).toAbsolutePath().normalize();
        Path targetDir = base.resolve(datePath).normalize();
        // 归一化后必须仍在上传根目录内，防止 ../ 目录穿越
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

    /**
     * 按文件头判断是否为允许的图片类型（防御伪造扩展名）。
     *
     * <p>JPG: {@code FF D8 FF}；PNG: {@code 89 50 4E 47}；GIF: {@code 47 49 46 38}；
     * WEBP: {@code RIFF....WEBP}。只读前 12 字节，不影响后续落盘。</p>
     *
     * @param file 上传文件
     * @return true 表示头部符合任一允许格式
     */
    private boolean looksLikeImage(MultipartFile file) {
        try (java.io.InputStream in = file.getInputStream()) {
            byte[] head = in.readNBytes(12);
            if (head.length < 12) return false;
            // JPEG
            if ((head[0] & 0xFF) == 0xFF && (head[1] & 0xFF) == 0xD8 && (head[2] & 0xFF) == 0xFF) return true;
            // PNG
            if ((head[0] & 0xFF) == 0x89 && head[1] == 'P' && head[2] == 'N' && head[3] == 'G') return true;
            // GIF87a / GIF89a
            if (head[0] == 'G' && head[1] == 'I' && head[2] == 'F' && head[3] == '8') return true;
            // WEBP：RIFF + 4 字节长度 + WEBP
            return head[0] == 'R' && head[1] == 'I' && head[2] == 'F' && head[3] == 'F'
                    && head[8] == 'W' && head[9] == 'E' && head[10] == 'B' && head[11] == 'P';
        } catch (IOException e) {
            return false;
        }
    }
}
