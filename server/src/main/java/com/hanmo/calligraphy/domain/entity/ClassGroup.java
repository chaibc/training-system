package com.hanmo.calligraphy.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.*;
import lombok.Data;

/**
 * 班级，对应表 class_group。
 *
 * <p>一个班级绑定一门课程、一位任课教师和固定的每周上课时段（weekday + start_time/end_time），
 * 课次（lesson）归属于班级。</p>
 */
@Data
@TableName("class_group")
public class ClassGroup {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String name;
    // 所属课程（course.id）
    private Long courseId;
    // 任课教师（teacher_profile.id）
    private Long teacherId;
    // 默认教室（classroom.id），可为空
    private Long classroomId;
    // 上课星期：1=周一 ... 7=周日
    private Integer weekday;
    // 每周上课开始时间
    private LocalTime startTime;
    // 每周上课结束时间
    private LocalTime endTime;
    // 班级容量（人数上限）
    private Integer capacity;
    // 班级状态：ACTIVE 在读 / FULL 满员 / FINISHED 已结课
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    // 逻辑删除标记：0 正常 / 1 已删除（查询由 @TableLogic 自动过滤）
    @TableLogic
    private Integer deleted;

    /**
     * 班级在读人数（class_student 中 status=ACTIVE 的条数）。
     *
     * <p>不是表字段，由 {@code /api/admin/classes/page} 一次性聚合填充，
     * 避免前端为每个班级单独请求名单来数人数。</p>
     */
    @TableField(exist = false)
    private Integer studentCount;

}
