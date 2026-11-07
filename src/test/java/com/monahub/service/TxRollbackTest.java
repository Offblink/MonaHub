package com.monahub.service;

import com.monahub.entity.FileInfo;
import com.monahub.entity.SysUser;
import com.monahub.mapper.FileInfoMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 声明式事务回滚实证（实验二任务 6.5）。
 *
 * <p>本类<b>不加</b>类级 @Transactional：回滚边界完全由
 * {@link TransactionTemplate}（即 applicationContext.xml 装配的
 * DataSourceTransactionManager）控制，观察到的"行消失"就是真回滚，
 * 不是测试事务的假象。</p>
 */
@ExtendWith(SpringExtension.class)
@ContextConfiguration(locations = "classpath:spring/applicationContext.xml")
class TxRollbackTest {

    @Autowired
    private PlatformTransactionManager transactionManager;

    @Autowired
    private FileInfoMapper fileInfoMapper;

    @Autowired
    private UserService userService;

    @Test
    void insertThenExceptionRollsBackRow() {
        SysUser zhangsan = userService.login("zhangsan", "123456");
        assertNotNull(zhangsan, "需要先导入 sql/02_data.sql 的演示账号");

        FileInfo meta = newMeta(zhangsan.getId());
        TransactionTemplate template = new TransactionTemplate(transactionManager);

        // 事务内：INSERT 已执行 → 故意抛异常 → 整个事务回滚
        assertThrows(IllegalStateException.class, () -> template.execute(status -> {
            fileInfoMapper.insert(meta);
            // 主键已回填：说明 INSERT 真的执行过（不是没进事务）
            assertNotNull(meta.getId(), "INSERT 应已执行并回填主键");
            throw new IllegalStateException("故意抛出，验证回滚");
        }));

        assertNotNull(meta.getId());
        assertNull(fileInfoMapper.selectByIdAndUploader(meta.getId(), zhangsan.getId()),
                "异常抛出后行必须消失——回滚生效");

        // 对照组：同一事务内不抛异常 → 正常提交、查得到；随后清理
        FileInfo committed = newMeta(zhangsan.getId());
        template.executeWithoutResult(status -> fileInfoMapper.insert(committed));
        assertNotNull(fileInfoMapper.selectByIdAndUploader(committed.getId(), zhangsan.getId()),
                "不抛异常必须提交（对照组，证明上面的消失确实是回滚）");
        fileInfoMapper.softDelete(committed.getId(), zhangsan.getId());
    }

    @Test
    void transactionManagerBeanIsWired() {
        assertNotNull(transactionManager, "DataSourceTransactionManager 必须装配");
        assertTrue(transactionManager.getClass().getName().contains("DataSource"),
                "应为数据源事务管理器，实际: " + transactionManager.getClass().getName());
    }

    private FileInfo newMeta(Long uploaderId) {
        FileInfo meta = new FileInfo();
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        meta.setOriginName("回滚实证-" + suffix + ".pdf");
        meta.setStoreName(UUID.randomUUID().toString().replace("-", "") + ".pdf");
        meta.setFilePath("zhangsan/2026/11/" + meta.getStoreName());
        meta.setExt("pdf");
        meta.setFileSize(1024L);
        meta.setContentType("application/pdf");
        meta.setUploaderId(uploaderId);
        meta.setCreateTime(LocalDateTime.now());
        meta.setStatus(1);
        meta.setDownloadCount(0);
        return meta;
    }
}
