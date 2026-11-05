package com.hanmo.calligraphy.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.*;
import com.baomidou.mybatisplus.annotation.TableLogic;
import lombok.Data;

/**
 * 机构公告，对应表 notice。
 *
 * <p>按类型与发布状态展示在管理后台 / 小程序。</p>
 */
@Data
@TableName("notice")
public class Notice {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String title;
    // 公告类型：NOTICE 通知 / ADMISSION 招生 / HOLIDAY 放假
    private String type;
    private String content;
    // 发布日期
    private LocalDate publishDate;
    // 状态：DRAFT 草稿 / PUBLISHED 已发布 / HIDDEN 已隐藏
    private String status;
    // 发布人（sys_user.id）
    private Long createdBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // 逻辑删除标记：0 正常 / 1 已删除（查询由 @TableLogic 自动过滤）
    @TableLogic
    private Integer deleted;
}
