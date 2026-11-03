package com.monahub.dto;

/**
 * 工作台顶部统计条：文件总数 / 本月新增 / 累计下载。
 */
public class FileStats {

    private final long total;
    private final long monthNew;
    private final long downloads;

    public FileStats(long total, long monthNew, long downloads) {
        this.total = total;
        this.monthNew = monthNew;
        this.downloads = downloads;
    }

    public long getTotal() {
        return total;
    }

    public long getMonthNew() {
        return monthNew;
    }

    public long getDownloads() {
        return downloads;
    }
}
