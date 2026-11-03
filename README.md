# MonaHub · 资料提交台

基于 **Spring + Spring MVC + MyBatis（SSM）** 的资料上传下载系统，对应《SSM 实训案例任务书》V2.0。
在任务书必做项（登录 / 拦截器 / 上传 / 列表 / 下载）之上，实现了老师口头加料与 F5：

- 选择即上传：同时多文件，服务端**有界线程池并发落盘**，逐文件独立结果与失败原因
- 文件按 **文档 / 图片 / 压缩包** 分类筛选 + 文件名搜索 + **分页**（MyBatis-Plus 分页插件，SQL 仍在 XML）
- 上传者可**冻结 / 解冻 / 重命名 / 删除**自己的文件（删除为逻辑删除）
- **下载次数统计** + AOP 下载日志（F5）
- 口令使用 **BCrypt**（自带随机盐），库里无明文
- 深色 / 浅色主题切换（偏好记忆，首屏无闪烁）

界面为 JSP + JSTL 手写，视觉参考 `Gasp-Design` 的暗色高对比与绿/蓝/紫渐变；
GSAP 3.12.7 已 vendored 到 `static/vendor/`，**运行期不依赖任何 CDN**，断网可演示。

---

## 1. 技术栈

| 层 | 选型 | 版本 |
|---|---|---|
| JDK | OpenJDK | 17 |
| 框架 | Spring（IoC / MVC / JDBC / TX / AOP） | 5.3.31 |
| 持久层 | MyBatis + XML 映射 | 3.5.15 / mybatis-spring 2.0.7 |
| 分页 | MyBatis-Plus PaginationInnerInterceptor（仅插件，不用 BaseMapper） | 3.5.5 / jsqlparser 4.6 |
| 口令 | Spring Security Crypto · BCryptPasswordEncoder | 5.7.11 |
| 连接池 | HikariCP | 4.0.3 |
| 数据库 | MySQL（utf8mb4） | 5.7+（本机 9.3 实测） |
| 视图 | JSP + JSTL | servlet-api 4.0.1 / jstl 1.2 |
| 上传 | commons-fileupload（CommonsMultipartResolver） | 1.5 |
| 构建 | Maven（war） | 3.9.x |
| 服务器 | Tomcat | 9.x（**不能**用 Tomcat 10+，jakarta 命名空间不兼容） |
| 前端 | 原生 CSS/JS + GSAP 3.12.7（本地） | — |

> 没有使用 Spring Boot：启动、装配、DispatcherServlet 全部按 SSM 传统方式显式配置。

---

## 2. 目录结构

```
MonaHub/
├─ pom.xml                     # war + 依赖版本 + cargo 本地 Tomcat 启动
├─ sql/
│  ├─ 01_schema.sql            # 建库建表（sys_user / file_info / download_log）
│  ├─ 02_data.sql              # 演示账号（BCrypt 哈希，可重复执行）
│  └─ 03_migrate_bcrypt_soft_delete.sql  # 旧库增量迁移（已有库才需要）
├─ docs/
│  └─ SSM实训-设计方案与规格.md  # 设计规格（动手前定稿）
└─ src/
   ├─ main/java/com/monahub/
   │  ├─ config/WebConfig.java          # WebMvcConfigurer#addInterceptors 注册拦截器
   │  ├─ interceptor/LoginInterceptor   # 未登录 302 /login
   │  ├─ controller/                    # LoginController / FileController / ErrorController
   │  ├─ service/                       # User / File / Storage / DownloadLog
   │  ├─ mapper/                        # MyBatis 接口（@Mapper）
   │  ├─ entity/ dto/ util/
   │  ├─ aspect/DownloadLogAspect       # F5：下载成功后写 download_log
   │  └─ exception/                     # BusinessException + 全局异常处理
   ├─ main/resources/
   │  ├─ spring/applicationContext.xml  # 父容器：数据源 / SqlSessionFactory / 事务 / 线程池
   │  ├─ spring/spring-mvc.xml          # 子容器：Controller / 静态资源 / 视图 / 上传解析器
   │  ├─ mapper/*.xml                   # 全部 SQL
   │  └─ common/app.properties          # 上传目录、白名单、大小、线程池
   ├─ main/webapp/
   │  ├─ WEB-INF/web.xml
   │  ├─ WEB-INF/views/{login,files,error}.jsp
   │  └─ static/{css,js,vendor/gsap.min.js}
   └─ test/java/com/monahub/            # 单元 / 集成测试
```

---

## 3. 启动步骤

### 3.1 前置

