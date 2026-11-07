package com.monahub.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.monahub.dto.FileCreateRequest;
import com.monahub.dto.FileUpdateRequest;
import com.monahub.dto.Result;
import com.monahub.entity.FileInfo;
import com.monahub.entity.SysUser;
import com.monahub.interceptor.LoginInterceptor;
import com.monahub.service.FileService;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;
import java.util.List;

/**
 * JSON API（实验二任务 5.3 的 RESTful 补充）：/api/files 五件事。
 *
 * <p>与 JSP 页面流程共用同一个 FileService 与会话；未登录由
 * {@link LoginInterceptor} 统一回 401 JSON（页面流程仍是 302）。</p>
 */
@RestController
@RequestMapping("/api/files")
public class ApiController {

    private final FileService fileService;

    public ApiController(FileService fileService) {
        this.fileService = fileService;
    }

    /** 分页 + 多条件：page/size 夹紧与越界回退都在 Service。 */
    @GetMapping
    public Result<Page<FileInfo>> list(
            @RequestParam(value = "page", defaultValue = "1") int page,
            @RequestParam(value = "size", defaultValue = "10") int size,
            @RequestParam(value = "category", defaultValue = "all") String category,
            @RequestParam(value = "q", defaultValue = "") String q,
            javax.servlet.http.HttpSession session) {
        return Result.ok(fileService.list(currentUser(session).getId(), category, q, page, size));
    }

    @GetMapping("/{id}")
    public Result<FileInfo> get(@PathVariable("id") Long id,
                                javax.servlet.http.HttpSession session) {
        return Result.ok(fileService.get(id, currentUser(session).getId()));
    }

    /** 新增：JSON 上传（Base64 内容），与表单上传同一条校验/去重/补偿链路。 */
    @PostMapping
    public Result<FileInfo> create(@RequestBody @Valid FileCreateRequest request,
                                   javax.servlet.http.HttpSession session) {
        return Result.ok(fileService.create(currentUser(session), request.getName(),
                request.getContent()));
    }

    /** 修改 = 重命名（只改展示名，扩展名规则由 Service 把关）。 */
    @PutMapping
    public Result<Void> update(@RequestBody @Valid FileUpdateRequest request,
                               javax.servlet.http.HttpSession session) {
        fileService.rename(request.getId(), currentUser(session).getId(), request.getNewName());
        return Result.ok(null);
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable("id") Long id,
                               javax.servlet.http.HttpSession session) {
        fileService.delete(id, currentUser(session).getId());
        return Result.ok(null);
    }

    /** 批量删除：body 为 id 数组；先全量校验归属，任一不存在则整批不删。 */
    @DeleteMapping
    public Result<Integer> deleteBatch(@RequestBody List<Long> ids,
                                       javax.servlet.http.HttpSession session) {
        return Result.ok(fileService.deleteBatch(ids, currentUser(session).getId()));
    }

    private static SysUser currentUser(javax.servlet.http.HttpSession session) {
        SysUser user = (SysUser) session.getAttribute(LoginInterceptor.SESSION_USER);
        if (user == null) {
            // 拦截器已拦，这里兜底防止直调时 NPE 变 500
            throw new com.monahub.exception.BusinessException(401, "未登录");
        }
        return user;
    }
}
