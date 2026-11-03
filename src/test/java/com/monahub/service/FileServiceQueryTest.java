package com.monahub.service;

import com.monahub.entity.FileInfo;
import com.monahub.entity.SysUser;
import com.monahub.exception.BusinessException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 查询/冻结/下载校验的集成测试。
 *
 * <p>依赖本机 MySQL 的 submission_hub（sql/01_schema.sql + 02_data.sql 已导入）；
 * 测试类带 @Transactional，所有写库操作在用例结束后回滚，不污染演示数据。</p>
 *
 * <p>注意：多文件上传走线程池、独立事务，不在测试事务内，
 * 因此这里只测"校验/查询/冻结/归属"这些同线程路径，真实上传由 README 的
 * curl 端到端脚本覆盖。</p>
 */
@ExtendWith(SpringExtension.class)
@ContextConfiguration(locations = "classpath:spring/applicationContext.xml")
@Transactional
class FileServiceQueryTest {

    @Autowired
    private UserService userService;

    @Autowired
    private FileService fileService;

    private SysUser zhangsan;
    private SysUser lisi;

    @BeforeEach
    void setUp() {
        zhangsan = userService.login("zhangsan", "123456");
        lisi = userService.login("lisi", "123456");
        assertNotNull(zhangsan, "需要先导入 sql/02_data.sql 的演示账号");
        assertNotNull(lisi);
    }

    @Test
    void loginReturnsSanitizedUserAndRejectsBadCredentials() {
        assertNull(zhangsan.getPassword());
        assertNull(zhangsan.getSalt());
        assertEquals("zhangsan", zhangsan.getUsername());

        assertNull(userService.login("zhangsan", "wrong-password"));
        assertNull(userService.login("nobody", "123456"));
        assertNull(userService.login(null, null));
        assertNull(userService.login("  ", "123456"));
    }

    @Test
    void metaIsListedFilteredAndIsolatedPerUploader() {
        FileInfo meta = newMeta("测试文档.pdf", "pdf");
        fileService.saveMeta(meta);
        assertNotNull(meta.getId());

        assertTrue(idsOf(fileService.list(zhangsan.getId(), "all", null)).contains(meta.getId()));
        assertTrue(idsOf(fileService.list(zhangsan.getId(), "doc", "测试文档")).contains(meta.getId()));
        assertFalse(idsOf(fileService.list(zhangsan.getId(), "image", null)).contains(meta.getId()));
        assertFalse(idsOf(fileService.list(lisi.getId(), "all", null)).contains(meta.getId()));

        Map<String, Integer> counts = fileService.countsByCategory(zhangsan.getId());
        assertTrue(counts.get("all") >= 1);
        assertTrue(counts.get("doc") >= 1);

        assertTrue(fileService.stats(zhangsan.getId()).getTotal() >= 1);
    }

    @Test
    void freezeRequiresOwnershipAndBlocksDownload() {
        FileInfo meta = newMeta("冻结测试.pdf", "pdf");
        fileService.saveMeta(meta);

        // 他人尝试冻结 → 404，且状态不变
        BusinessException foreign = assertThrows(BusinessException.class,
                () -> fileService.freeze(meta.getId(), lisi.getId(), true));
        assertEquals(404, foreign.getStatus());

        // 本人冻结 → 可下载校验返回 403（先于磁盘缺失判断）
        fileService.freeze(meta.getId(), zhangsan.getId(), true);
        BusinessException frozen = assertThrows(BusinessException.class,
                () -> fileService.openForDownload(meta.getId(), zhangsan.getId()));
        assertEquals(403, frozen.getStatus());

        // 解冻后回到"文件已丢失"的 404（测试数据没有真实磁盘文件）
        fileService.freeze(meta.getId(), zhangsan.getId(), false);
        BusinessException missing = assertThrows(BusinessException.class,
                () -> fileService.openForDownload(meta.getId(), zhangsan.getId()));
        assertEquals(404, missing.getStatus());
    }

    @Test
    void downloadOfSomeoneElsesFileIsNotFound() {
        FileInfo meta = newMeta("越权测试.pdf", "pdf");
        fileService.saveMeta(meta);

        BusinessException exception = assertThrows(BusinessException.class,
                () -> fileService.openForDownload(meta.getId(), lisi.getId()));
        assertEquals(404, exception.getStatus());
    }

    @Test
    void uploadRejectsOversizeAndDisallowedExtensions() {
        // 用 11MB 的 Mock 文件验证服务端大小校验；扩展名 .exe 验证白名单
        byte[] big = new byte[11 * 1024 * 1024];
        org.springframework.mock.web.MockMultipartFile oversize =
                new org.springframework.mock.web.MockMultipartFile("files", "big.pdf", "application/pdf", big);
        org.springframework.mock.web.MockMultipartFile exe =
                new org.springframework.mock.web.MockMultipartFile("files", "virus.exe", "application/octet-stream", new byte[]{1});

        List<com.monahub.dto.UploadResult> results =
                fileService.upload(new org.springframework.web.multipart.MultipartFile[]{oversize, exe}, zhangsan);

        assertEquals(2, results.size());
        assertFalse(results.get(0).isOk());
        assertTrue(results.get(0).getMessage().contains("上限"));
        assertFalse(results.get(1).isOk());
        assertTrue(results.get(1).getMessage().contains("类型不允许"));
    }

    private FileInfo newMeta(String originName, String ext) {
        FileInfo meta = new FileInfo();
        meta.setOriginName(originName);
        meta.setStoreName("0123456789abcdef0123456789abcdef." + ext);
        meta.setFilePath("zhangsan/2026/11/" + meta.getStoreName());
        meta.setExt(ext);
        meta.setFileSize(1024L);
        meta.setContentType("application/pdf");
        meta.setUploaderId(zhangsan.getId());
        meta.setCreateTime(LocalDateTime.now());
        meta.setStatus(1);
        meta.setDownloadCount(0);
        return meta;
    }

    private static List<Long> idsOf(List<FileInfo> files) {
        java.util.List<Long> ids = new java.util.ArrayList<>();
        for (FileInfo file : files) {
            ids.add(file.getId());
        }
        return ids;
    }
}
