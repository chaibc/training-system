package com.hanmo.calligraphy.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.*;
import lombok.Data;

@Data
@TableName("teacher_profile")
public class TeacherProfile {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;
    private String teacherNo;
    private String name;
    private String title;
    private Integer years;
    private String styles;
    private String coursesText;
    private String bio;
    private String quote;
    private String tags;
    private String avatar;
    private String status;
    private Integer sort;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    @TableLogic
    private Integer deleted;

}
