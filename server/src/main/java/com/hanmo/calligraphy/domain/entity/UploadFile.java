package com.hanmo.calligraphy.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.*;
import lombok.Data;

@Data
@TableName("upload_file")
public class UploadFile {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String originalName;
    private String storedName;
    private String filePath;
    private String url;
    private String mimeType;
    private Long size;
    private String bizType;
    private Long uploadedBy;
    private LocalDateTime createdAt;
}
