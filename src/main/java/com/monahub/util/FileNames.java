package com.monahub.util;

import java.util.Locale;
import java.util.UUID;

/**
 * 文件名的纯函数工具：清洗、取扩展名、生成 UUID 名、格式化大小、生成 ASCII 退化名。
 * 全部无状态，便于单测。
 */
public final class FileNames {

    private FileNames() {
    }

    /**
     * 取最后一个点之后的扩展名并转小写；没有扩展名返回空串。
     * 例：{"作业.PDF" → "pdf"}、{"archive.tar.gz" → "gz"}、{"README" → ""}
     */
    public static String extOf(String filename) {
        if (filename == null) {
            return "";
        }
        int dot = filename.lastIndexOf('.');
        if (dot < 0 || dot == filename.length() - 1) {
            return "";
        }
        return filename.substring(dot + 1).toLowerCase(Locale.ROOT);
    }

    /**
     * 清洗客户端上报的原始文件名：去掉路径部分（IE/边缘浏览器会带 C:\...），
     * 去掉控制字符，限制 255 字符（与数据库列一致）。清洗结果只用于入库展示。
     */
    public static String sanitize(String originalFilename) {
        if (originalFilename == null) {
            return "";
        }
        String name = originalFilename.replace('\\', '/');
        int slash = name.lastIndexOf('/');
        if (slash >= 0) {
            name = name.substring(slash + 1);
        }
        name = name.replaceAll("[\\p{Cntrl}]", "").trim();
        if (name.length() > 255) {
            name = name.substring(name.length() - 255);
        }
        return name;
    }

    /** 磁盘名：32 位 UUID（去横线）+ 扩展名，与用户原始名彻底解耦。 */
    public static String storeName(String ext) {
        String uuid = UUID.randomUUID().toString().replace("-", "");
        return ext == null || ext.isBlank() ? uuid : uuid + "." + ext;
    }

    /** 人类可读大小：B / KB / MB，保留一位小数。 */
    public static String sizeText(long bytes) {
        if (bytes < 1024) {
            return bytes + " B";
        }
        double kb = bytes / 1024.0;
        if (kb < 1024) {
            return String.format(Locale.ROOT, "%.1f KB", kb);
        }
        return String.format(Locale.ROOT, "%.1f MB", kb / 1024.0);
    }

    /**
     * Content-Disposition 里的 ASCII 退化名：非 ASCII 字符替换为下划线，保留扩展名。
     * 现代浏览器优先使用 filename*=UTF-8''...，这里是给老客户端的兜底。
     */
    public static String asciiFallback(String originName) {
        if (originName == null || originName.isBlank()) {
            return "download";
        }
        String ascii = originName.replaceAll("[^\\x20-\\x7E]", "_").replace("\"", "'");
        return ascii.isBlank() ? "download" : ascii;
    }
}
