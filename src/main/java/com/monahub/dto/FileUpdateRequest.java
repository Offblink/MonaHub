package com.monahub.dto;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

/**
 * PUT /api/files 的请求体：按 id 重命名（只改展示名，扩展名规则由 Service 把关）。
 */
public class FileUpdateRequest {

    @NotNull(message = "文件 id 不能为空")
    private Long id;

    @NotBlank(message = "文件名不能为空")
    @Size(max = 255, message = "文件名不能超过 255 个字符")
    private String newName;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getNewName() {
        return newName;
    }

    public void setNewName(String newName) {
        this.newName = newName;
    }
}
