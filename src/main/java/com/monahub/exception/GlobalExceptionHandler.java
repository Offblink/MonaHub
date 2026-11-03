package com.monahub.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import javax.servlet.http.HttpServletResponse;
import org.springframework.ui.Model;

/**
 * 统一异常出口：全部落到 error.jsp，用户只看到一句人话，堆栈只进日志。
 *
 * <p>任务书要求"文件不存在时返回友好提示，不打印异常堆栈"，
 * 这里同时也是 F4 三种失败（越权 / 冻结 / 缺失）的兜底。</p>
 */
@ControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(BusinessException.class)
    public String handleBusiness(BusinessException e, HttpServletResponse response, Model model) {
        response.setStatus(e.getStatus());
        model.addAttribute("code", e.getStatus());
        model.addAttribute("message", e.getMessage());
        return "error";
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public String handleTooLarge(MaxUploadSizeExceededException e,
                                 HttpServletResponse response, Model model) {
        log.warn("上传超过大小限制: {}", e.getMessage());
        response.setStatus(413);
        model.addAttribute("code", 413);
        model.addAttribute("message", "文件太大了：单个文件不超过 10MB，一次最多 50MB");
        return "error";
    }

    @ExceptionHandler(Exception.class)
    public String handleOther(Exception e, HttpServletResponse response, Model model) {
        log.error("未捕获异常", e);
        response.setStatus(500);
        model.addAttribute("code", 500);
        model.addAttribute("message", "服务器开小差了，请稍后再试");
        return "error";
    }
}
