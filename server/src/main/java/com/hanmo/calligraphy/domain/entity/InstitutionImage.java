package com.hanmo.calligraphy.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.*;
import lombok.Data;

/**
 * 教学环境图片，对应表 institution_image。
 *
 * <p>用于机构介绍页的图片展示，按 sort 排序。</p>
 */
@Data
@TableName("institution_image")
public class InstitutionImage {

    @TableId(type = IdType.AUTO)
    private Long id;

    // 所属机构（institution_info.id，单机构场景固定为 1）
    private Long institutionId;
    // 图片访问地址
    private String imageUrl;
    // 图片标题 / 说明
    private String title;
    private Integer sort;
    private LocalDateTime createdAt;
}
