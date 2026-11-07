package com.monahub.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.monahub.entity.SysUser;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 用户查询。登录查询 SQL 写在 mapper/SysUserMapper.xml；继承 BaseMapper 以备单表操作复用。
 */
@Mapper
public interface SysUserMapper extends BaseMapper<SysUser> {

    SysUser findByUsername(@Param("username") String username);
}
