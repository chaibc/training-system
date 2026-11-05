package com.hanmo.calligraphy.domain.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.hanmo.calligraphy.domain.entity.CourseStage;
import org.apache.ibatis.annotations.Mapper;

/**
 * 课程阶段（course_stage 表）的 MyBatis-Plus Mapper。
 *
 * <p>仅继承 BaseMapper，提供单表增删改查与分页能力，未定义自定义 SQL。</p>
 */
@Mapper
public interface CourseStageMapper extends BaseMapper<CourseStage> {
}
