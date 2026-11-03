package com.monahub.service;

import com.monahub.dto.StoredFile;
import com.monahub.exception.BusinessException;
import com.monahub.util.FileNames;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;

/**
 * 磁盘存储：负责"文件放在哪、叫什么"，不碰数据库。
 *
 * <p>目录规则（任务书 F2）：{@code <upload.root>/<username>/<yyyy>/<MM>/<uuid>.<ext>}，
 * 既避免单目录堆积，也让"谁的资料"在磁盘上一眼可见。</p>
 *
 * <p>安全：根目录只来自配置；所有拼接路径都先 {@code normalize()} 再校验
 * {@code startsWith(root)}，任何 {@code ../} 逃逸都会被拒绝。</p>
 */
@Service
public class StorageService {

    private static final Logger log = LoggerFactory.getLogger(StorageService.class);

    private final Path root;

    /**
     * @param configuredRoot upload.root；留空时落到 {@code ${user.home}/SubmissionHub/upload}
     */
    public StorageService(@Value("${upload.root:}") String configuredRoot) {
        String value = configuredRoot == null ? "" : configuredRoot.trim();
        if (value.isEmpty()) {
            this.root = Paths.get(System.getProperty("user.home"), "SubmissionHub", "upload")
                    .toAbsolutePath().normalize();
        } else {
            this.root = Paths.get(value).toAbsolutePath().normalize();
        }
    }

    @PostConstruct
    public void init() throws IOException {
        Files.createDirectories(root);
        log.info("上传根目录: {}", root);
    }

    public Path getRoot() {
        return root;
    }

    /**
     * 落盘：生成 UUID 名 + 年/月目录，返回相对路径给数据库。
     * 注意 {@code MultipartFile#transferTo} 需要绝对路径，所以这里用 Path 而不是字符串拼接。
     */
    public StoredFile save(MultipartFile file, String username, String originName) throws IOException {
        String ext = FileNames.extOf(originName);
        String storeName = FileNames.storeName(ext);

        LocalDate today = LocalDate.now();
        String relativeDir = safeSegment(username) + "/" + today.getYear()
                + "/" + String.format("%02d", today.getMonthValue());

        Path dir = withinRoot(relativeDir);
        Files.createDirectories(dir);
        Path target = withinRoot(relativeDir + "/" + storeName);

        file.transferTo(target.toFile());

        return new StoredFile(originName, storeName, relativeDir + "/" + storeName,
                ext, file.getSize(), file.getContentType());
    }

    /**
     * 相对路径 → 绝对路径，并做 canonical 前缀校验。
     *
     * @throws BusinessException 路径为空或逃逸出上传根目录
     */
    public Path resolve(String relativePath) {
        if (relativePath == null || relativePath.isBlank()) {
            throw new BusinessException(400, "文件路径为空");
        }
        return withinRoot(relativePath);
    }

    public boolean exists(String relativePath) {
        try {
            return Files.isRegularFile(resolve(relativePath));
        } catch (BusinessException e) {
            return false;
        }
    }

    /** 补偿用：入库失败时删掉刚写的文件；路径非法或删除失败都只记日志，不掩盖原始异常。 */
    public void deleteQuietly(String relativePath) {
        Path target;
        try {
            target = resolve(relativePath);
        } catch (BusinessException e) {
            log.debug("跳过非法路径的清理: {}", relativePath);
            return;
        }
        try {
            Files.deleteIfExists(target);
        } catch (Exception e) {
            log.warn("清理磁盘文件失败: {}", relativePath, e);
        }
    }

    private Path withinRoot(String relative) {
        Path candidate = root.resolve(relative.replace('\\', '/')).normalize();
        if (!candidate.startsWith(root)) {
            throw new BusinessException(403, "非法的文件路径");
        }
        return candidate;
    }

    /** 目录名只允许字母/数字/下划线/连字符；账号来自数据库，仍然防御性清洗。 */
    private static String safeSegment(String username) {
        String cleaned = username == null ? "" : username.replaceAll("[^A-Za-z0-9_-]", "_");
        return cleaned.isBlank() ? "unknown" : cleaned;
    }
}
