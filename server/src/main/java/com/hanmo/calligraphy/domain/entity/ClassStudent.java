package com.hanmo.calligraphy.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.*;
import lombok.Data;

/**
 * 班级学员关系，对应表 class_student。
 *
 * <p>记录学员加入班级的时间与在班状态；唯一键 (class_id, student_id) 防止重复加入。</p>
 */
@Data
@TableName("class_student")
public class ClassStudent {

    @TableId(type = IdType.AUTO)
    private Long id;

    // 班级 ID（class_group.id）
    private Long classId;
    // 学员 ID（student.id）
    private Long studentId;
    // 入班日期
    private LocalDate joinedAt;
    // 在班状态（默认 ACTIVE）
    private String status;
    private LocalDateTime createdAt;
}
