package com.monahub.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.monahub.aspect.WebLogAspect;
import com.monahub.interceptor.LoginInterceptor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.validation.Validator;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;
import org.springframework.web.multipart.commons.CommonsMultipartResolver;
import org.springframework.web.servlet.ViewResolver;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.view.InternalResourceViewResolver;

import java.util.List;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;

/**
 * SpringMVC 子容器配置（实验二任务 3.3，全注解形态）——原 spring-mvc.xml 的等价移植，
 * 同时承担指导书示例里 SpringMvcConfig 的角色。
 *
 * <p>任务书 F1 要求拦截器"使用 WebMvcConfigurer#addInterceptors 注册，不使用 XML"，
 * 所以由本类的 {@code @EnableWebMvc} 驱动 MVC 装配并收集 WebMvcConfigurer。</p>
 */
@Configuration
@EnableWebMvc
@EnableAspectJAutoProxy
@ComponentScan(basePackages = {"com.monahub.controller", "com.monahub.interceptor",
        "com.monahub.exception"})
public class SpringMvcConfig implements WebMvcConfigurer {

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
    public void extendMessageConverters(List<HttpMessageConverter<?>> converters) {
        for (HttpMessageConverter<?> converter : converters) {
            if (converter instanceof MappingJackson2HttpMessageConverter jackson) {
                jackson.getObjectMapper().disable(
                        com.fasterxml.jackson.databind.SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
            }
        }
    }

    /** 静态资源（原 &lt;mvc:resources mapping="/static/**" location="/static/"/&gt;）。 */
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/static/**").addResourceLocations("/static/");
    }

    /**
     * 请求日志切面必须注册在子容器（与 Controller 同容器，AOP 代理才有效），
     * 所以走 @Bean 而不是让父容器的 component-scan 扫到它。
     */
    @Bean
    public WebLogAspect webLogAspect() {
        return new WebLogAspect();
    }

    /** 上传接口的 JSON 响应（XHR 形态）序列化用（FileController 注入）。 */
    @Bean
    public ObjectMapper objectMapper() {
        return new ObjectMapper();
    }

    /** JSP 视图：Controller 返回 "files" → /WEB-INF/views/files.jsp（WEB-INF 不可直接访问）。 */
    @Bean
    public ViewResolver viewResolver() {
        InternalResourceViewResolver resolver = new InternalResourceViewResolver();
        resolver.setPrefix("/WEB-INF/views/");
        resolver.setSuffix(".jsp");
        return resolver;
    }

    /**
     * 文件上传解析器：Bean 名必须是 multipartResolver。
     * <ul>
     *   <li>单文件 10MB——与 app.properties 的 upload.max-size 保持一致，改限值两处同步；</li>
     *   <li>一次请求总量 50MB（多文件批量）；</li>
     *   <li>resolveLazily=true：超限异常在参数解析阶段抛出，可被全局异常出口友好处理。</li>
     * </ul>
     */
    @Bean
    public CommonsMultipartResolver multipartResolver() {
        CommonsMultipartResolver resolver = new CommonsMultipartResolver();
        resolver.setDefaultEncoding("UTF-8");
        resolver.setMaxUploadSizePerFile(10485760);
        resolver.setMaxUploadSize(52428800);
        resolver.setResolveLazily(true);
        return resolver;
    }
}
