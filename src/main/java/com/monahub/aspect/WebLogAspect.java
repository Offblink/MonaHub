package com.monahub.aspect;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;

/**
 * AOP 请求日志切面（实验二任务 6.4）：记录 Controller 层入参、耗时与异常。
 *
 * <p>必须与 Controller 同容器（SpringMVC 子容器）才生效——AOP 代理是容器内的，
 * 所以它由 {@link com.monahub.config.SpringMvcConfig} 以 @Bean 注册在子容器，
 * 而不是让父容器 {@code SpringConfig} 的 component-scan 扫到（父容器里没有 Controller 可切）。</p>
 */
@Aspect
public class WebLogAspect {

    private static final Logger log = LoggerFactory.getLogger(WebLogAspect.class);

    @Pointcut("execution(* com.monahub.controller..*(..))")
    public void controllerLayer() {
    }

    @Around("controllerLayer()")
    public Object around(ProceedingJoinPoint pjp) throws Throwable {
        String method = pjp.getSignature().toShortString();
        long start = System.currentTimeMillis();
        log.info("[REQ] {} 参数: {}", method, Arrays.toString(pjp.getArgs()));
        try {
            Object result = pjp.proceed();
            log.info("[RESP] {} 耗时: {}ms", method, System.currentTimeMillis() - start);
            return result;
        } catch (Throwable t) {
            log.error("[ERR] {} 异常: {} 耗时: {}ms", method, t.getMessage(),
                    System.currentTimeMillis() - start);
            throw t;
        }
    }
}
