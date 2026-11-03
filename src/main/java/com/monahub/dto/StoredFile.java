package com.monahub.dto;

/**
 * 落盘结果：StorageService 的返回值，只描述"磁盘上发生了什么"，
 * 入库由 FileService 负责，两者失败可以分别补偿。
 */
public class StoredFile {

    private final String originName;
    private final String storeName;
    private final String relativePath;
    private final String ext;
    private final long size;
    private final String contentType;

    public StoredFile(String originName, String storeName, String relativePath,
                      String ext, long size, String contentType) {
        this.originName = originName;
        this.storeName = storeName;
        this.relativePath = relativePath;
        this.ext = ext;
        this.size = size;
        this.contentType = contentType;
    }

    public String getOriginName() {
        return originName;
    }

    public String getStoreName() {
        return storeName;
    }

    public String getRelativePath() {
        return relativePath;
    }

    public String getExt() {
        return ext;
    }

    public long getSize() {
        return size;
    }

    public String getContentType() {
        return contentType;
    }
}
