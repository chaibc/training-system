package com.hanmo.calligraphy.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.*;
import lombok.Data;

@Data
@TableName("work_auth")
public class WorkAuth {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long studentId;
    private Long parentUserId;
    private String scope;
    private String method;
    private LocalDate authorizedAt;
    private String status;
    private String evidenceUrl;
    private LocalDateTime createdAt;
}
