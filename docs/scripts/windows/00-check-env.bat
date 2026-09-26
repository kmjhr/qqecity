@echo off
chcp 65001 >nul
REM =============================================================
REM 青启e城 · 00 环境检查脚本（Windows）
REM 功能：检查开发所需的全部依赖是否已安装
REM 检查项：Java / Maven / Node.js / MySQL / Redis / Git
REM =============================================================

setlocal enabledelayedexpansion
cd /d "%~dp0"
call config.bat

echo =============================================================
echo    青启e城 · 开发环境检查
echo =============================================================
echo.

set PASS=0
set FAIL=0

REM ---------- 1. Java ----------
echo [1/6] 检查 Java ...
where java >nul 2>nul
if %errorlevel%==0 (
    for /f "tokens=3" %%i in ('java -version 2^>^&1 ^| findstr /i "version"') do (
        set JAVA_VER=%%~i
    )
    echo     ✅ Java 已安装：!JAVA_VER!
    set /a PASS+=1
) else (
    echo     ❌ 未检测到 Java，请安装 JDK 17+
    echo        下载地址：https://adoptium.net/
    set /a FAIL+=1
)
echo.

REM ---------- 2. Maven ----------
echo [2/6] 检查 Maven ...
where mvn >nul 2>nul
if %errorlevel%==0 (
    for /f "tokens=3" %%i in ('mvn -version 2^>^&1 ^| findstr /i "Apache Maven"') do (
        set MAVEN_VER=%%i
    )
    echo     ✅ Maven 已安装：!MAVEN_VER!
    set /a PASS+=1
) else (
    echo     ⚠️  未检测到 Maven（使用 IDE 内置 Maven 可忽略）
    echo        下载地址：https://maven.apache.org/download.cgi
    set /a FAIL+=1
)
echo.

REM ---------- 3. Node.js ----------
echo [3/6] 检查 Node.js ...
where node >nul 2>nul
if %errorlevel%==0 (
    for /f "delims=" %%i in ('node -v') do set NODE_VER=%%i
    echo     ✅ Node.js 已安装：!NODE_VER!
    where npm >nul 2>nul
    if !errorlevel!==0 (
        for /f "delims=" %%i in ('npm -v') do set NPM_VER=%%i
        echo     ✅ npm 已安装：!NPM_VER!
    )
    set /a PASS+=1
) else (
    echo     ❌ 未检测到 Node.js，请安装 Node.js 18+
    echo        下载地址：https://nodejs.org/
    set /a FAIL+=1
)
echo.

REM ---------- 4. MySQL ----------
echo [4/6] 检查 MySQL ...
where mysql >nul 2>nul
if %errorlevel%==0 (
    echo     ✅ MySQL 命令行工具已找到
    REM 尝试连接
    mysql -h%MYSQL_HOST% -P%MYSQL_PORT% -u%MYSQL_USER% -p%MYSQL_PASSWORD% -e "SELECT 1;" >nul 2>nul
    if !errorlevel!==0 (
        echo     ✅ MySQL 连接成功（%MYSQL_HOST%:%MYSQL_PORT%）
        REM 检查数据库是否存在
        mysql -h%MYSQL_HOST% -P%MYSQL_PORT% -u%MYSQL_USER% -p%MYSQL_PASSWORD% -e "USE %DB_NAME%;" >nul 2>nul
        if !errorlevel!==0 (
            echo     ✅ 数据库 %DB_NAME% 已存在
        ) else (
            echo     ⚠️  数据库 %DB_NAME% 不存在，请运行 01-init-db.bat 初始化
        )
    ) else (
        echo     ❌ MySQL 连接失败，请检查服务是否启动及配置是否正确
    )
    set /a PASS+=1
) else (
    echo     ⚠️  未找到 mysql 命令（如未加入 PATH 可忽略）
    echo        请确保 MySQL 服务已启动
    set /a FAIL+=1
)
echo.

REM ---------- 5. Redis ----------
echo [5/6] 检查 Redis ...
where redis-cli >nul 2>nul
if %errorlevel%==0 (
    echo     ✅ redis-cli 已找到
) else (
    echo     ℹ️  未找到 redis-cli（Windows 可使用 Redis 安装目录）
)

REM 尝试连接 Redis
set REDIS_OK=0
if "%REDIS_PASSWORD%"=="" (
    echo PING | redis-cli -h %REDIS_HOST% -p %REDIS_PORT% >nul 2>nul
    if !errorlevel!==0 set REDIS_OK=1
) else (
    echo PING | redis-cli -h %REDIS_HOST% -p %REDIS_PORT% -a %REDIS_PASSWORD% --no-auth-warning >nul 2>nul
    if !errorlevel!==0 set REDIS_OK=1
)

if %REDIS_OK%==1 (
    echo     ✅ Redis 连接成功（%REDIS_HOST%:%REDIS_PORT%）
) else (
    echo     ⚠️  Redis 连接失败或未安装（MVP 阶段为可选依赖）
    echo        参考文档：docs/deployment/redis-deployment.md
)
set /a PASS+=1
echo.

REM ---------- 6. Git ----------
echo [6/6] 检查 Git ...
where git >nul 2>nul
if %errorlevel%==0 (
    for /f "delims=" %%i in ('git --version') do set GIT_VER=%%i
    echo     ✅ !GIT_VER!
    set /a PASS+=1
) else (
    echo     ❌ 未检测到 Git
    echo        下载地址：https://git-scm.com/download/win
    set /a FAIL+=1
)
echo.

REM ---------- 汇总 ----------
echo =============================================================
echo    检查完成：通过 !PASS! / 6 项，失败/警告 !FAIL! / 6 项
echo =============================================================
echo.

if %FAIL%==0 (
    echo 🎉 环境全部就绪，可以开始运行项目了！
    echo    快速开始：运行 99-start-all.bat 一键启动全部服务
) else (
    echo ⚠️  有 %FAIL% 项未通过，请先安装缺失的依赖。
    echo    依赖安装完成后，可再次运行本脚本确认。
)
echo.
pause
