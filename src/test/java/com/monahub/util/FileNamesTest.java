package com.monahub.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 文件名纯函数：扩展名、清洗、UUID 名、大小格式化、ASCII 退化名、分类。
 */
class FileNamesTest {

    @Test
    void extOfHandlesCaseAndMissingExtension() {
        assertEquals("pdf", FileNames.extOf("作业.PDF"));
        assertEquals("gz", FileNames.extOf("archive.tar.gz"));
        assertEquals("", FileNames.extOf("README"));
        assertEquals("", FileNames.extOf("trailing."));
        assertEquals("", FileNames.extOf(null));
    }

    @Test
    void sanitizeStripsClientSidePathAndControlChars() {
        assertEquals("作业.pdf", FileNames.sanitize("C:\\Users\\me\\作业.pdf"));
        assertEquals("作业.pdf", FileNames.sanitize("/tmp/作业.pdf"));
        assertEquals("a.pdf", FileNames.sanitize("a\u0000.pdf"));
        assertEquals("", FileNames.sanitize(null));
    }

    @Test
    void storeNameIsUuidPlusExtension() {
        String name = FileNames.storeName("pdf");
        assertTrue(name.matches("[0-9a-f]{32}\\.pdf"), name);
        assertFalse(name.contains("作业"));
        assertTrue(FileNames.storeName("").matches("[0-9a-f]{32}"));
    }

    @Test
    void sizeTextReadsNaturally() {
        assertEquals("0 B", FileNames.sizeText(0));
        assertEquals("1023 B", FileNames.sizeText(1023));
        assertEquals("1.0 KB", FileNames.sizeText(1024));
        assertEquals("1.5 MB", FileNames.sizeText(1024L * 1024 + 512 * 1024));
    }

    @Test
    void asciiFallbackKeepsExtensionAndEscapesQuotes() {
        assertEquals("__.pdf", FileNames.asciiFallback("作业.pdf"));
        assertEquals("a'b.pdf", FileNames.asciiFallback("a\"b.pdf"));
        assertEquals("download", FileNames.asciiFallback(""));
    }

    @Test
    void categoryGroupsWhitelistedExtensions() {
        assertEquals(FileCategory.DOC, FileCategory.of("pdf"));
        assertEquals(FileCategory.DOC, FileCategory.of("DOCX"));
        assertEquals(FileCategory.IMAGE, FileCategory.of("png"));
        assertEquals(FileCategory.ARCHIVE, FileCategory.of("zip"));
        assertNull(FileCategory.of("exe"));
        assertNull(FileCategory.of(null));
        assertNull(FileCategory.byKey("all"));
        assertEquals(FileCategory.IMAGE, FileCategory.byKey("image"));
    }
}
