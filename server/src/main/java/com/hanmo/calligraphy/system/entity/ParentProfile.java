package com.hanmo.calligraphy.system.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 家长档案，对应表 parent_profile。
 *
 * <p>与账号（sys_user）一对一（唯一键 uk_parent_user），保存家长姓名、电话与微信昵称。</p>
 */
@Data
@TableName("parent_profile")
public class ParentProfile {

    @TableId(type = IdType.AUTO)
    private Long id;

    // 关联账号（sys_user.id，唯一）
    private Long userId;
    private String name;
    // 联系电话
    private String phone;
    // 微信昵称
    private String wechatNickname;
    // 备注
    private String remark;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
