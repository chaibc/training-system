package com.hanmo.calligraphy.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.*;
import lombok.Data;

@Data
@TableName("institution_image")
public class InstitutionImage {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long institutionId;
    private String imageUrl;
    private String title;
    private Integer sort;
    private LocalDateTime createdAt;
}
