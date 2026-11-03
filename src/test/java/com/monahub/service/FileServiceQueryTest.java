package com.monahub.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.monahub.entity.FileInfo;
import com.monahub.entity.SysUser;
import com.monahub.exception.BusinessException;
import com.monahub.mapper.FileInfoMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 查询 / 分页 / 重命名 / 逻辑删除 / 冻结 / 下载校验的集成测试。
 *
 * <p>依赖本机 MySQL 的 submission_hub（sql/01_schema.sql + 02_data.sql 已导入）；
 * 测试类带 @Transactional，所有写库操作在用例结束后回滚，不污染演示数据。</p>
 *
 * <p>注意：多文件上传走线程池、独立事务，不在测试事务内，
 * 因此这里只测"校验/查询/分页/改名/删除"这些同线程路径，真实上传由 README 的
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

    @Autowired
    private FileInfoMapper fileInfoMapper;

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

        assertTrue(idsOf(fileService.list(zhangsan.getId(), "all", null, 1, 20)).contains(meta.getId()));
        assertTrue(idsOf(fileService.list(zhangsan.getId(), "doc", "测试文档", 1, 20)).contains(meta.getId()));
        assertFalse(idsOf(fileService.list(zhangsan.getId(), "image", null, 1, 20)).contains(meta.getId()));
        assertFalse(idsOf(fileService.list(lisi.getId(), "all", null, 1, 20)).contains(meta.getId()));

        Map<String, Integer> counts = fileService.countsByCategory(zhangsan.getId());
        assertTrue(counts.get("all") >= 1);
        assertTrue(counts.get("doc") >= 1);

        assertTrue(fileService.stats(zhangsan.getId()).getTotal() >= 1);
    }

    @Test
    void duplicateLookupMatchesSameUploaderAndIgnoresDeletedAndOthers() {
        String md5 = "0123456789abcdef0123456789abcdef";
        FileInfo meta = newMeta("原始文件.pdf", "pdf");
        meta.setMd5(md5);
        fileService.saveMeta(meta);

        // 本人、同内容 → 命中
        FileInfo hit = fileInfoMapper.selectDuplicate(zhangsan.getId(), md5);
        assertNotNull(hit);
        assertEquals(meta.getId(), hit.getId());

        // 他人、同内容 → 不算重复
        assertNull(fileInfoMapper.selectDuplicate(lisi.getId(), md5));

        // 删除后不再参与去重（允许重新上传）
        fileService.delete(meta.getId(), zhangsan.getId());
        assertNull(fileInfoMapper.selectDuplicate(zhangsan.getId(), md5));
    }

    @Test
    void paginationSplitsRecordsAndReportsTotal() {
        String keyword = "分页专用" + UUID.randomUUID().toString().substring(0, 8);
        for (int i = 1; i <= 3; i++) {
            fileService.saveMeta(newMeta(keyword + "-" + i + ".pdf", "pdf"));
        }

        Page<FileInfo> first = fileService.list(zhangsan.getId(), "all", keyword, 1, 2);
        assertEquals(3, first.getTotal());
        assertEquals(2, first.getRecords().size());
        assertEquals(1, first.getCurrent());

        Page<FileInfo> second = fileService.list(zhangsan.getId(), "all", keyword, 2, 2);
        assertEquals(3, second.getTotal());
        assertEquals(1, second.getRecords().size());
        assertEquals(2, second.getCurrent());

        // 页码越界 → 自动回退到最后一页，而不是空白
        Page<FileInfo> beyond = fileService.list(zhangsan.getId(), "all", keyword, 99, 2);
        assertEquals(2, beyond.getCurrent());
        assertEquals(1, beyond.getRecords().size());

        // 每页上限被夹紧
        assertEquals(50, fileService.list(zhangsan.getId(), "all", keyword, 1, 100000).getSize());
    }

    @Test
    void renameKeepsExtensionAndOnlyTouchesOwnFiles() {
        FileInfo meta = newMeta("旧名字.pdf", "pdf");
        fileService.saveMeta(meta);

        // 只给主名 → 自动补原扩展名
        fileService.rename(meta.getId(), zhangsan.getId(), "新名字");
        assertTrue(originNames(zhangsan).contains("新名字.pdf"));

        // 同扩展名（大小写不敏感）允许
        fileService.rename(meta.getId(), zhangsan.getId(), "又改了.PDF");
        assertTrue(originNames(zhangsan).contains("又改了.PDF"));

        // 改扩展名 → 400
        BusinessException wrongExt = assertThrows(BusinessException.class,
                () -> fileService.rename(meta.getId(), zhangsan.getId(), "伪装.exe"));
        assertEquals(400, wrongExt.getStatus());

        // 空名字 → 400
        BusinessException empty = assertThrows(BusinessException.class,
                () -> fileService.rename(meta.getId(), zhangsan.getId(), "   "));
        assertEquals(400, empty.getStatus());

        // 他人改名 → 404
        BusinessException foreign = assertThrows(BusinessException.class,
                () -> fileService.rename(meta.getId(), lisi.getId(), "抢改.pdf"));
        assertEquals(404, foreign.getStatus());
    }

    @Test
    void logicalDeleteHidesRecordFromListDownloadAndStats() {
        FileInfo meta = newMeta("待删除.pdf", "pdf");
        fileService.saveMeta(meta);

        long statsBefore = fileService.stats(zhangsan.getId()).getTotal();
        fileService.delete(meta.getId(), zhangsan.getId());

        assertFalse(idsOf(fileService.list(zhangsan.getId(), "all", "待删除", 1, 20))
                .contains(meta.getId()));
        assertEquals(statsBefore - 1, fileService.stats(zhangsan.getId()).getTotal());

        BusinessException download = assertThrows(BusinessException.class,
                () -> fileService.openForDownload(meta.getId(), zhangsan.getId()));
        assertEquals(404, download.getStatus());

        // 已删除的记录不能再次改名/删除，也不能被他人删除
        BusinessException rename = assertThrows(BusinessException.class,
                () -> fileService.rename(meta.getId(), zhangsan.getId(), "复活.pdf"));
        assertEquals(404, rename.getStatus());
        BusinessException foreign = assertThrows(BusinessException.class,
                () -> fileService.delete(meta.getId(), lisi.getId()));
        assertEquals(404, foreign.getStatus());
    }

    @Test
    void freezeRequiresOwnershipAndBlocksDownload() {
        FileInfo meta = newMeta("冻结测试.pdf", "pdf");
        fileService.saveMeta(meta);

        BusinessException foreign = assertThrows(BusinessException.class,
                () -> fileService.freeze(meta.getId(), lisi.getId(), true));
        assertEquals(404, foreign.getStatus());

        fileService.freeze(meta.getId(), zhangsan.getId(), true);
        BusinessException frozen = assertThrows(BusinessException.class,
                () -> fileService.openForDownload(meta.getId(), zhangsan.getId()));
        assertEquals(403, frozen.getStatus());

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
        meta.setStoreName(UUID.randomUUID().toString().replace("-", "") + "." + ext);
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

    private List<Long> idsOf(Page<FileInfo> page) {
        List<Long> ids = new ArrayList<>();
        for (FileInfo file : page.getRecords()) {
            ids.add(file.getId());
        }
        return ids;
    }

    private List<String> originNames(SysUser user) {
        List<String> names = new ArrayList<>();
        for (FileInfo file : fileService.list(user.getId(), "all", null, 1, 50).getRecords()) {
            names.add(file.getOriginName());
        }
        return names;
    }
}
