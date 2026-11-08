package com.monahub.aspect;

import com.monahub.service.DownloadLogService;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * F5 下载日志切面：下载成功（FileService#afterDownload 正常返回）后写 download_log。
 *
 * <p>为什么切在 Service 而不是 Controller：</p>
 * <ul>
 *   <li>Service 在父容器，切面与它同容器，AOP 代理一定生效；</li>
 *   <li>参数（fileId / downloaderId / ip）已经是业务语义，不需要从 session 或 request 里绕；</li>
 *   <li>日志是"附带的账"，写失败只告警，绝不反向影响下载。</li>
 * </ul>
 */
@Aspect
@Component
public class DownloadLogAspect {

    private static final Logger log = LoggerFactory.getLogger(DownloadLogAspect.class);

    private final DownloadLogService downloadLogService;

    public DownloadLogAspect(DownloadLogService downloadLogService) {
        this.downloadLogService = downloadLogService;
    }

    @AfterReturning(
            // AOP绑定在哪都行，还可以传递参数
            pointcut = "execution(* com.monahub.service.FileService.afterDownload(..)) "
                    + "&& args(fileId, downloaderId, ip)")
    // 的确使用了上面三个参数
    public void record(Long fileId, Long downloaderId, String ip) {
        try {
            downloadLogService.record(fileId, downloaderId, ip);
        } catch (Exception e) {
            log.warn("写下载日志失败 fileId={} downloaderId={}", fileId, downloaderId, e);
        }
    }
}