1. **JDK 17**（本项目 `maven.compiler.release=17`）
2. **Maven 3.9+**
3. **MySQL 5.7+ 已启动**
4. **Tomcat 9**（可选：本机已装时 cargo 直接使用，不下载）

### 3.2 建库与演示账号

```bash
mysql -u root -p < sql/01_schema.sql
mysql -u root -p < sql/02_data.sql
```

### 3.3 数据库口令（不入库）

复制模板并填入本机口令：

```bash
cp src/main/resources/common/db.properties.example src/main/resources/common/db.properties
# 编辑 db.properties 的 jdbc.password
```

`db.properties` 已在 `.gitignore` 中；仓库里只有 `CHANGE_ME` 的模板 —— 真实口令绝不入库。

### 3.4 启动

```bash
# 方式 A：cargo 使用本机 Tomcat（pom 里 tomcat.home 指向本机安装），端口 18080
mvn -Prun package cargo:run

# 方式 B：使用别的 Tomcat 9 —— 把 target/monahub.war 丢进 webapps/ 即可
mvn clean package
```

访问：**http://127.0.0.1:18080/monahub/**

> 端口被占用时可覆盖：`mvn -Prun package cargo:run -Dcargo.servlet.port=8081`
> `cargo:run` 本身不触发打包，命令里必须先 `package`。

### 3.5 演示账号

| 账号 | 密码 | 姓名 |
|---|---|---|
| `zhangsan` | `123456` | 张三 |
| `lisi` | `123456` | 李四 |

库里只存 BCrypt 哈希（形如 `$2a$10$...`，哈希自带随机盐），无任何明文口令。
`salt` 列保留为任务书表结构的兼容位（写空串），不再单独使用。

---

## 4. 功能与验收对照（任务书四条）

| 验收现象 | 本实现 |
|---|---|
| ① 登录 → 上传 PDF → 磁盘出现文件、`file_info` 新增记录、列表显示 | 登录后拖入 PDF，落盘 `upload/zhangsan/<年>/<月>/<uuid>.pdf`，列表即时可见 |
| ② 未登录访问上传页被重定向到登录页 | `LoginInterceptor` 302 → `/login`（放行清单见下） |
| ③ 下载正常、中文文件名不乱码 | `Content-Disposition: attachment; filename*=UTF-8''...` + ASCII 退化名 |
| ④ 换账号看不到也下载不了上一个账号的文件 | 列表恒定 `uploader_id` 过滤；下载 `selectByIdAndUploader` 查不到即 404「文件不存在或无权访问」 |

### 4.1 拦截器放行清单（评分点）

在 `WebConfig#addInterceptors` 注册，**不使用 XML**：

```java
registry.addInterceptor(new LoginInterceptor())
        .addPathPatterns("/**")
        .excludePathPatterns("/login", "/static/**", "/error", "/favicon.ico");
```

`/static/**` 必须在列，否则登录页与工作台的 CSS/JS 会被拦掉（"登录后样式全丢"的经典坑）。

### 4.2 上传的服务端三要点

1. **大小**：`CommonsMultipartResolver` 单文件 10MB + Service 二次校验（不信任前端）。
2. **类型**：扩展名白名单 `pdf/jpg/jpeg/png/doc/docx/zip`，来自 `app.properties`。
3. **重命名**：`UUID + 扩展名` 落盘；用户原始名只进数据库 `origin_name`。

目录规则：`<upload.root>/<username>/<yyyy>/<MM>/`，避免单目录堆积。
`file_path` 存**相对路径**，绝对根只来自配置。

### 4.3 多文件并发

- 每个文件一个 `Callable`，进 `ThreadPoolTaskExecutor`（core 2 / max 8 / queue 32 / `CallerRunsPolicy`）。
- 每文件独立事务、独立结果；落盘成功但入库失败时删除孤文件（补偿）。
- 页面逐条显示成功/失败原因；浏览器侧每个文件一个 XHR，可看到各自进度条。

### 4.4 重命名与逻辑删除

- **重命名**：`POST /files/{id}/rename`，只改展示名 `origin_name`，磁盘 UUID 名不动；
  必须保持原扩展名（只给主名会自动补），杜绝"名字是 .pdf、内容其实是 exe"的欺骗面；仅本人可改。
- **逻辑删除**：`POST /files/{id}/delete`，`deleted = 1`；记录与磁盘文件保留（管理员可恢复/清理），
  但列表、统计、下载、改名全部过滤掉，删除后直连下载 id 返回 404。
- 两者都带 `uploader_id` 条件，改不到/删不到别人的记录。

