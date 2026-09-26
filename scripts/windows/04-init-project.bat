@echo off
chcp 65001 >nul
REM =============================================================
REM 青启e城 · 04 项目初始化（Windows）
REM 功能：克隆后首次运行，安装前端依赖、后端编译
REM =============================================================

setlocal enabledelayedexpansion
cd /d "%~dp0"
call config.bat

echo =============================================================
echo    青启e城 · 项目初始化
REM 获取项目根目录
cd ..\..
set PROJECT_ROOT=%cd%
echo    项目目录：%PROJECT_ROOT%
echo =============================================================
echo.

REM 回到脚本目录
cd /d "%~dp0"

REM 第1步：确认数据库已就绪
echo [1/4] 检查数据库是否就绪...
mysql -h%MYSQL_HOST% -P%MYSQL_PORT% -u%MYSQL_USER% -p%MYSQL_PASSWORD% -e "USE %DB_NAME%;" >nul 2>nul
if %errorlevel% equ 0 (
    echo ✅ 数据库 %DB_NAME% 已就绪
) else (
    echo ❌ 数据库未就绪，请先运行 01-init-db.bat 初始化
    pause
    exit /b 1
)
echo.

REM 第2步：后端编译
echo [2/4] 编译后端（Maven）...
cd "%PROJECT_ROOT%\backend"

where mvn >nul 2>nul
if %errorlevel%==0 (
    echo   使用系统 Maven 编译...
    call mvn clean package -DskipTests -q
    if %errorlevel% equ 0 (
        echo ✅ 后端编译成功
    ) else (
        echo ⚠️  Maven 编译返回非零，可能有警告或错误
    )
) else (
    echo   未检测到系统 Maven，尝试使用 Maven Wrapper...
    if exist mvnw.cmd (
        call mvnw.cmd clean package -DskipTests -q
        if %errorlevel% equ 0 (
            echo ✅ 后端编译成功（Maven Wrapper）
        ) else (
            echo ⚠️  编译返回非零
        )
    ) else (
        echo ⚠️  未找到 Maven，请手动编译或使用 IDE 打开项目
    )
)
echo.

REM 第3步：用户前端依赖安装
echo [3/4] 安装用户前端依赖（user-web）...
cd "%PROJECT_ROOT%\user-web"

where npm >nul 2>nul
if %errorlevel%==0 (
    call npm install
    if %errorlevel% equ 0 (
        echo ✅ user-web 依赖安装成功
    ) else (
        echo ⚠️  npm install 返回非零
    )
) else (
    echo ❌ 未检测到 npm，请先安装 Node.js
)
echo.

REM 第4步：管理前端依赖安装
echo [4/4] 安装管理前端依赖（admin-web）...
cd "%PROJECT_ROOT%\admin-web"

where npm >nul 2>nul
if %errorlevel%==0 (
    call npm install
    if %errorlevel% equ 0 (
        echo ✅ admin-web 依赖安装成功
    ) else (
        echo ⚠️  npm install 返回非零
    )
) else (
    echo ❌ 未检测到 npm，请先安装 Node.js
)
echo.

echo =============================================================
echo    项目初始化完成！
echo =============================================================
echo.
echo 🚀 下一步：
echo    启动全部服务：运行 99-start-all.bat
echo    或分别启动：
echo      05-start-backend.bat     启动后端
echo      06-start-user-web.bat    启动用户前端
echo      07-start-admin-web.bat   启动管理前端
echo.
pause
