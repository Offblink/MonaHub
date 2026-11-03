package com.monahub.interceptor;

import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

/**
 * 统一登录校验：未登录访问受保护资源一律 302 回登录页。
 *
 * <p>放行路径不写在这里，而写在 {@link com.monahub.config.WebConfig#addInterceptors}
 * —— 拦截器只负责"拦"，"哪些不拦"由注册处集中声明，便于答辩时逐条指认。</p>
 */
public class LoginInterceptor implements HandlerInterceptor {

    /** 会话属性名：登录成功后放脱敏用户对象。 */
    public static final String SESSION_USER = "loginUser";

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
            throws Exception {
        HttpSession session = request.getSession(false);
        if (session != null && session.getAttribute(SESSION_USER) != null) {
            return true;
        }
        response.sendRedirect(request.getContextPath() + "/login");
        return false;
    }
}
