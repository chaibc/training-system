package com.hanmo.calligraphy.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.*;
import lombok.Data;

/**
 * 学员档案，对应表 student。
 *
 * <p>保存学员基础信息、在读状态与主家长账号；student_no 唯一。</p>
 */
@Data
@TableName("student")
public class Student {

    @TableId(type = IdType.AUTO)
    private Long id;

    // 学号（唯一）
    private String studentNo;
    private String name;
    // 性别
    private String gender;
    // 出生日期
    private LocalDate birthDate;
    // 就读学校
    private String school;
    // 年级
    private String grade;
    // 学员状态：ACTIVE 在读 / PAUSED 停课 / GRADUATED 结业 / LEFT 离校
    private String status;
    // 入学日期
    private LocalDate enrollDate;
    // 主家长账号（sys_user.id）
    private Long primaryParentUserId;
    private String remark;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    // 逻辑删除标记：0 正常 / 1 已删除（查询由 @TableLogic 自动过滤）
    @TableLogic
    private Integer deleted;

}
