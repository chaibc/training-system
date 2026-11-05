package com.hanmo.calligraphy.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.*;
import lombok.Data;

@Data
@TableName("work")
public class Work {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long studentId;
    private Long classId;
    private Long teacherId;
    private String title;
    private String workType;
    private String contentChar;
    private String imageUrl;
    private String comment;
    private Integer authorized;
    private String status;
    private Integer likeCount;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    @TableLogic
    private Integer deleted;

}
