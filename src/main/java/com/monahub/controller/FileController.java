package com.monahub.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.monahub.dto.UploadResult;
import com.monahub.entity.FileInfo;
import com.monahub.entity.SysUser;
import com.monahub.interceptor.LoginInterceptor;
import com.monahub.service.FileService;
import com.monahub.service.StorageService;
import com.monahub.util.FileNames;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.io.InputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * 工作台：列表 / 上传 / 冻结 / 下载。
 *
 * <p>Controller 只做三件事：取会话用户、调 Service、组织视图或响应；
 * 文件校验、路径安全、归属判断全部在 Service 层完成。</p>
 */
@Controller
@RequestMapping("/files")
public class FileController {

    private static final Logger log = LoggerFactory.getLogger(FileController.class);

    private final FileService fileService;
    private final StorageService storageService;
    private final ObjectMapper objectMapper;

    public FileController(FileService fileService,
                          StorageService storageService,
                          ObjectMapper objectMapper) {
        this.fileService = fileService;
        this.storageService = storageService;
        this.objectMapper = objectMapper;
    }

    /** 列表页：恒定只看本人；分类与关键字是可选的收窄条件。 */
    @GetMapping
    public String list(@RequestParam(value = "category", defaultValue = "all") String category,
                       @RequestParam(value = "q", defaultValue = "") String keyword,
                       HttpSession session,
                       Model model) {
        SysUser user = currentUser(session);
        model.addAttribute("user", user);
        model.addAttribute("files", fileService.list(user.getId(), category, keyword));
        model.addAttribute("stats", fileService.stats(user.getId()));
        model.addAttribute("counts", fileService.countsByCategory(user.getId()));
        model.addAttribute("category", category);
        model.addAttribute("keyword", keyword);
        return "files";
    }

    /**
     * 多文件上传。
     *
     * <p>双形态（渐进增强）：</p>
     * <ul>
     *   <li>普通表单 → 302 回 /files，flash 里带逐文件结果；</li>
     *   <li>XHR（带 X-Requested-With）→ 直接返回 JSON 数组，前端逐条显示进度/结果。</li>
     * </ul>
     */
    @PostMapping("/upload")
    public String upload(@RequestParam(value = "files", required = false) MultipartFile[] files,
                         HttpServletRequest request,
                         HttpServletResponse response,
                         HttpSession session,
                         RedirectAttributes redirectAttributes) throws IOException {
        SysUser user = currentUser(session);
        List<UploadResult> results = fileService.upload(files, user);

        if (isAjax(request)) {
            response.setContentType("application/json;charset=UTF-8");
            response.setCharacterEncoding("UTF-8");
            objectMapper.writeValue(response.getWriter(), results);
            // 已经直接写出响应，不再交给视图解析器
            return null;
        }

        redirectAttributes.addFlashAttribute("results", results);
        return "redirect:/files";
    }

    /** 冻结 / 解冻。frozen=1 冻结，frozen=0 解冻；幂等。 */
    @PostMapping("/{id}/freeze")
    public String freeze(@PathVariable("id") Long id,
                         @RequestParam(value = "frozen", defaultValue = "1") int frozen,
                         @RequestParam(value = "category", defaultValue = "all") String category,
                         @RequestParam(value = "q", defaultValue = "") String keyword,
                         HttpSession session,
                         RedirectAttributes redirectAttributes) {
        SysUser user = currentUser(session);
        boolean target = frozen == 1;
        fileService.freeze(id, user.getId(), target);
        redirectAttributes.addFlashAttribute("message",
                target ? "已冻结，该文件暂时无法下载" : "已解冻，可以继续下载");
        redirectAttributes.addAttribute("category", category);
        redirectAttributes.addAttribute("q", keyword);
        return "redirect:/files";
    }

    /**
     * 下载：只认 fileId。磁盘路径从数据库取，绝不接收客户端传入的文件名/路径。
     */
    @GetMapping("/{id}/download")
    public void download(@PathVariable("id") Long id,
                         HttpServletRequest request,
                         HttpServletResponse response,
                         HttpSession session) throws IOException {
        SysUser user = currentUser(session);
        FileInfo file = fileService.openForDownload(id, user.getId());
        Path path = storageService.resolve(file.getFilePath());

        String encodedName = URLEncoder.encode(file.getOriginName(), StandardCharsets.UTF_8)
                .replace("+", "%20");
        String fallbackName = FileNames.asciiFallback(file.getOriginName());

        String contentType = file.getContentType();
        response.setContentType(contentType == null || contentType.isBlank()
                ? "application/octet-stream" : contentType);
        response.setContentLengthLong(file.getFileSize() == null
                ? Files.size(path) : file.getFileSize());
        // RFC 5987：filename* 用 UTF-8 百分号编码，中文名在各浏览器都不乱码；
        // filename 保留 ASCII 退化名，兼容老客户端
        response.setHeader("Content-Disposition",
                "attachment; filename=\"" + fallbackName + "\"; filename*=UTF-8''" + encodedName);
        response.setHeader("X-Content-Type-Options", "nosniff");

        try (InputStream in = Files.newInputStream(path)) {
            IOUtils.copy(in, response.getOutputStream());
            response.flushBuffer();
        }

        // 计数与日志是"附带的账"，失败不能影响已经发出去的文件
        try {
            fileService.afterDownload(file.getId(), user.getId(), clientIp(request));
        } catch (Exception e) {
            log.warn("下载计数/日志写入失败 fileId={}", file.getId(), e);
        }
    }

    // ============================================================

    private static SysUser currentUser(HttpSession session) {
        return (SysUser) session.getAttribute(LoginInterceptor.SESSION_USER);
    }

    private static boolean isAjax(HttpServletRequest request) {
        return "XMLHttpRequest".equalsIgnoreCase(request.getHeader("X-Requested-With"));
    }

    private static String clientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        String ip = (forwarded == null || forwarded.isBlank())
                ? request.getRemoteAddr()
                : forwarded.split(",")[0].trim();
        return ip.length() > 64 ? ip.substring(0, 64) : ip;
    }
}
