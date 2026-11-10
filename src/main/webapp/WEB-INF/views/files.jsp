<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<c:set var="ctx" value="${pageContext.request.contextPath}"/>
<!DOCTYPE html>
<html lang="zh-CN">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <meta name="color-scheme" content="dark light">
    <script>
        /* 首屏前先落主题属性，避免刷新时先闪一下暗色（偏好 > 系统 > 暗色） */
        (function () {
            try {
                var saved = localStorage.getItem('monahub:theme');
                if (saved !== 'light' && saved !== 'dark') {
                    saved = window.matchMedia('(prefers-color-scheme: light)').matches ? 'light' : 'dark';
                }
                if (saved === 'light') {
                    document.documentElement.setAttribute('data-theme', 'light');
                }
            } catch (ignore) {
            }
        })();
    </script>
    <title>工作台 · MonaHub 资料提交台</title>
    <link rel="stylesheet" href="${ctx}/static/css/app.css">
</head>
<body class="page-hub" data-ctx="${ctx}">

<header class="topbar">
    <a class="brand-mini" href="${ctx}/files">
        <span class="mark">M</span>
        <span>MonaHub</span>
        <span class="brand-suffix">资料提交台</span>
    </a>

    <div class="topbar-right">
        <button type="button" class="btn btn-ghost btn-sm" data-theme-toggle aria-label="切换深浅主题" title="切换深浅主题">🌙</button>
        <div class="userchip">
            <span class="avatar"><c:out value="${fn:substring(user.displayName, 0, 1)}"/></span>
            <span class="userchip-name"><c:out value="${user.displayName}"/></span>
            <span class="userchip-id">@<c:out value="${user.username}"/></span>
        </div>
        <form method="post" action="${ctx}/logout" class="inline-form">
            <button type="submit" class="btn btn-ghost btn-sm">退出</button>
        </form>
    </div>
</header>

