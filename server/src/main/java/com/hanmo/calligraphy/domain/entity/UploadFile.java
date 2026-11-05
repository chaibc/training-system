package com.hanmo.calligraphy.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.*;
import lombok.Data;

/**
 * 上传文件记录，对应表 upload_file。
 *
 * <p>保存文件元数据与访问地址；stored_name 唯一，实际文件存放在 app.upload.dir 目录。</p>
 */
@Data
@TableName("upload_file")
public class UploadFile {

    @TableId(type = IdType.AUTO)
    private Long id;

    // 原始文件名
    private String originalName;
    // 存储文件名（唯一，UUID + 扩展名）
    private String storedName;
    // 服务器物理路径
    private String filePath;
    // 对外访问地址
    private String url;
    // MIME 类型
    private String mimeType;
    // 文件大小（字节）
    private Long size;
    // 业务类型标记（作品 / 资质 / 机构图片等）
    private String bizType;
    // 上传人（sys_user.id）
    private Long uploadedBy;
    private LocalDateTime createdAt;
}
