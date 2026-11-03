package com.monahub.service;

import com.monahub.entity.SysUser;
import com.monahub.mapper.SysUserMapper;
import com.monahub.util.PasswordUtil;
import org.springframework.stereotype.Service;

/**
 * 登录认证。口令比对只在这里发生，Controller 不接触 password/salt。
 */
@Service
public class UserService {

    private final SysUserMapper sysUserMapper;

    public UserService(SysUserMapper sysUserMapper) {
        this.sysUserMapper = sysUserMapper;
    }

    /**
     * 校验账号口令。
     *
     * @return 成功返回已脱敏的用户（password/salt 置空，可安全放进 session）；失败返回 null
     */
    public SysUser login(String username, String password) {
        if (username == null || username.isBlank() || password == null || password.isEmpty()) {
            return null;
        }
        SysUser user = sysUserMapper.findByUsername(username.trim());
        if (user == null || !PasswordUtil.verify(password, user.getSalt(), user.getPassword())) {
            return null;
        }
        // 放进 session 之前必须脱敏：会话里不保留任何口令材料
        user.setPassword(null);
        user.setSalt(null);
        return user;
    }
}
