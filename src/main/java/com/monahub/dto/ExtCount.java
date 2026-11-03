package com.monahub.dto;

/**
 * 扩展名计数行（countByExt 的查询结果）。
 */
public class ExtCount {

    private String ext;
    private int cnt;

    public String getExt() {
        return ext;
    }

    public void setExt(String ext) {
        this.ext = ext;
    }

    public int getCnt() {
        return cnt;
    }

    public void setCnt(int cnt) {
        this.cnt = cnt;
    }
}
