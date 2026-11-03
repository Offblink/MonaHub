package com.monahub.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.monahub.dto.ExtCount;
import com.monahub.dto.FileStats;
import com.monahub.dto.StoredFile;
import com.monahub.dto.UploadResult;
import com.monahub.entity.FileInfo;
import com.monahub.entity.SysUser;
import com.monahub.exception.BusinessException;
import com.monahub.mapper.FileInfoMapper;
import com.monahub.util.FileCategory;
import com.monahub.util.FileNames;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.Callable;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

/**
 * 文件业务：校验 → 并发落盘 → 元信息入库；查询、冻结、下载校验也在这里。
 *
 * <p>并发模型（老师批注"能做同时多文件" + 用户要求"服务器并发响应"）：</p>
 * <ul>
 *   <li>每个文件一个 Callable，提交进 uploadExecutor（2–8 线程，队列满时 CallerRuns）；</li>
 *   <li>每个文件独立事务、独立结果：一个失败不影响其余文件，页面逐条报原因；</li>
 *   <li>落盘成功但入库失败时删除刚写的文件（补偿），不留孤儿文件。</li>
 * </ul>
 */
@Service
public class FileService {

    private static final Logger log = LoggerFactory.getLogger(FileService.class);

    /** 单页上限：页码来自 URL，必须夹紧，防止一次查全表。 */
    private static final int MAX_PAGE_SIZE = 50;

    private final FileInfoMapper fileInfoMapper;
    private final StorageService storageService;
    private final ThreadPoolTaskExecutor uploadExecutor;
    private final long maxSize;
    private final Set<String> allowedExt;

    public FileService(FileInfoMapper fileInfoMapper,
                       StorageService storageService,
                       @Qualifier("uploadExecutor") ThreadPoolTaskExecutor uploadExecutor,
                       @Value("${upload.max-size:10485760}") long maxSize,
                       @Value("${upload.allowed-ext:pdf,jpg,jpeg,png,doc,docx,zip}") String allowedExt) {
        this.fileInfoMapper = fileInfoMapper;
        this.storageService = storageService;
        this.uploadExecutor = uploadExecutor;
        this.maxSize = maxSize;

        Set<String> exts = new TreeSet<>();
        for (String item : allowedExt.split(",")) {
            String ext = item.trim().toLowerCase(Locale.ROOT);
            if (!ext.isEmpty()) {
                exts.add(ext);
            }
        }
        this.allowedExt = Collections.unmodifiableSet(exts);
    }

    // ============================================================
    // 上传
    // ============================================================

