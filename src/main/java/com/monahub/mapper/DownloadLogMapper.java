package com.monahub.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.monahub.entity.DownloadLog;
import org.apache.ibatis.annotations.Mapper;

/**
 * 下载日志写入（F5）：insert 走 MyBatis-Plus BaseMapper，主键 {@code @TableId AUTO} 回填。
 */
@Mapper
public interface DownloadLogMapper extends BaseMapper<DownloadLog> {
}