<main class="hub">

    <!-- ==================== 统计条 ==================== -->
    <section class="stats" aria-label="统计">
        <article class="stat card">
            <span class="stat-label">文件总数</span>
            <strong class="stat-value" data-count="${stats.total}">${stats.total}</strong>
            <span class="stat-foot">全部提交记录</span>
        </article>
        <article class="stat card">
            <span class="stat-label">本月新增</span>
            <strong class="stat-value" data-count="${stats.monthNew}">${stats.monthNew}</strong>
            <span class="stat-foot">当月上传的文件</span>
        </article>
        <article class="stat card">
            <span class="stat-label">累计下载</span>
            <strong class="stat-value" data-count="${stats.downloads}">${stats.downloads}</strong>
            <span class="stat-foot">本人文件的下载次数</span>
        </article>
    </section>

    <!-- ==================== 上传区（选择即上传） ==================== -->
    <section class="upload card" id="dropzone" aria-label="上传文件">
        <form id="uploadForm" method="post" action="${ctx}/files/upload" enctype="multipart/form-data">

            <input type="file" id="fileInput" name="files" multiple hidden
                   accept=".pdf,.jpg,.jpeg,.png,.doc,.docx,.zip">

            <div class="dz-empty" id="dzEmpty">
                <span class="dz-icon" aria-hidden="true">↑</span>
                <p class="dz-title">
                    拖入文件，或
                    <label class="link" for="fileInput">点击选择</label>
                </p>
                <p class="dz-hint">PDF / Word / 图片 / ZIP · 单个不超过 10MB · 选择后立即上传</p>
            </div>

            <ul class="file-chips" id="fileChips"></ul>

            <div class="dz-actions" id="dzActions" hidden>
                <span class="dz-meta" id="dzMeta"></span>
                <div class="dz-btns">
                    <button type="button" class="btn btn-ghost btn-sm" id="reloadBtn" hidden>刷新列表</button>
                    <button type="button" class="btn btn-ghost btn-sm" id="clearBtn">清空列表</button>
                </div>
            </div>

            <noscript>
                <div class="dz-actions">
                    <span class="dz-meta">当前浏览器禁用了脚本：选好文件后点右侧按钮上传</span>
                    <button type="submit" class="btn btn-primary btn-sm">上传</button>
                </div>
            </noscript>
        </form>
    </section>

    <!-- ==================== flash：逐文件上传结果 ==================== -->
    <c:if test="${not empty results}">
        <section class="report card" aria-live="polite">
            <h2 class="report-title">本次上传结果</h2>
            <ul class="report-list">
                <c:forEach var="r" items="${results}">
                    <li class="${r.duplicate ? 'dup' : (r.ok ? 'ok' : 'fail')}">
                        <span class="dot"></span>
                        <strong><c:out value="${r.name}"/></strong>
                        <span class="report-msg"><c:out value="${r.message}"/></span>
                    </li>
                </c:forEach>
            </ul>
        </section>
    </c:if>

    <c:if test="${not empty message}">
        <div class="toast" data-toast role="status"><c:out value="${message}"/></div>
    </c:if>

    <!-- ==================== 工具栏：分类 + 搜索 ==================== -->
    <c:url var="allUrl" value="/files">
        <c:param name="category" value="all"/>
        <c:param name="q" value="${keyword}"/>
    </c:url>
    <c:url var="docUrl" value="/files">
        <c:param name="category" value="doc"/>
        <c:param name="q" value="${keyword}"/>
    </c:url>
    <c:url var="imageUrl" value="/files">
        <c:param name="category" value="image"/>
        <c:param name="q" value="${keyword}"/>
    </c:url>
    <c:url var="archiveUrl" value="/files">
        <c:param name="category" value="archive"/>
        <c:param name="q" value="${keyword}"/>
    </c:url>

    <section class="toolbar">
        <nav class="pills" aria-label="按文件类型筛选">
            <a class="pill ${category == 'all' ? 'active' : ''}" href="${allUrl}">全部 <span>${counts.all}</span></a>
            <a class="pill ${category == 'doc' ? 'active' : ''}" href="${docUrl}">文档 <span>${counts.doc}</span></a>
            <a class="pill ${category == 'image' ? 'active' : ''}" href="${imageUrl}">图片 <span>${counts.image}</span></a>
            <a class="pill ${category == 'archive' ? 'active' : ''}" href="${archiveUrl}">压缩包 <span>${counts.archive}</span></a>
        </nav>

        <form class="search" method="get" action="${ctx}/files" role="search">
            <input type="hidden" name="category" value="${category}">
            <input type="hidden" name="size" value="${pageSize}">
            <input type="search" name="q" value="${fn:escapeXml(keyword)}"
                   placeholder="搜索文件名…" aria-label="搜索文件名">
            <button type="submit" class="btn btn-ghost btn-sm">搜索</button>
        </form>

        <c:if test="${not empty files}">
            <button type="button" class="btn btn-ghost btn-sm" id="multiBtn" aria-pressed="false">多选</button>
        </c:if>
    </section>

    <!-- ==================== 多选批量操作 ==================== -->
    <section class="batch-bar card" id="batchBar" hidden aria-label="批量操作">
        <span class="batch-count" id="batchCount">已选 0 项</span>
        <div class="batch-actions">
            <form id="batchForm" method="post" action="${ctx}/files/batch-delete"
                  class="inline-form js-action-form" data-kind="batch-delete" data-batch-count="0">
                <input type="hidden" name="category" value="${category}">
                <input type="hidden" name="q" value="${fn:escapeXml(keyword)}">
                <input type="hidden" name="page" value="${page.current}">
                <input type="hidden" name="size" value="${pageSize}">
                <span id="batchIds"></span>
                <button type="submit" class="btn btn-danger btn-sm" id="batchDeleteBtn" disabled>删除选中</button>
            </form>
            <button type="button" class="btn btn-ghost btn-sm" id="batchCancelBtn">取消</button>
        </div>
    </section>

    <!-- ==================== 文件列表 ==================== -->
    <section class="list card" id="fileList" aria-label="我的文件">
        <c:choose>
            <c:when test="${empty files}">
                <div class="empty">
                    <div class="empty-art" aria-hidden="true">
                        <span></span><span></span><span></span>
                    </div>
                    <h3>这里还没有资料</h3>
                    <p>
                        <c:choose>
                            <c:when test="${category != 'all' || not empty keyword}">
                                当前筛选条件下没有匹配的文件，换个类型或关键字试试。
                            </c:when>
                            <c:otherwise>
                                把第一个文件拖到上面的上传区，选择后会自动上传并出现在这里。
                            </c:otherwise>
                        </c:choose>
                    </p>
                </div>
            </c:when>
            <c:otherwise>
                <div class="list-head" aria-hidden="true">
                    <span class="row-check"></span>
                    <span>文件</span>
                    <span>大小</span>
                    <span>上传时间</span>
                    <span>状态</span>
                    <span>下载</span>
                    <span class="head-actions">操作</span>
                </div>

                <c:forEach var="f" items="${files}">
                    <article class="row" data-cat="${f.categoryKey}"
                             data-open-url="${ctx}/files/${f.id}/open"
                             data-frozen="${f.frozen}">
                        <label class="row-check" title="选择该文件">
                            <input type="checkbox" class="js-row-check" value="${f.id}"
                                   aria-label="选择 ${fn:escapeXml(f.originName)}">
                        </label>
                        <div class="cell file-cell">
                            <span class="type-ico" data-cat="${f.categoryKey}">
                                <c:out value="${fn:toUpperCase(f.ext)}"/>
                            </span>
                            <span class="file-text">
                                <span class="file-name" title="${fn:escapeXml(f.originName)}">
                                    <c:out value="${f.originName}"/>
                                </span>
                                <span class="file-meta">${f.categoryLabel} · .<c:out value="${f.ext}"/></span>
                            </span>
                        </div>

                        <span class="cell cell-muted" data-label="大小">${f.sizeText}</span>
                        <span class="cell cell-muted" data-label="上传时间">${f.createTimeText}</span>
                        <span class="cell">
                            <span class="badge ${f.frozen ? 'badge-frozen' : 'badge-normal'}">
                                ${f.frozen ? '已冻结' : '正常'}
                            </span>
                        </span>
                        <span class="cell cell-muted" data-label="下载次数">${f.downloadCount}</span>

                        <div class="cell row-actions">
                            <c:choose>
                                <c:when test="${f.frozen}">
                                    <span class="btn btn-ghost btn-sm is-disabled" aria-disabled="true">不可下载</span>
                                </c:when>
                                <c:otherwise>
                                    <a class="btn btn-primary btn-sm" href="${ctx}/files/${f.id}/download">下载</a>
                                </c:otherwise>
                            </c:choose>

                            <form method="post" action="${ctx}/files/${f.id}/freeze"
                                  class="inline-form js-action-form"
                                  data-kind="freeze"
                                  data-action="${f.frozen ? '解冻' : '冻结'}"
                                  data-file="${fn:escapeXml(f.originName)}">
                                <input type="hidden" name="frozen" value="${f.frozen ? 0 : 1}">
                                <input type="hidden" name="category" value="${category}">
                                <input type="hidden" name="q" value="${fn:escapeXml(keyword)}">
                                <input type="hidden" name="page" value="${page.current}">
                                <input type="hidden" name="size" value="${pageSize}">
                                <button type="submit" class="btn btn-ghost btn-sm">
                                    ${f.frozen ? '解冻' : '冻结'}
                                </button>
                            </form>

                            <form method="post" action="${ctx}/files/${f.id}/rename"
                                  class="inline-form js-rename-form"
                                  data-file="${fn:escapeXml(f.originName)}"
                                  data-ext="${fn:escapeXml(f.ext)}">
                                <input type="hidden" name="newName" value="">
                                <input type="hidden" name="category" value="${category}">
                                <input type="hidden" name="q" value="${fn:escapeXml(keyword)}">
                                <input type="hidden" name="page" value="${page.current}">
                                <input type="hidden" name="size" value="${pageSize}">
                                <button type="submit" class="btn btn-ghost btn-sm">重命名</button>
                            </form>

                            <form method="post" action="${ctx}/files/${f.id}/delete"
                                  class="inline-form js-action-form"
                                  data-kind="delete"
                                  data-action="删除"
                                  data-file="${fn:escapeXml(f.originName)}">
                                <input type="hidden" name="category" value="${category}">
                                <input type="hidden" name="q" value="${fn:escapeXml(keyword)}">
                                <input type="hidden" name="page" value="${page.current}">
                                <input type="hidden" name="size" value="${pageSize}">
                                <button type="submit" class="btn btn-ghost btn-sm btn-quiet-danger">删除</button>
                            </form>
                        </div>
                    </article>
                </c:forEach>

                <c:if test="${page.pages > 1}">
                    <c:url var="prevUrl" value="/files">
                        <c:param name="category" value="${category}"/>
                        <c:param name="q" value="${keyword}"/>
                        <c:param name="size" value="${pageSize}"/>
                        <c:param name="page" value="${page.current - 1}"/>
                    </c:url>
                    <c:url var="nextUrl" value="/files">
                        <c:param name="category" value="${category}"/>
                        <c:param name="q" value="${keyword}"/>
                        <c:param name="size" value="${pageSize}"/>
                        <c:param name="page" value="${page.current + 1}"/>
                    </c:url>

                    <nav class="pager" aria-label="分页">
                        <c:choose>
                            <c:when test="${page.current > 1}">
                                <a class="btn btn-ghost btn-sm" href="${prevUrl}">上一页</a>
                            </c:when>
                            <c:otherwise>
                                <span class="btn btn-ghost btn-sm is-disabled" aria-disabled="true">上一页</span>
                            </c:otherwise>
                        </c:choose>

                        <span class="pager-info">
                            第 ${page.current} / ${page.pages} 页 · 共 ${page.total} 个文件
                        </span>

                        <c:choose>
                            <c:when test="${page.current < page.pages}">
                                <a class="btn btn-ghost btn-sm" href="${nextUrl}">下一页</a>
                            </c:when>
                            <c:otherwise>
                                <span class="btn btn-ghost btn-sm is-disabled" aria-disabled="true">下一页</span>
                            </c:otherwise>
                        </c:choose>
                    </nav>
                </c:if>
            </c:otherwise>
        </c:choose>
    </section>

</main>

<!-- ==================== 通用操作模态（冻结/解冻/删除/重命名） ==================== -->
<div class="modal" id="actionModal" hidden>
    <div class="modal-card glass" role="dialog" aria-modal="true" aria-labelledby="modalTitle">
        <h3 id="modalTitle">确认操作</h3>
        <p id="modalText"></p>
        <p class="modal-file" id="modalFile"></p>
        <input type="text" id="modalInput" class="modal-input" maxlength="255" hidden
               aria-label="新的文件名" autocomplete="off">
        <p class="modal-hint" id="modalHint" hidden></p>
        <div class="modal-actions">
            <button type="button" class="btn btn-ghost btn-sm" id="modalCancel">取消</button>
            <button type="button" class="btn btn-danger btn-sm" id="modalOk">确认</button>
        </div>
    </div>
</div>

<script src="${ctx}/static/vendor/gsap.min.js"></script>
<script src="${ctx}/static/js/app.js"></script>
</body>
</html>
