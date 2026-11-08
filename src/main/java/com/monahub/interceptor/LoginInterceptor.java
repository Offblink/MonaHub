package com.monahub.interceptor;

import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

/**
 * 统一登录校验：未登录访问受保护资源一律 302 回登录页。
 *
 * <p>放行路径不写在这里，而写在 {@link com.monahub.config.SpringMvcConfig#addInterceptors}
 * —— 拦截器只负责"拦"，"哪些不拦"由注册处集中声明</p>
 */
public class LoginInterceptor implements HandlerInterceptor {

    /** 会话属性名：登录成功后放脱敏用户对象。 */
    public static final String SESSION_USER = "loginUser";

    @Override
    // 拿到当前会话的状态，在处理之前拦截非法会话，重定向到登录界面
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
            throws Exception {
        HttpSession session = request.getSession(false); // 获取当前对话

        // 当前会话不空，且属性也不空
        if (session != null && session.getAttribute(SESSION_USER) != null) {
            return true;
        }

        // JSON API（/api/**）未登录回 401 JSON，前端 fetch 直接读 code；
        // 页面流程保持 302 回登录页。路径判断只做前缀比较，不解释请求体。
        String uri = request.getRequestURI();
        String contextPath = request.getContextPath();

        // 三元表达式：uri为空->路径为空字符，uri不空->路径为uri
        String path = uri == null ? "" : uri;

        if (contextPath != null && !contextPath.isEmpty() && path.startsWith(contextPath)) {
            path = path.substring(contextPath.length());
        }
        if (path.startsWith("/api/")) {
            response.setStatus(401);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"code\":401,\"message\":\"未登录\"}");
            return false;
        }

        // 重定向到登录界面
        response.sendRedirect(request.getContextPath() + "/login");
        return false;
    }
}
