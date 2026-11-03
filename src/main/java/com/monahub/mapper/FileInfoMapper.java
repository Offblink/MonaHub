package com.monahub.mapper;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.monahub.dto.ExtCount;
import com.monahub.entity.FileInfo;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 文件元信息访问。
 *
 * <p>所有查询都带 uploader_id 与 deleted = 0 条件：列表与下载天然按人隔离，
 * 逻辑删除的记录对用户完全不可见。</p>
 *
 * <p>分页用 MyBatis-Plus 的 {@link IPage} + PaginationInnerInterceptor，
 * 但 SQL 仍然写在 FileInfoMapper.xml 里（不引入 BaseMapper）。</p>
 */
@Mapper
public interface FileInfoMapper {

    int insert(FileInfo file);

    /** 下载/改名/删除前的归属查询：限定 id + uploader_id，越权返回 null。 */
    FileInfo selectByIdAndUploader(@Param("id") Long id,
                                   @Param("uploaderId") Long uploaderId);

    /** 分页列表：按上传人 + 可选分类扩展名集合 + 可选关键字。 */
    List<FileInfo> selectPage(IPage<FileInfo> page,
                              @Param("uploaderId") Long uploaderId,
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

    /** 重命名：只改展示名 origin_name，磁盘上的 UUID 名不动。 */
    int rename(@Param("id") Long id,
               @Param("uploaderId") Long uploaderId,
               @Param("originName") String originName);

    /** 逻辑删除：deleted = 1，记录保留（磁盘文件也保留，便于管理员恢复/清理）。 */
    int softDelete(@Param("id") Long id,
                   @Param("uploaderId") Long uploaderId);
}
