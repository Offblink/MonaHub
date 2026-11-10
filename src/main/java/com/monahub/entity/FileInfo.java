package com.monahub.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.monahub.util.FileCategory;
import com.monahub.util.FileNames;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * 文件元信息，对应 file_info 表。
 *
 * <p>设计要点：</p>
 * <ul>
 *   <li>{@code originName} 只存数据库，用于列表展示与下载时回填文件名；</li>
 *   <li>{@code storeName} 是服务端生成的 UUID 名，只用于磁盘；</li>
 *   <li>{@code filePath} 是相对 upload.root 的路径，绝对根永远来自配置。</li>
 * </ul>
 */
@Getter
@Setter
public class FileInfo {

    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    @TableId(type = IdType.AUTO)
    private Long id;
    private String originName;
    private String storeName;
    private String filePath;
    private String ext;

    /** 内容 MD5，用于"同一账号重复上传同一文件"的去重。 */
    private String md5;

    private Long fileSize;
    private String contentType;
    private Long uploaderId;
    private LocalDateTime createTime;
    /** 1 = 正常，0 = 已冻结（本人与访客都不可下载）。 */
    private Integer status;
    /** 0 = 正常，1 = 已逻辑删除（列表与下载都不可见）。 */
    private Integer deleted;
    private Integer downloadCount;

    // ---------- 以下为页面派生字段，不对应数据库列 ----------

    /** 分类 key：doc / image / archive / other，供筛选胶囊与图标使用。 */
    public String getCategoryKey() {
        FileCategory category = FileCategory.of(ext);
        return category == null ? "other" : category.key();
    }

    /** 分类中文名：文档 / 图片 / 压缩包 / 其他。 */
    public String getCategoryLabel() {
        FileCategory category = FileCategory.of(ext);
        return category == null ? "其他" : category.label();
    }

    /** 人类可读大小，如 1.2 MB。 */
    public String getSizeText() {
        return FileNames.sizeText(fileSize == null ? 0L : fileSize);
    }

    /** 列表里展示的上传时间（去掉秒与 T）。 */
    public String getCreateTimeText() {
        return createTime == null ? "" : createTime.format(TIME_FORMAT);
    }

    public boolean isFrozen() {
        return status != null && status == 0;
    }
}
