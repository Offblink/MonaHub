package com.monahub.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
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
 * <p>MyBatis-Plus：继承 {@link BaseMapper} —— insert 走 MP（{@code @TableId AUTO} 主键回填），
 * 归属单行读由 Service 侧用条件构造器 {@code selectOne} 完成；
 * 分页动态条件、MD5 去重、聚合统计与带归属条件的更新仍写在 FileInfoMapper.xml
 * （任务书「XML 映射」知识点；分页 SQL 不写 LIMIT，交给 PaginationInnerInterceptor）。
 * XML 语句 id 不叫 insert/selectPage：与 BaseMapper 注入的方法同名会在启动期撞 Mapped Statements。</p>
 */
@Mapper
public interface FileInfoMapper extends BaseMapper<FileInfo> {

    /** 重复上传检查：同一账号下、未删除记录里内容 MD5 相同的第一条。 */
    FileInfo selectDuplicate(@Param("uploaderId") Long uploaderId,
                             @Param("md5") String md5);

    /** 分页列表：按上传人 + 可选分类扩展名集合 + 可选关键字。 */
    List<FileInfo> selectPageQuery(IPage<FileInfo> page,
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
