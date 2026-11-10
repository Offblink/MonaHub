package com.monahub.dto;

import lombok.Getter;
import lombok.Setter;

/**
 * 单个文件的上传结果。多文件上传时逐条返回，页面上逐条显示成功/失败原因，
 * 不用一个整体成功/失败掩盖半成功。
 */
@Getter
@Setter
public class UploadResult {

    private String name;
    private boolean ok;
    /** true = 内容 MD5 与已有记录相同，本次没有落盘也没有入库。 */
    private boolean duplicate;
    private String message;

    public UploadResult() {
    }

    public UploadResult(String name, boolean ok, String message) {
        this(name, ok, false, message);
    }

    public UploadResult(String name, boolean ok, boolean duplicate, String message) {
        this.name = name;
        this.ok = ok;
        this.duplicate = duplicate;
        this.message = message;
    }

    /** 重复上传：跳过落盘/入库，只回一条提示。 */
    public static UploadResult duplicate(String name, String existingName) {
        return new UploadResult(name, false, true,
                "与已提交的「" + existingName + "」内容相同，已跳过重复上传");
    }
}
