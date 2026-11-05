package com.hanmo.calligraphy.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.math.BigDecimal;
import java.time.*;
import lombok.Data;

/**
 * 点名与消课记录，对应表 attendance。
 *
 * <p>一条记录表示某学员在某课次的出勤状态与实际扣减课时；
 * 表上唯一键 (lesson_id, student_id) 保证同一课次同一学员只有一条记录。</p>
 */
@Data
@TableName("attendance")
public class Attendance {

    @TableId(type = IdType.AUTO)
    private Long id;

    // 课次 ID（lesson.id）
    private Long lessonId;
    // 学员 ID（student.id）
    private Long studentId;
    // 出勤状态：PRESENT 出勤 / LEAVE 请假 / ABSENT 缺勤
    private String status;
    // 本次消课课时（单位：课时，DECIMAL(6,1)）
    private BigDecimal consumedHours;
    private String note;
    // 点名操作人（sys_user.id）
    private Long recordedBy;
    // 点名时间
    private LocalDateTime recordedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
