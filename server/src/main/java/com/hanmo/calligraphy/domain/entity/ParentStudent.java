package com.hanmo.calligraphy.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.*;
import lombok.Data;

@Data
@TableName("parent_student")
public class ParentStudent {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long parentUserId;
    private Long studentId;
    private String relation;
    private Integer isPrimary;
    private String status;
    private String source;
    private Long verifiedBy;
    private LocalDateTime verifiedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
