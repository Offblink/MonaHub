package com.monahub.config;

import com.monahub.aspect.WebLogAspect;
import com.monahub.interceptor.LoginInterceptor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.validation.Validator;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.config.annotation.CorsRegistry;

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
@EnableAspectJAutoProxy
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

    /**
     * 纯 Spring（非 Boot）下 @EnableWebMvc 不保证 @Valid 真的跑——
     * 注解在、校验静默失效（JFT-E2 实测坑），显式提供 JSR-303 校验器。
     */
    @Override
    public Validator getValidator() {
        return new LocalValidatorFactoryBean();
    }

    /** 跨域：只给 JSON API 开（JSP 页面同源不需要）。 */
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
                .allowedOriginPatterns("*")
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .allowCredentials(true)
                .maxAge(3600);
    }

    /**
     * 纯 Spring 的 Jackson 默认把 LocalDateTime 序列化成 [2026,11,3,11,38,30] 数组
     * （Spring Boot 才会自动关时间戳）——这里显式关掉，API 返回 ISO-8601 字符串。
     */
    @Override
    public void extendMessageConverters(
            java.util.List<org.springframework.http.converter.HttpMessageConverter<?>> converters) {
        for (org.springframework.http.converter.HttpMessageConverter<?> converter : converters) {
            if (converter instanceof org.springframework.http.converter.json.MappingJackson2HttpMessageConverter jackson) {
                jackson.getObjectMapper()
                        .disable(com.fasterxml.jackson.databind.SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
            }
        }
    }

    /**
     * 请求日志切面必须注册在子容器（与 Controller 同容器，AOP 代理才有效），
     * 所以走 @Bean 而不是让父容器的 component-scan 扫到它。
     */
    @Bean
    public WebLogAspect webLogAspect() {
        return new WebLogAspect();
    }
}
