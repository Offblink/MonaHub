package com.monahub.util;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * 文件分类：扩展名 → 文档 / 图片 / 压缩包。
 *
 * <p>白名单本身由 upload.allowed-ext 配置决定；本枚举只负责"分类展示与筛选"，
 * 两者都改了才一致（README 已注明扩展白名单时的注意事项）。</p>
 */
public enum FileCategory {

    // 枚举
    DOC("doc", "文档", "pdf", "doc", "docx"),
    IMAGE("image", "图片", "jpg", "jpeg", "png"),
    ARCHIVE("archive", "压缩包", "zip");

    private final String key;
    private final String label;
    private final Set<String> extensions;

    FileCategory(String key, String label, String... extensions) {
        this.key = key;
        this.label = label;
        this.extensions = Collections.unmodifiableSet(
                new LinkedHashSet<>(Arrays.asList(extensions)));
    }

    public String key() {
        return key;
    }

    public String label() {
        return label;
    }

    public Set<String> extensions() {
        return extensions;
    }

    /** 扩展名归类；未知扩展名返回 null（页面显示"其他"）。 */
    public static FileCategory of(String ext) {
        if (ext == null) {
            return null;
        }
        String normalized = ext.toLowerCase();
        for (FileCategory category : values()) {
            if (category.extensions.contains(normalized)) {
                return category;
            }
        }
        return null;
    }

    /** 按 key 取分类；null / all / 未知值返回 null（= 不过滤）。 */
    public static FileCategory byKey(String key) {
        if (key == null || key.isBlank() || "all".equalsIgnoreCase(key)) {
            return null;
        }
        for (FileCategory category : values()) {
            if (category.key.equalsIgnoreCase(key)) {
                return category; // 传入字符串类型的分类，传出枚举类的实例分类
            }
        }
        return null;
    }

    public List<String> extensionList() {
        return List.copyOf(extensions);
    }
}
