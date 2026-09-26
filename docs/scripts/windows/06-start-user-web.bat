@echo off
chcp 65001 >nul
REM =============================================================
REM 青启e城 · 06 启动用户前端（Windows）
REM 功能：启动 user-web 开发服务器
REM =============================================================

setlocal enabledelayedexpansion
cd /d "%~dp0"
call config.bat

echo =============================================================
echo    青启e城 · 启动用户前端（user-web）
echo    端口：%USER_WEB_PORT%
echo =============================================================
echo.

REM 定位项目根目录
cd ..\..
set PROJECT_ROOT=%cd%
cd /d "%~dp0"

set WEB_DIR=%PROJECT_ROOT%\user-web

REM 检查 node_modules
if not exist "%WEB_DIR%\node_modules" (
    echo ⚠️  未检测到 node_modules，正在安装依赖...
    cd "%WEB_DIR%"
    call npm install
    if %errorlevel% neq 0 (
        echo ❌ 依赖安装失败
        pause
        exit /b 1
    )
    echo ✅ 依赖安装完成
    echo.
)

REM 启动
echo 🚀 正在启动用户前端...
echo    访问地址：http://localhost:%USER_WEB_PORT%
echo.
echo    按 Ctrl+C 停止服务
echo.

cd "%WEB_DIR%"
npm run dev

echo.
echo 用户前端已停止。
pause
