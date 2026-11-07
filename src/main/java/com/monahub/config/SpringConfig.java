package com.monahub.config;

import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import com.zaxxer.hikari.HikariDataSource;
import org.mybatis.spring.SqlSessionFactoryBean;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.context.annotation.PropertySource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import javax.sql.DataSource;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * 父容器配置（实验二任务 3.2，全注解形态）——原 applicationContext.xml 的等价移植：
 * ① 配置源 ② HikariCP 数据源 ③ SqlSessionFactory（含 MyBatis-Plus 分页插件）
 * ④ Mapper 扫描 ⑤ 声明式事务 ⑥ 上传线程池 ⑦ AOP（下载日志切面的自动代理开关）。
 *
 * <p>扫描范围只含 service 与 aspect：Controller / 拦截器 / 异常出口属于子容器
 * （{@link SpringMvcConfig}），两边不重叠——重复扫描会让同一 Bean 注册两份、
 * AOP 双重代理。</p>
 */
@Configuration
@PropertySource(value = {"classpath:common/db.properties", "classpath:common/app.properties"},
        encoding = "UTF-8")
@ComponentScan(basePackages = {"com.monahub.service", "com.monahub.aspect"})
@EnableAspectJAutoProxy
@EnableTransactionManagement
@MapperScan("com.monahub.mapper")
public class SpringConfig {

    /** ① 数据源：参数全部来自 db.properties，禁止硬编码（口令文件不入库）。 */
    @Bean(destroyMethod = "close")
    public HikariDataSource dataSource(@Value("${jdbc.driver}") String driver,
                                       @Value("${jdbc.url}") String url,
                                       @Value("${jdbc.username}") String user,
                                       @Value("${jdbc.password}") String pwd) {
        HikariDataSource ds = new HikariDataSource();
        ds.setDriverClassName(driver);
        ds.setJdbcUrl(url);
        ds.setUsername(user);
        ds.setPassword(pwd);
        ds.setMaximumPoolSize(10);
        ds.setMinimumIdle(2);
        ds.setConnectionTimeout(10000);
        return ds;
    }

    /**
     * MyBatis-Plus 分页拦截器（3.5.5）：只用分页能力——给 XML 里的 select
     * 追加 LIMIT 与 count 查询，不引 BaseMapper、不改 XML 的 SQL 写法。
     * maxLimit=100 防 {@code ?size=100000} 一次拖库。
     */
    @Bean
    public MybatisPlusInterceptor mybatisPlusInterceptor() {
        PaginationInnerInterceptor page = new PaginationInnerInterceptor();
        page.setMaxLimit(100L);
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
        interceptor.addInnerInterceptor(page);
        return interceptor;
    }

    /** ② SqlSessionFactory：SQL 全部写在 classpath:mapper/*.xml 里。 */
    @Bean
    public SqlSessionFactoryBean sqlSessionFactory(DataSource dataSource,
                                                   MybatisPlusInterceptor mybatisPlusInterceptor)
            throws Exception {
        SqlSessionFactoryBean factory = new SqlSessionFactoryBean();
        factory.setDataSource(dataSource);
        factory.setMapperLocations(new PathMatchingResourcePatternResolver()
                .getResources("classpath:mapper/*.xml"));
        factory.setTypeAliasesPackage("com.monahub.entity");
        factory.setPlugins(mybatisPlusInterceptor);
        return factory;
    }

    /** ③ 声明式事务：{@code @Transactional}（rollbackFor=Exception）的执行者。 */
    @Bean
    public DataSourceTransactionManager transactionManager(DataSource dataSource) {
        return new DataSourceTransactionManager(dataSource);
    }

    /**
     * ④ 上传线程池：core=2 / max=8 / queue=32；队列满时由提交线程自己执行
     * （CallerRunsPolicy），拒绝策略上不丢文件、只是退化为串行。
     */
    @Bean
    public ThreadPoolTaskExecutor uploadExecutor(@Value("${upload.pool.core:2}") int core,
                                                 @Value("${upload.pool.max:8}") int max,
                                                 @Value("${upload.pool.queue:32}") int queue) {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(core);
        executor.setMaxPoolSize(max);
        executor.setQueueCapacity(queue);
        executor.setKeepAliveSeconds(60);
        executor.setThreadNamePrefix("upload-");
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        return executor;
    }
}
