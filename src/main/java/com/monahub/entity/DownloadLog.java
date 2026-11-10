package com.monahub.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * 下载日志，对应 download_log 表（任务书 F5）。
 * 由 {@link com.monahub.aspect.DownloadLogAspect} 在下载成功后写入。
 */
@Getter
@Setter
public class DownloadLog {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long fileId;
    private Long downloaderId;
    private LocalDateTime downloadTime;
    private String ip;
}