### 4.5 下载安全

```
fileId → WHERE id=? AND uploader_id=? → 冻结判断 → 磁盘存在判断 → canonical 前缀校验 → 流式输出
```

- 磁盘路径永远从库中取，**不接收客户端传入的文件名/路径**；
- `root.resolve(path).normalize().startsWith(root)` 阻断 `../` 逃逸；
- 冻结 / 缺失 / 越权分别落到 403 / 404 友好页，不打印堆栈；
- 计数与日志失败只告警，不影响已发出的文件。

---

## 5. 接口一览

| 方法 | 路径 | 说明 |
|---|---|---|
| GET | `/login` | 登录页（放行） |
| POST | `/login` | 登录提交（放行） |
| POST | `/logout` | 退出 |
| GET | `/files` | 分页列表：`category=all\|doc\|image\|archive`、`q=关键字`、`page`、`size`（≤50） |
| POST | `/files/upload` | 多文件上传（字段 `files`）；XHR 时返回 JSON 逐条结果 |
| POST | `/files/{id}/freeze` | `frozen=1` 冻结 / `0` 解冻（仅本人） |
| POST | `/files/{id}/rename` | 重命名展示名（`newName`，扩展名不可改） |
| POST | `/files/{id}/delete` | 逻辑删除（仅本人） |
| GET | `/files/{id}/download` | 下载（仅本人、非冻结、未删除） |
| GET | `/error` | 友好错误页（容器 404/500 也转发到这里） |

---

## 6. 数据库设计

| 表 | 关键字段 | 说明 |
|---|---|---|
| `sys_user` | `username`(唯一)、`password`(72)、`salt`(32，兼容位)、`real_name` | 口令为 BCrypt 哈希（自带盐），`salt` 写空串 |
| `file_info` | `origin_name`、`store_name`、`file_path`、`ext`、`file_size`、`content_type`、`uploader_id`、`create_time`、`status`、`deleted`、`download_count` | `status`：1 正常 / 0 冻结；`deleted`：0 正常 / 1 逻辑删除；`idx_uploader_time` 支撑列表 |
| `download_log` | `file_id`、`downloader_id`、`download_time`、`ip` | F5，AOP 写入 |

磁盘存文件、数据库存元信息；**没有 BLOB 字段**。

思考题（答辩可讲）：`file_info(uploader_id)` 索引让"我的文件"列表与归属校验都走索引，避免全表扫描；
`origin_name` 必须入库的原因是磁盘名已改成 UUID，用户可读名、下载时的命名只能靠元信息还原。

---

## 7. 主题与前端

- 深色为基准，`:root[data-theme="light"]` 覆盖设计令牌；首屏由 `<head>` 内联脚本读取
  `localStorage['monahub:theme']`（无记录时跟随系统）先落属性，刷新不闪白。
- 右上角按钮切换，支持 View Transitions 圆形涟漪；`prefers-reduced-motion` 下直接切换。
- GSAP 只做四件事：标题逐字入场、背景光球视差、统计数字递增、列表行 stagger；
  库缺席或减少动效时页面退化为静态但完全可用。
- 上传交互：**选择/拖入即上传**（每个文件一个 XHR，进度与结果逐条显示），没有"开始上传"按钮；
  无脚本时退化为普通表单提交（服务端行为完全一致）。

---

## 8. 测试与端到端验证

```bash
# 单元 + 集成测试（集成测试连本机 MySQL submission_hub，事务回滚不污染数据）
mvn test
```

覆盖：BCrypt 哈希与校验、文件名纯函数与分类、存储 UUID/分目录/路径穿越、拦截器重定向、
登录成功/失败、列表归属隔离、分类过滤、分页（总数/越界回退/每页上限）、
重命名（补扩展名/禁改扩展名/越权）、逻辑删除、冻结与越权下载。

### 8.1 curl 端到端（四条验收的脚本化复验）

```bash
BASE=http://127.0.0.1:18080/monahub
JAR=C:/tmp/scratch/monahub-cookie.txt

# ① 登录（302 → /files）
curl -s -i -c "$JAR" -d "username=zhangsan&password=123456" "$BASE/login" | head -5

# ② 上传中文名 PDF（成功）+ exe（应被服务端拒绝）
curl -s -b "$JAR" -H "X-Requested-With: XMLHttpRequest" \
     -F "files=@作业.pdf;filename=结业作业.pdf" -F "files=@virus.exe;filename=virus.exe" \
     "$BASE/files/upload"

# ③ 列表（应含中文名）+ 分类过滤
curl -s -b "$JAR" "$BASE/files?category=doc" | grep -o "结业作业.pdf"

# ④ 下载（检查 filename* 与字节）
curl -s -D - -o downloaded.pdf -b "$JAR" "$BASE/files/<id>/download" | grep -i content-disposition
cmp 作业.pdf downloaded.pdf && echo BYTES_OK
```

