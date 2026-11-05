package com.hanmo.calligraphy.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.hanmo.calligraphy.system.entity.SysRolePermission;
import org.apache.ibatis.annotations.Mapper;

/**
 * 角色模块权限（sys_role_permission 表）的 MyBatis-Plus Mapper。
 *
 * <p>仅继承 BaseMapper，提供单表增删改查与分页能力，未定义自定义 SQL。</p>
 */
@Mapper
public interface SysRolePermissionMapper extends BaseMapper<SysRolePermission> {
}
