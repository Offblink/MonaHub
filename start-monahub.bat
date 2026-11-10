@echo off
chcp 65001 >nul
cd /d "%~dp0"

rem 已有实例在监听 18080 时，clean 会被其日志句柄挡住（Failed to delete monahub.log）——先检测再动手
netstat -ano | findstr ":18080" | findstr "LISTENING" >nul
if not errorlevel 1 (
    echo 检测到 18080 已被占用：已有 MonaHub 实例在运行（停止 = 关闭 MonaHub-Tomcat 窗口）。
    echo 如需重启：先关闭旧窗口，再运行本脚本。
    start "" http://127.0.0.1:18080/monahub/
    pause
    exit /b 0
)

echo [1/3] 打包（跳过测试）...
call mvn -B -DskipTests clean package
if errorlevel 1 (
    echo 打包失败，看上面的报错。
    pause
    exit /b 1
)

echo [2/3] 新窗口启动 Tomcat（停止 = 关闭该窗口或 Ctrl+C）...
start "MonaHub-Tomcat" cmd /k mvn -B -Prun cargo:run

echo [3/3] 等待 http://127.0.0.1:18080/monahub/ 就绪...
set /a n=0
:wait
ping -n 3 127.0.0.1 >nul
curl -sf -o NUL --max-time 3 http://127.0.0.1:18080/monahub/login && goto ready
set /a n+=1
if %n% lss 45 goto wait
echo 两分钟还没起来，去 MonaHub-Tomcat 窗口看报错（常见：MySQL 没启动）。
pause
exit /b 1

:ready
start "" http://127.0.0.1:18080/monahub/
echo.
echo 已打开 http://127.0.0.1:18080/monahub/
echo 账号 zhangsan/123456 或 lisi/123456；停止服务 = 关闭 MonaHub-Tomcat 窗口。
pause
