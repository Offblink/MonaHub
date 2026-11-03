<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
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
    <title>${code} · MonaHub</title>
    <link rel="stylesheet" href="${ctx}/static/css/app.css">
</head>
<body class="page-error" data-ctx="${ctx}">

<button type="button" class="theme-float" data-theme-toggle aria-label="切换深浅主题" title="切换深浅主题">🌙</button>

<main class="error-shell">
    <div class="error-card glass">
        <p class="error-code"><c:out value="${code}"/></p>
        <h1><c:out value="${message}"/></h1>
        <p class="error-hint">
            如果是从下载链接进来的，可能是文件已被冻结、已丢失，或不属于当前账号。
        </p>
        <div class="error-actions">
            <a class="btn btn-primary" href="${ctx}/files">返回工作台</a>
            <a class="btn btn-ghost" href="${ctx}/login">重新登录</a>
        </div>
    </div>
</main>

</body>
</html>
