package com.hanmo.calligraphy.domain.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.hanmo.calligraphy.domain.entity.MakeupArrangement;
import org.apache.ibatis.annotations.Mapper;

/**
 * 补课安排（makeup_arrangement 表）的 MyBatis-Plus Mapper。
 *
 * <p>仅继承 BaseMapper，提供单表增删改查与分页能力，未定义自定义 SQL。</p>
 */
@Mapper
public interface MakeupArrangementMapper extends BaseMapper<MakeupArrangement> {
}
