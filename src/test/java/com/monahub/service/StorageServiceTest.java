package com.monahub.service;

import com.monahub.dto.StoredFile;
import com.monahub.exception.BusinessException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 存储层：UUID 命名、用户/年/月分目录、中文原名只留元信息、路径穿越拒绝。
 */
class StorageServiceTest {

    @TempDir
    Path tempDir;

    private StorageService storage;

    @BeforeEach
    void setUp() throws Exception {
        storage = new StorageService(tempDir.toString());
        storage.init();
    }

    @Test
    void saveWritesUuidNameUnderUserYearMonthDirectory() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "files", "结业作业.PDF", "application/pdf",
                "hello monahub".getBytes(StandardCharsets.UTF_8));

        StoredFile stored = storage.save(file, "zhangsan", "结业作业.PDF");

        String expectedPrefix = "zhangsan/" + LocalDate.now().getYear() + "/"
                + String.format("%02d", LocalDate.now().getMonthValue()) + "/";
        assertTrue(stored.getRelativePath().startsWith(expectedPrefix), stored.getRelativePath());
        assertTrue(stored.getStoreName().matches("[0-9a-f]{32}\\.pdf"), stored.getStoreName());
        assertFalse(stored.getRelativePath().contains("结业作业"));
        assertEquals("结业作业.PDF", stored.getOriginName());
        assertEquals("pdf", stored.getExt());
        assertEquals(file.getSize(), stored.getSize());

        Path onDisk = storage.resolve(stored.getRelativePath());
        assertTrue(Files.isRegularFile(onDisk));
        assertEquals("hello monahub", Files.readString(onDisk));
    }

    @Test
    void resolveRejectsTraversalOutsideRoot() {
        assertThrows(BusinessException.class, () -> storage.resolve("../evil.txt"));
        assertThrows(BusinessException.class, () -> storage.resolve("zhangsan/../../evil.txt"));
        assertThrows(BusinessException.class, () -> storage.resolve(""));
        assertThrows(BusinessException.class, () -> storage.resolve(null));
    }

    @Test
    void existsAndDeleteQuietlyAreSafe() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "files", "a.png", "image/png", new byte[]{1, 2, 3});
        StoredFile stored = storage.save(file, "lisi", "a.png");

        assertTrue(storage.exists(stored.getRelativePath()));
        storage.deleteQuietly(stored.getRelativePath());
        assertFalse(storage.exists(stored.getRelativePath()));

        // 重复删除、非法路径都不应抛异常
        storage.deleteQuietly(stored.getRelativePath());
        storage.deleteQuietly("../outside.txt");
    }

    @Test
    void blankUsernameIsSanitizedIntoDirectory() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "files", "b.zip", "application/zip", new byte[]{1});
        StoredFile stored = storage.save(file, "张三/../x", "b.zip");
        String firstSegment = stored.getRelativePath().split("/")[0];
        assertTrue(firstSegment.matches("[A-Za-z0-9_-]+"), firstSegment);
        assertFalse(stored.getRelativePath().contains(".."));
    }
}