    /**
     * 多文件上传。方法本身不抛业务异常，所有失败都体现在逐文件结果里。
     *
     * @param files 表单字段 files（可多值）
     * @param user  当前登录用户（会话中的脱敏对象）
     */
    public List<UploadResult> upload(MultipartFile[] files, SysUser user) {
        List<UploadResult> results = new ArrayList<>();
        if (files == null || files.length == 0) {
            results.add(new UploadResult("（未选择文件）", false, "请选择要上传的文件"));
            return results;
        }

        List<Callable<UploadResult>> tasks = new ArrayList<>();
        for (MultipartFile file : files) {
            if (file == null) {
                continue;
            }
            String originName = FileNames.sanitize(file.getOriginalFilename());
            // 浏览器在未选择文件时可能提交一个空 part，直接跳过
            if (file.getSize() == 0 && originName.isEmpty()) {
                continue;
            }
            tasks.add(() -> uploadOne(file, user, originName));
        }
        if (tasks.isEmpty()) {
            results.add(new UploadResult("（未选择文件）", false, "请选择要上传的文件"));
            return results;
        }

        List<Future<UploadResult>> futures;
        try {
            futures = uploadExecutor.getThreadPoolExecutor()
                    .invokeAll(tasks, 120, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            results.add(new UploadResult("（批量上传）", false, "上传被中断，请重试"));
            return results;
        }

        for (Future<UploadResult> future : futures) {
            try {
                results.add(future.get());
            } catch (CancellationException e) {
                results.add(new UploadResult("（超时）", false, "处理超时，请分批上传"));
            } catch (ExecutionException e) {
                Throwable cause = e.getCause() == null ? e : e.getCause();
                log.error("上传任务未捕获异常", cause);
                results.add(new UploadResult("（未知）", false, "处理失败，请重试"));
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                results.add(new UploadResult("（批量上传）", false, "上传被中断，请重试"));
            }
        }
        return results;
    }

    private UploadResult uploadOne(MultipartFile file, SysUser user, String originName) {
        try {
            String ext = FileNames.extOf(originName);
            validate(originName, ext, file.getSize());

            StoredFile stored = storageService.save(file, user.getUsername(), originName);

            FileInfo meta = new FileInfo();
            meta.setOriginName(stored.getOriginName());
            meta.setStoreName(stored.getStoreName());
            meta.setFilePath(stored.getRelativePath());
            meta.setExt(stored.getExt());
            meta.setFileSize(stored.getSize());
            meta.setContentType(stored.getContentType());
            meta.setUploaderId(user.getId());
            meta.setCreateTime(LocalDateTime.now());
            meta.setStatus(1);
            meta.setDownloadCount(0);

            try {
                saveMeta(meta);
            } catch (RuntimeException e) {
                // 入库失败 → 删除刚写的文件，保证"库里有记录 ⇔ 磁盘有文件"
                storageService.deleteQuietly(stored.getRelativePath());
                throw e;
            }
            return new UploadResult(originName, true, "上传成功");
        } catch (BusinessException e) {
            return new UploadResult(originName, false, e.getMessage());
        } catch (Exception e) {
            log.error("上传失败: {}", originName, e);
            return new UploadResult(originName, false, "服务器写入失败，请重试");
        }
    }

    /** 服务端校验：不依赖任何前端限制（任务书硬性要求）。 */
    private void validate(String originName, String ext, long size) {
        if (originName == null || originName.isEmpty()) {
            throw new BusinessException(400, "文件名不能为空");
        }
        if (size <= 0) {
            throw new BusinessException(400, "不能上传空文件");
        }
        if (size > maxSize) {
            throw new BusinessException(400,
                    "文件「" + originName + "」超过 " + FileNames.sizeText(maxSize) + " 上限");
        }
        if (ext.isEmpty()) {
            throw new BusinessException(400, "文件「" + originName + "」缺少扩展名");
        }
        if (!allowedExt.contains(ext)) {
            throw new BusinessException(400,
                    "文件「" + originName + "」类型不允许，仅支持 " + String.join(" / ", allowedExt));
        }
    }

    /** 单条元信息入库；独立事务，由上传线程调用（走 Service 代理）。 */
    @Transactional
    public void saveMeta(FileInfo meta) {
        fileInfoMapper.insert(meta);
    }

    // ============================================================
    // 查询 / 统计
    // ============================================================

    /**
     * 分页列表：按上传人隔离，可按分类与关键字收窄。
     *
     * <p>分页交给 MyBatis-Plus 的 PaginationInnerInterceptor：
     * XML 里只写 SELECT ... WHERE ... ORDER BY，LIMIT/OFFSET 与 count 由插件生成。</p>
     *
     * @param pageNo   ＞=1，越界时自动回退到最后一页
     * @param pageSize 1..MAX_PAGE_SIZE
     */
    public Page<FileInfo> list(Long uploaderId, String categoryKey, String keyword,
                               int pageNo, int pageSize) {
        FileCategory category = FileCategory.byKey(categoryKey);
        List<String> exts = category == null ? null : category.extensionList();
        String q = keyword == null ? null : keyword.trim();
        if (q != null && q.isEmpty()) {
            q = null;
        }

        int current = Math.max(1, pageNo);
        int size = Math.min(MAX_PAGE_SIZE, Math.max(1, pageSize));
        Page<FileInfo> page = new Page<>(current, size);

        List<FileInfo> records = fileInfoMapper.selectPage(page, uploaderId, exts, q);
        page.setRecords(records);

        // 删除/筛选后页码越界：回退到最后一页再查一次，避免用户看到空白页
        if (records.isEmpty() && current > 1 && page.getTotal() > 0
                && page.getPages() < current) {
            page.setCurrent(page.getPages());
            page.setRecords(fileInfoMapper.selectPage(page, uploaderId, exts, q));
        }
        return page;
    }

    public FileStats stats(Long uploaderId) {
        LocalDateTime monthStart = LocalDate.now().withDayOfMonth(1).atStartOfDay();
        return new FileStats(
                fileInfoMapper.countByUploader(uploaderId),
                fileInfoMapper.countByUploaderSince(uploaderId, monthStart),
                fileInfoMapper.sumDownloadCount(uploaderId));
    }

    /**
     * 分类计数：一次 GROUP BY 拿全量，Java 侧按 {@link FileCategory} 归并，
     * 页面顶部胶囊直接显示各类数量。
     */
    public Map<String, Integer> countsByCategory(Long uploaderId) {
        Map<String, Integer> counts = new LinkedHashMap<>();
        counts.put("all", 0);
        for (FileCategory category : FileCategory.values()) {
            counts.put(category.key(), 0);
        }
        counts.put("other", 0);

        int total = 0;
        for (ExtCount row : fileInfoMapper.countByExt(uploaderId)) {
            total += row.getCnt();
            FileCategory category = FileCategory.of(row.getExt());
            String key = category == null ? "other" : category.key();
            counts.merge(key, row.getCnt(), Integer::sum);
        }
        counts.put("all", total);
        return counts;
    }

    // ============================================================
    // 重命名 / 逻辑删除
    // ============================================================

    /**
     * 重命名：只改展示名 origin_name，磁盘 UUID 名不动。
     * 允许只输入主名（自动补原扩展名），但不允许改扩展名 —— 否则会出现
     * "名字是 .pdf、内容其实是 exe" 的欺骗面。
     */
    @Transactional
    public void rename(Long id, Long uploaderId, String newName) {
        FileInfo file = fileInfoMapper.selectByIdAndUploader(id, uploaderId);
        if (file == null) {
            throw new BusinessException(404, "文件不存在或无权操作");
        }

        String name = FileNames.sanitize(newName);
        if (name == null || name.isBlank()) {
            throw new BusinessException(400, "文件名不能为空");
        }

        String ext = file.getExt();
        String newExt = FileNames.extOf(name);
        if (newExt.isEmpty()) {
            name = name + "." + ext;
        } else if (!newExt.equalsIgnoreCase(ext)) {
            throw new BusinessException(400, "只能修改文件名，不能修改扩展名");
        }
        if (name.length() > 255) {
            throw new BusinessException(400, "文件名不能超过 255 个字符");
        }

        if (fileInfoMapper.rename(id, uploaderId, name) == 0) {
            throw new BusinessException(404, "文件不存在或无权操作");
        }
    }

    /**
     * 逻辑删除：deleted = 1，记录与磁盘文件都保留（管理员可清理或恢复）。
     * 删除后列表、统计、下载、改名全部不可见。
     */
    @Transactional
    public void delete(Long id, Long uploaderId) {
        if (fileInfoMapper.softDelete(id, uploaderId) == 0) {
            throw new BusinessException(404, "文件不存在或无权操作");
        }
    }

    // ============================================================
    // 冻结 / 下载
    // ============================================================

    /** 冻结 = status 0，解冻 = status 1；WHERE 带 uploader_id，改不到别人的记录。 */
    @Transactional
    public void freeze(Long id, Long uploaderId, boolean frozen) {
        int updated = fileInfoMapper.updateStatus(id, uploaderId, frozen ? 0 : 1);
        if (updated == 0) {
            throw new BusinessException(404, "文件不存在或无权操作");
        }
    }

    /**
     * 下载前校验：归属 → 冻结状态 → 磁盘是否存在。
     * 路径永远从数据库取，绝不接收客户端传入的文件名或路径。
     */
    public FileInfo openForDownload(Long id, Long uploaderId) {
        FileInfo file = fileInfoMapper.selectByIdAndUploader(id, uploaderId);
        if (file == null) {
            throw new BusinessException(404, "文件不存在或无权访问");
        }
        if (file.isFrozen()) {
            throw new BusinessException(403, "该文件已被上传者冻结，暂不可下载");
        }
        if (!storageService.exists(file.getFilePath())) {
            throw new BusinessException(404, "文件已丢失，请联系管理员");
        }
        return file;
    }

    /**
     * 下载成功后的计数。参数里的 downloaderId/ip 不参与更新，
     * 只服务于 {@link com.monahub.aspect.DownloadLogAspect} 的切点绑定（F5）。
     */
    @Transactional
    public void afterDownload(Long fileId, Long downloaderId, String ip) {
        fileInfoMapper.incrementDownloadCount(fileId);
    }
}
