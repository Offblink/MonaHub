package com.monahub.interceptor;

import com.monahub.entity.SysUser;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 拦截器只做两件事：已登录放行、未登录 302 到 /login（带上下文路径）。
 * 放行清单在 WebConfig 里，这里验证拦截器本身。
 */
class LoginInterceptorTest {

    private final LoginInterceptor interceptor = new LoginInterceptor();

    @Test
    void unauthenticatedRequestRedirectsToLogin() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setContextPath("/monahub");
        MockHttpServletResponse response = new MockHttpServletResponse();

        assertFalse(interceptor.preHandle(request, response, new Object()));
        assertEquals("/monahub/login", response.getRedirectedUrl());
    }

    @Test
    void authenticatedRequestPasses() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setContextPath("/monahub");
        request.getSession().setAttribute(LoginInterceptor.SESSION_USER, new SysUser());
        MockHttpServletResponse response = new MockHttpServletResponse();

        assertTrue(interceptor.preHandle(request, response, new Object()));
        assertNull(response.getRedirectedUrl());
    }
}
