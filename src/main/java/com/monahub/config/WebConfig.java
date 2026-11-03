package com.monahub.config;

import com.monahub.interceptor.LoginInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * SpringMVC 配置。
 *
 * <p>任务书 F1 明确要求拦截器"使用 WebMvcConfigurer#addInterceptors 注册，不使用 XML 配置"，
 * 所以这里用 @Configuration 类而不再写 {@code <mvc:interceptors>}。</p>
 *
 * <p>放行清单的设计依据：</p>
 * <ul>
 *   <li>{@code /login}：登录页与登录提交接口，未登录必须能访问；</li>
 *   <li>{@code /static/**}：CSS/JS/图标，登录页本身要用 —— 漏了会出现"登录后样式全丢"；</li>
 *   <li>{@code /error}：错误页转发，否则异常时会来回重定向；</li>
 *   <li>{@code /favicon.ico}：浏览器自动请求，不值得拦截。</li>
 * </ul>
 */
@Configuration
@EnableWebMvc
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new LoginInterceptor())
                .addPathPatterns("/**")
                .excludePathPatterns(
                        "/login",
                        "/static/**",
                        "/error",
                        "/favicon.ico");
    }
}
