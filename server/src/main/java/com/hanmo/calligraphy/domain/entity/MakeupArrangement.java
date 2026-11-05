package com.hanmo.calligraphy.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.*;
import lombok.Data;

@Data
@TableName("makeup_arrangement")
public class MakeupArrangement {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long leaveRequestId;
    private Long studentId;
    private Long lessonId;
    private String status;
    private Long arrangedBy;
    private LocalDateTime arrangedAt;
    private String note;
    private LocalDateTime createdAt;
}
