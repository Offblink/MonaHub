package com.monahub.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;

import java.time.LocalDateTime;

/**
 * 下载日志，对应 download_log 表（任务书 F5）。
 * 由 {@link com.monahub.aspect.DownloadLogAspect} 在下载成功后写入。
 */
public class DownloadLog {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long fileId;
    private Long downloaderId;
    private LocalDateTime downloadTime;
    private String ip;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getFileId() {
        return fileId;
    }

    public void setFileId(Long fileId) {
        this.fileId = fileId;
    }

    public Long getDownloaderId() {
        return downloaderId;
    }

    public void setDownloaderId(Long downloaderId) {
        this.downloaderId = downloaderId;
    }

    public LocalDateTime getDownloadTime() {
        return downloadTime;
    }

    public void setDownloadTime(LocalDateTime downloadTime) {
        this.downloadTime = downloadTime;
    }

    public String getIp() {
        return ip;
    }

    public void setIp(String ip) {
        this.ip = ip;
    }
}
