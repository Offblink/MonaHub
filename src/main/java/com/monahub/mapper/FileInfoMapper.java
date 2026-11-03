package com.monahub.mapper;

import com.monahub.dto.ExtCount;
import com.monahub.entity.FileInfo;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 文件元信息访问。
 *
 * <p>所有查询都带 uploader_id 条件：列表与下载天然按人隔离，
 * 不存在"先查全表再在 Java 里筛"的写法。</p>
 */
@Mapper
public interface FileInfoMapper {

    int insert(FileInfo file);

    /** 下载专用：同时限定 id 与上传人，越权时直接返回 null。 */
    FileInfo selectByIdAndUploader(@Param("id") Long id,
                                   @Param("uploaderId") Long uploaderId);

    /** 列表：按上传人 + 可选分类扩展名集合 + 可选关键字。 */
    List<FileInfo> selectByUploader(@Param("uploaderId") Long uploaderId,
                                    @Param("exts") List<String> exts,
                                    @Param("keyword") String keyword);

    /** 分类计数：一次分组查询，避免按类循环查库。 */
    List<ExtCount> countByExt(@Param("uploaderId") Long uploaderId);

    long countByUploader(@Param("uploaderId") Long uploaderId);

    long countByUploaderSince(@Param("uploaderId") Long uploaderId,
                              @Param("since") LocalDateTime since);

    long sumDownloadCount(@Param("uploaderId") Long uploaderId);

    int updateStatus(@Param("id") Long id,
                     @Param("uploaderId") Long uploaderId,
                     @Param("status") Integer status);

    int incrementDownloadCount(@Param("id") Long id);
}
