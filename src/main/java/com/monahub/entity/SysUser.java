package com.monahub.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * 用户实体，对应 sys_user 表。
 *
 * <p>安全约定：password 与 salt 只在 Service 内部校验时使用，
 * 登录成功后放进 session 的对象必须先脱敏（见 {@link com.monahub.service.UserService#login}）。</p>
 */
@Getter
@Setter
public class SysUser {

    @TableId(type = IdType.AUTO)
    private Long id;
    private String username;
    private String password;
    private String salt;
    private String realName;
    private LocalDateTime createTime;

    /** 页面显示名：real_name 为空时退回账号。 */
    public String getDisplayName() {
        return (realName == null || realName.isBlank()) ? username : realName;
    }
}
