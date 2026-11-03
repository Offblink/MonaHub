<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
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
    <title>登录 · MonaHub 资料提交台</title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/static/css/app.css">
</head>
<body class="page-login" data-ctx="${pageContext.request.contextPath}">

<button type="button" class="theme-float" data-theme-toggle aria-label="切换深浅主题" title="切换深浅主题">🌙</button>

<!-- 背景光球：对应 Gasp-Design 的 floating-orbs，纯 CSS + 极轻量 GSAP 视差 -->
<div class="orb orb-a" aria-hidden="true"></div>
<div class="orb orb-b" aria-hidden="true"></div>
<div class="orb orb-c" aria-hidden="true"></div>

<main class="login-shell">

    <section class="brand-block">
        <a class="brand-mini" href="${pageContext.request.contextPath}/login">
            <span class="mark">M</span>
            <span>MonaHub</span>
        </a>

        <h1 class="hero-title" data-split>
            <span class="line">资料提交台</span>
            <span class="line grad">交得清楚，查得明白</span>
        </h1>

        <p class="hero-sub">
            面向结业作业材料收集的轻量工作台：多文件并发上传、按类型归档、
            冻结与下载留痕，一条链路跑完。
        </p>

        <ul class="capabilities">
            <li><span class="dot dot-green"></span>服务端校验 · UUID 落盘</li>
            <li><span class="dot dot-blue"></span>多文件并发 · 逐条结果</li>
            <li><span class="dot dot-violet"></span>归属校验 · 下载计数</li>
        </ul>

    </section>

    <section class="panel glass">
        <header class="panel-head">
            <h2>欢迎回来</h2>
            <p>登录后管理你的提交记录</p>
        </header>

        <c:if test="${not empty error}">
            <div class="alert alert-error" role="alert">
                <c:out value="${error}"/>
            </div>
        </c:if>

        <form method="post" action="${pageContext.request.contextPath}/login" class="login-form" autocomplete="on">
            <label class="field">
                <span class="field-label">账号</span>
                <input type="text" name="username" value="${fn:escapeXml(username)}"
                       placeholder="请输入账号" required autofocus autocomplete="username">
            </label>

            <label class="field">
                <span class="field-label">密码</span>
                <span class="field-pass">
                    <input type="password" name="password" id="passwordInput"
                           placeholder="请输入密码" required autocomplete="current-password">
                    <button type="button" class="pass-toggle" id="passToggle" aria-label="显示密码">显示</button>
                </span>
            </label>

            <button type="submit" class="btn btn-primary btn-block">进入工作台</button>
        </form>

        <div class="demo-accounts">
            <span class="demo-label">演示账号</span>
            <button type="button" class="chip" data-fill-user="zhangsan" data-fill-pass="123456">zhangsan / 123456</button>
            <button type="button" class="chip" data-fill-user="lisi" data-fill-pass="123456">lisi / 123456</button>
        </div>

    </section>

</main>

<script src="${pageContext.request.contextPath}/static/vendor/gsap.min.js"></script>
<script src="${pageContext.request.contextPath}/static/js/app.js"></script>
</body>
</html>
