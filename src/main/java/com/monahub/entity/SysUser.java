package com.monahub.entity;

import java.time.LocalDateTime;

/**
 * 用户实体，对应 sys_user 表。
 *
 * <p>安全约定：password 与 salt 只在 Service 内部校验时使用，
 * 登录成功后放进 session 的对象必须先脱敏（见 {@link com.monahub.service.UserService#login}）。</p>
 */
public class SysUser {

    private Long id;
    private String username;
    private String password;
    private String salt;
    private String realName;
    private LocalDateTime createTime;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getSalt() {
        return salt;
    }

    public void setSalt(String salt) {
        this.salt = salt;
    }

    public String getRealName() {
        return realName;
    }

    public void setRealName(String realName) {
        this.realName = realName;
    }

    public LocalDateTime getCreateTime() {
        return createTime;
    }

    public void setCreateTime(LocalDateTime createTime) {
        this.createTime = createTime;
    }

    /** 页面显示名：real_name 为空时退回账号。 */
    public String getDisplayName() {
        return (realName == null || realName.isBlank()) ? username : realName;
    }
}
