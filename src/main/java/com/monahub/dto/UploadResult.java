package com.monahub.dto;

/**
 * 单个文件的上传结果。多文件上传时逐条返回，页面上逐条显示成功/失败原因，
 * 不用一个整体成功/失败掩盖半成功。
 */
public class UploadResult {

    private String name;
    private boolean ok;
    private String message;

    public UploadResult() {
    }

    public UploadResult(String name, boolean ok, String message) {
        this.name = name;
        this.ok = ok;
        this.message = message;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public boolean isOk() {
        return ok;
    }

    public void setOk(boolean ok) {
        this.ok = ok;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
