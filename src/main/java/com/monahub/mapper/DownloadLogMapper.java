package com.monahub.mapper;

import com.monahub.entity.DownloadLog;
import org.apache.ibatis.annotations.Mapper;

/**
 * 下载日志写入（F5）。
 */
@Mapper
public interface DownloadLogMapper {

    int insert(DownloadLog log);
}
