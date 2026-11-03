package com.monahub.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import javax.servlet.RequestDispatcher;
import javax.servlet.http.HttpServletRequest;

/**
 * 友好错误页。容器级 404/500（web.xml 的 error-page）也转发到这里。
 */
@Controller
public class ErrorController {

    @GetMapping("/error")
    public String error(HttpServletRequest request, Model model) {
        Object codeAttr = request.getAttribute(RequestDispatcher.ERROR_STATUS_CODE);
        int code = codeAttr instanceof Integer ? (Integer) codeAttr : 404;
        if (code < 100 || code > 599) {
            code = 500;
        }

        Object messageAttr = request.getAttribute(RequestDispatcher.ERROR_MESSAGE);
        String message = messageAttr instanceof String && !((String) messageAttr).isBlank()
                ? friendly(code)
                : friendly(code);

        model.addAttribute("code", code);
        model.addAttribute("message", message);
        return "error";
    }

    /** 把状态码翻译成一句人话；不把异常堆栈或内部路径暴露给用户。 */
    static String friendly(int code) {
        switch (code) {
            case 400:
                return "请求参数有误，请返回重试";
            case 403:
                return "没有权限访问该资源";
            case 404:
                return "页面或文件不存在";
            case 405:
                return "请求方式不被支持";
            case 413:
                return "上传内容超过大小限制";
            default:
                return code >= 500 ? "服务器开小差了，请稍后再试" : "请求无法完成";
        }
    }
}
