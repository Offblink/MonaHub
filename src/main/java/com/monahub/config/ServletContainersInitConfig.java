package com.monahub.config;

import org.springframework.web.filter.CharacterEncodingFilter;
import org.springframework.web.servlet.support.AbstractAnnotationConfigDispatcherServletInitializer;

import javax.servlet.Filter;

/**
 * Web 容器初始化（实验二任务 3.1，全注解形态）——取代原 web.xml 中的
 * ContextLoaderListener / DispatcherServlet / CharacterEncodingFilter 三段声明。
 *
 * <p>Servlet 3.0+ 由容器经 SPI 发现（spring-web 的 ServletContainerInitializer），
 * web.xml 只保留 error-page / session-config 这类 Servlet 规范仅有的声明式能力。</p>
 */
public class ServletContainersInitConfig extends AbstractAnnotationConfigDispatcherServletInitializer {

    /** 父容器：Service / Mapper / 数据源 / 事务 / 上传线程池（原 applicationContext.xml）。 */
    @Override
    protected Class<?>[] getRootConfigClasses() {
        return new Class<?>[]{SpringConfig.class};
    }

    /** 子容器：Controller / 拦截器 / 异常出口（原 spring-mvc.xml）。 */
    @Override
    protected Class<?>[] getServletConfigClasses() {
        return new Class<?>[]{SpringMvcConfig.class};
    }

    @Override
    protected String[] getServletMappings() {
        return new String[]{"/"};
    }

    /**
     * 编码过滤器：只强制请求编码（上传表单里的中文文件名不乱码），
     * 不强制响应——否则下载接口的 application/pdf 会被追加 {@code ;charset=UTF-8}。
     */
    @Override
    protected Filter[] getServletFilters() {
        CharacterEncodingFilter filter = new CharacterEncodingFilter();
        filter.setEncoding("UTF-8");
        filter.setForceRequestEncoding(true);
        filter.setForceResponseEncoding(false);
        return new Filter[]{filter};
    }
}
