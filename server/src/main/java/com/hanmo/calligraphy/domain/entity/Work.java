package com.hanmo.calligraphy.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.*;
import lombok.Data;

/**
 * 学员作品，对应表 work。
 *
 * <p>保存作品图片、类型、教师点评与审核 / 展示状态，like_count 为点赞数。</p>
 */
@Data
@TableName("work")
public class Work {

    @TableId(type = IdType.AUTO)
    private Long id;

    // 作者学员（student.id）
    private Long studentId;
    // 所属班级（class_group.id）
    private Long classId;
    // 指导教师（teacher_profile.id）
    private Long teacherId;
    // 作品标题
    private String title;
    // 作品类型：HARD 硬笔 / SOFT 软笔
    private String workType;
    // 作品文字内容
    private String contentChar;
    // 作品图片访问地址
    private String imageUrl;
    // 教师点评
    private String comment;
    // 是否已获家长授权：1=是 / 0=否
    private Integer authorized;
    // 展示状态：PENDING 待审核 / DISPLAYED 已展示 / HIDDEN 已隐藏 / REJECTED 已驳回
    private String status;
    // 点赞数
    private Integer likeCount;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    // 逻辑删除标记：0 正常 / 1 已删除（查询由 @TableLogic 自动过滤）
    @TableLogic
    private Integer deleted;

}
