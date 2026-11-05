package com.hanmo.calligraphy.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.hanmo.calligraphy.system.entity.SysUser;
import org.apache.ibatis.annotations.Mapper;

/**
 * 账号（sys_user 表）的 MyBatis-Plus Mapper。
 *
 * <p>仅继承 BaseMapper，提供单表增删改查与分页能力，未定义自定义 SQL。</p>
 */
@Mapper
public interface SysUserMapper extends BaseMapper<SysUser> {
}