### 8.2 人工验收

浏览器 1440 / 390 两档：登录页（深/浅）、工作台（有数据/空态）、冻结模态、403 页；
上传中文名文件后下载，确认文件名不乱码；换 `lisi` 登录确认列表为空、直连他人 id 得到 404。

本机实测记录（2026-11-03）：

| 项 | 结果 |
|---|---|
| `mvn test` | **23 / 23 通过**（单元 + MySQL 集成） |
| 未登录 `/files` | 302 → `/monahub/login` |
| 中文名 PDF + PNG 并发上传 | 2 成功入库落盘；`.exe` 被服务端拒绝并给出原因 |
| 下载 | 200，`filename*=UTF-8''…`，字节与源文件一致，`download_count` +1，`download_log` +1 |
| 冻结后下载 | 403「该文件已被上传者冻结，暂不可下载」 |
| 换 `lisi` 直接访问他人 id | 404「文件不存在或无权访问」 |
| 11MB 文件 | 413「文件太大了：单个文件不超过 10MB，一次最多 50MB」 |
| 分页 | 8 条数据 → 「第 1 / 2 页」，每页 6 条；`page=99` 回退到第 2 页；`size=100000` 夹紧到 50 |
| 重命名 | 只填主名自动补 `.pdf`；改成 `.exe` → 400「只能修改文件名，不能修改扩展名」；他人 → 404 |
| 逻辑删除 | 列表/统计立即消失（8→7），原 id 下载返回 404；他人删除 → 404 |
| 选择即上传 | 选中文件后无需按钮，自动上传、显示进度，成功后自动刷新列表 |

### 8.3 界面截图（`docs/screenshots/`）

| 文件 | 场景 |
|---|---|
| `login-dark.webp` / `login-light.webp` | 登录页深浅两主题 |
| `dashboard-dark.webp` / `dashboard-light.webp` | 工作台（统计、上传区、分类胶囊、文件列表） |
| `dashboard-mobile-light.webp` | 390px 移动端（无横向溢出） |
| `rename-modal-light.webp` | 重命名模态（输入框预填、扩展名提示） |
| `delete-modal-light.webp` | 逻辑删除确认模态 |
| `error-404-light.webp` | 越权/缺失的友好错误页 |

---

## 9. 配置项（`common/app.properties`，可入库）

| 键 | 默认 | 说明 |
|---|---|---|
| `upload.root` | 空 → `${user.home}/SubmissionHub/upload` | 绝对根目录只来自配置 |
| `upload.max-size` | `10485760` | 单文件上限（字节） |
| `upload.allowed-ext` | `pdf,jpg,jpeg,png,doc,docx,zip` | 白名单；改这里时同步 `FileCategory` |
| `upload.pool.core/max/queue` | `2 / 8 / 32` | 并发落盘线程池 |

---

## 10. 已知边界与课后拓展

未做（任务书列为课后拓展或明确删减）：

- MD5 去重与"重复上传"提示
- ZIP 打包、完整操作审计
- 注册 / 改密 / 角色权限
- 逻辑删除后的回收站/物理清盘（记录与磁盘文件目前保留，由管理员清理）

安全说明：口令使用 BCrypt（cost 10）；生产可提高 cost 并配合 HTTPS 传输。
逻辑删除不是"物理删除"，敏感数据仍建议配套保留期与清理任务。

---

## 11. 常见问题

| 现象 | 原因 / 处理 |
|---|---|
| 启动报 `ClassNotFoundException: javax.servlet...` | 用了 Tomcat 10+；必须 Tomcat 9（jakarta 不兼容） |
| 端口 18080 被占 | `-Dcargo.servlet.port=8081` |
| 上传中文名变 `???` | 确认 `CharacterEncodingFilter(forceEncoding=true)` 与 multipart `defaultEncoding=UTF-8` 都在 |
| `Access denied for user` | `db.properties` 口令未填或为空（该文件故意不入库） |
| 登录后样式全丢 | 拦截器放行清单漏了 `/static/**` |
| 上传 .exe 成功 | 白名单被改动或前端校验替代了服务端校验 —— 服务端必须兜底 |
| 下载他人文件返回 200 | 危险信号：检查是否绕过 `selectByIdAndUploader` —— 属一票否决缺陷 |
