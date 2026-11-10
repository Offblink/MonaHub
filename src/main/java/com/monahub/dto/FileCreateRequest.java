package com.monahub.dto;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

import lombok.Getter;
import lombok.Setter;

/**
 * POST /api/files 的请求体：文件名 + Base64 内容。
 *
 * <p>content 的长度上限与解码后的字节上限在 Service 侧校验
 * （解码前按 4/3 膨胀率预估，避免超大 body 撑爆内存）。</p>
 */
@Getter
@Setter
public class FileCreateRequest {

    @NotBlank(message = "文件名不能为空")
    @Size(max = 255, message = "文件名不能超过 255 个字符")
    private String name;

    @NotBlank(message = "文件内容不能为空")
    private String content;
}
