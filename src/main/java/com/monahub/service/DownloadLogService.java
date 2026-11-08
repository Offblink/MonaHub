package com.monahub.service;

import com.monahub.entity.DownloadLog;
import com.monahub.mapper.DownloadLogMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * 下载日志写入（F5）。由 {@link com.monahub.aspect.DownloadLogAspect} 调用，
 * 写失败只降级为告警，绝不影响用户下载。
 */
@Service
public class DownloadLogService {

    private final DownloadLogMapper downloadLogMapper;

    public DownloadLogService(DownloadLogMapper downloadLogMapper) {
        this.downloadLogMapper = downloadLogMapper;
    }

    // 生声明式事务
    @Transactional(rollbackFor = Exception.class)
    public void record(Long fileId, Long downloaderId, String ip) {
        DownloadLog log = new DownloadLog();
        log.setFileId(fileId);
        log.setDownloaderId(downloaderId);
        log.setDownloadTime(LocalDateTime.now());
        log.setIp(ip);
        downloadLogMapper.insert(log); // 直接继承了mp的baseMapper，并复用其中的crud方法
    }
}
