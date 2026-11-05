package com.hanmo.calligraphy.system.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("parent_profile")
public class ParentProfile {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;
    private String name;
    private String phone;
    private String wechatNickname;
    private String remark;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
