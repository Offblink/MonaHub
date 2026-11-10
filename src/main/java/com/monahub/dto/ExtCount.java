package com.monahub.dto;

import lombok.Getter;
import lombok.Setter;

/**
 * 扩展名计数行（countByExt 的查询结果）。
 */
@Getter
@Setter
public class ExtCount {

    private String ext;
    private int cnt;
}
