package com.monahub.mapper;

import com.monahub.entity.SysUser;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 用户查询。SQL 写在 mapper/SysUserMapper.xml。
 */
@Mapper
public interface SysUserMapper {

    SysUser findByUsername(@Param("username") String username);
}
