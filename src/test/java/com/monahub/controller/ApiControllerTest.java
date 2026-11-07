package com.monahub.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.monahub.entity.FileInfo;
import com.monahub.entity.SysUser;
import com.monahub.exception.RestGlobalExceptionHandler;
import com.monahub.interceptor.LoginInterceptor;
import com.monahub.service.FileService;
import com.monahub.service.StorageService;
import com.monahub.service.UserService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * /api/files 五件事的集成测试：真实 FileService + 真实 MySQL，
 * MockMvc standalone 只负责 HTTP 层（拦截器 / 校验器 / 统一异常出口全装配）。
 *
 * <p>测试类带 @Transactional：写库操作用例结束回滚，不污染演示数据；
 * create 落盘的磁盘文件由 {@link #cleanupDisk()} 按 filePath 逐个清理。</p>
 */
@ExtendWith(SpringExtension.class)
@ContextConfiguration(locations = "classpath:spring/applicationContext.xml")
@Transactional
class ApiControllerTest {

    private static final ObjectMapper JSON = new ObjectMapper();

    @Autowired
    private FileService fileService;

    @Autowired
    private StorageService storageService;

    @Autowired
    private UserService userService;

    private MockMvc mockMvc;
    private SysUser zhangsan;
    private SysUser lisi;
    private org.springframework.mock.web.MockHttpSession session;
    private final List<String> diskFiles = new ArrayList<>();

    @BeforeEach
    void setUp() {
        zhangsan = userService.login("zhangsan", "123456");
        lisi = userService.login("lisi", "123456");
        session = new org.springframework.mock.web.MockHttpSession();
        session.setAttribute(LoginInterceptor.SESSION_USER, zhangsan);

        mockMvc = MockMvcBuilders.standaloneSetup(new ApiController(fileService))
                .addInterceptors(new LoginInterceptor())
                .setControllerAdvice(new RestGlobalExceptionHandler())
                .setValidator(new LocalValidatorFactoryBean())
                .build();
    }

    @AfterEach
    void cleanupDisk() {
        diskFiles.forEach(storageService::deleteQuietly);
        diskFiles.clear();
    }

    // ---------- 认证 ----------

    @Test
    void unauthenticatedApiReturns401JsonNotRedirect() throws Exception {
        MvcResult result = mockMvc.perform(get("/api/files"))
                .andExpect(status().isUnauthorized())
                .andReturn();
        String body = result.getResponse().getContentAsString(StandardCharsets.UTF_8);
        assertTrue(body.contains("\"code\":401"));
        assertTrue(result.getResponse().getRedirectedUrl() == null,
                "API 未登录不能 302，必须 401 JSON");
    }

    // ---------- 列表 / 单查 ----------

    @Test
    void listReturnsPagedRecordsAsResultEnvelope() throws Exception {
        fileService.saveMeta(newMeta("API列表.pdf", "pdf"));

        MvcResult result = mockMvc.perform(get("/api/files")
                        .param("page", "1").param("size", "5")
                        .session(session))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode data = dataOf(result);
        assertEquals(200, dataOf(result).get("code").asInt());
        assertTrue(data.get("data").get("total").asLong() >= 1);
        assertTrue(data.get("data").get("records").isArray());
    }

    @Test
    void getReturnsSingleRecordOr404() throws Exception {
        FileInfo meta = newMeta("API单查.pdf", "pdf");
        fileService.saveMeta(meta);

        MvcResult ok = mockMvc.perform(get("/api/files/" + meta.getId()).session(session))
                .andExpect(status().isOk())
                .andReturn();
        assertEquals(meta.getId().longValue(), dataOf(ok).get("data").get("id").asLong());

        mockMvc.perform(get("/api/files/999999999").session(session))
                .andExpect(status().isNotFound());
    }

    @Test
    void pageBoundaryIsClampedNotRejected() throws Exception {
        // 第 0 页、size=0：指导书分页边界用例——夹紧而非报错
        mockMvc.perform(get("/api/files").param("page", "0").param("size", "0")
                        .session(session))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/files").param("size", "99999")
                        .session(session))
                .andExpect(status().isOk());
    }

    // ---------- 新增 ----------

    @Test
    void createStoresFileToDiskAndDatabase() throws Exception {
        String name = "API新增-" + UUID.randomUUID().toString().substring(0, 8) + ".pdf";
        MvcResult result = mockMvc.perform(post("/api/files")
                        .contentType("application/json")
                        .session(session)
                        .content("{\"name\":\"" + name + "\",\"content\":\"" + b64("hello monahub api") + "\"}"))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode data = dataOf(result);
        assertEquals(200, data.get("code").asInt());
        long id = data.get("data").get("id").asLong();
        String filePath = data.get("data").get("filePath").asText();
        diskFiles.add(filePath);

        // 库里查得到、磁盘上真实存在
        assertEquals(name, fileService.get(id, zhangsan.getId()).getOriginName());
        assertTrue(storageService.exists(filePath), "create 必须真实落盘");
    }

    @Test
    void createRejectsBlankNameViaJsR303() throws Exception {
        MvcResult result = mockMvc.perform(post("/api/files")
                        .contentType("application/json")
                        .session(session)
                        .content("{\"name\":\"  \",\"content\":\"aGVsbG8=\"}"))
                .andExpect(status().isBadRequest())
                .andReturn();
        assertEquals(400, dataOf(result).get("code").asInt());
        assertTrue(dataOf(result).get("message").asText().contains("文件名"));
    }

    @Test
    void createRejectsBadBase64AndDisallowedExt() throws Exception {
        mockMvc.perform(post("/api/files")
                        .contentType("application/json")
                        .session(session)
                        .content("{\"name\":\"a.pdf\",\"content\":\"!!!not-base64!!!\"}"))
                .andExpect(status().isBadRequest());

        mockMvc.perform(post("/api/files")
                        .contentType("application/json")
                        .session(session)
                        .content("{\"name\":\"virus.exe\",\"content\":\"aGVsbG8=\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createRejectsDuplicateContentWith409() throws Exception {
        String content = b64("duplicate-content-" + UUID.randomUUID());
        String name1 = "去重A-" + UUID.randomUUID().toString().substring(0, 8) + ".pdf";
        String name2 = "去重B-" + UUID.randomUUID().toString().substring(0, 8) + ".pdf";

        MvcResult first = mockMvc.perform(post("/api/files")
                        .contentType("application/json")
                        .session(session)
                        .content("{\"name\":\"" + name1 + "\",\"content\":\"" + content + "\"}"))
                .andExpect(status().isOk())
                .andReturn();
        diskFiles.add(dataOf(first).get("data").get("filePath").asText());

        MvcResult second = mockMvc.perform(post("/api/files")
                        .contentType("application/json")
                        .session(session)
                        .content("{\"name\":\"" + name2 + "\",\"content\":\"" + content + "\"}"))
                .andExpect(status().isConflict())
                .andReturn();
        assertTrue(dataOf(second).get("message").asText().contains("重复"));
    }

    // ---------- 修改 ----------

    @Test
    void putRenamesButKeepsExtension() throws Exception {
        FileInfo meta = newMeta("API旧名.pdf", "pdf");
        fileService.saveMeta(meta);

        mockMvc.perform(put("/api/files")
                        .contentType("application/json")
                        .session(session)
                        .content("{\"id\":" + meta.getId() + ",\"newName\":\"API新名\"}"))
                .andExpect(status().isOk());
        assertEquals("API新名.pdf", fileService.get(meta.getId(), zhangsan.getId()).getOriginName());

        // 改扩展名 → 400
        MvcResult wrongExt = mockMvc.perform(put("/api/files")
                        .contentType("application/json")
                        .session(session)
                        .content("{\"id\":" + meta.getId() + ",\"newName\":\"伪装.exe\"}"))
                .andExpect(status().isBadRequest())
                .andReturn();
        assertEquals(400, dataOf(wrongExt).get("code").asInt());

        // 缺 id（JSR-303 @NotNull）→ 400
        mockMvc.perform(put("/api/files")
                        .contentType("application/json")
                        .session(session)
                        .content("{\"newName\":\"没有id.pdf\"}"))
                .andExpect(status().isBadRequest());

        // 他人的文件 → 404
        org.springframework.mock.web.MockHttpSession lisiSession =
                new org.springframework.mock.web.MockHttpSession();
        lisiSession.setAttribute(LoginInterceptor.SESSION_USER, lisi);
        mockMvc.perform(put("/api/files")
                        .contentType("application/json")
                        .session(lisiSession)
                        .content("{\"id\":" + meta.getId() + ",\"newName\":\"抢改.pdf\"}"))
                .andExpect(status().isNotFound());
    }

    // ---------- 删除 ----------

    @Test
    void deleteSingleHidesRecord() throws Exception {
        FileInfo meta = newMeta("API删除.pdf", "pdf");
        fileService.saveMeta(meta);

        mockMvc.perform(delete("/api/files/" + meta.getId()).session(session))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/files/" + meta.getId()).session(session))
                .andExpect(status().isNotFound());
    }

    @Test
    void batchDeleteRemovesAllWhenAllOwned() throws Exception {
        FileInfo a = newMeta("批量A.pdf", "pdf");
        FileInfo b = newMeta("批量B.pdf", "pdf");
        fileService.saveMeta(a);
        fileService.saveMeta(b);

        MvcResult result = mockMvc.perform(delete("/api/files")
                        .contentType("application/json")
                        .session(session)
                        .content("[" + a.getId() + "," + b.getId() + "]"))
                .andExpect(status().isOk())
                .andReturn();
        assertEquals(2, dataOf(result).get("data").asInt());

        mockMvc.perform(get("/api/files/" + a.getId()).session(session))
                .andExpect(status().isNotFound());
    }

    @Test
    void batchDeleteWithForeignIdDeletesNothing() throws Exception {
        FileInfo mine = newMeta("我自己的.pdf", "pdf");
        FileInfo foreign = newMeta("别人的.pdf", "pdf");
        foreign.setUploaderId(lisi.getId());
        fileService.saveMeta(mine);
        fileService.saveMeta(foreign);

        // 混入他人 id → 404，且整批不动（先全量校验再删）
        mockMvc.perform(delete("/api/files")
                        .contentType("application/json")
                        .session(session)
                        .content("[" + mine.getId() + "," + foreign.getId() + "]"))
                .andExpect(status().isNotFound());

        assertEquals(mine.getOriginName(),
                fileService.get(mine.getId(), zhangsan.getId()).getOriginName(),
                "校验失败必须整批回滚，不能删一半");

        // 空数组 → 400
        mockMvc.perform(delete("/api/files")
                        .contentType("application/json")
                        .session(session)
                        .content("[]"))
                .andExpect(status().isBadRequest());
    }

    // ---------- 工具 ----------

    private static JsonNode dataOf(MvcResult result) throws Exception {
        return JSON.readTree(result.getResponse().getContentAsString(StandardCharsets.UTF_8));
    }

    private static String b64(String text) {
        return java.util.Base64.getEncoder()
                .encodeToString(text.getBytes(StandardCharsets.UTF_8));
    }

    private FileInfo newMeta(String originName, String ext) {
        FileInfo meta = new FileInfo();
        meta.setOriginName(originName);
        meta.setStoreName(UUID.randomUUID().toString().replace("-", "") + "." + ext);
        meta.setFilePath("zhangsan/2026/11/" + meta.getStoreName());
        meta.setExt(ext);
        meta.setFileSize(1024L);
        meta.setContentType("application/pdf");
        meta.setUploaderId(zhangsan.getId());
        meta.setCreateTime(java.time.LocalDateTime.now());
        meta.setStatus(1);
        meta.setDownloadCount(0);
        return meta;
    }
}
