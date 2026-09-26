@echo off
chcp 65001 >nul
REM =============================================================
REM 青启e城 · 99 一键全部启动（Windows）
REM 功能：依次启动 后端 + 用户前端 + 管理前端
REM 注意：前端会在新窗口打开，后端在当前窗口运行
REM =============================================================

setlocal enabledelayedexpansion
cd /d "%~dp0"
call config.bat

echo =============================================================
echo    青启e城 · 一键启动全部服务
echo =============================================================
echo.
echo 即将启动以下服务：
echo   [1] 后端服务    http://localhost:%BACKEND_PORT%
echo   [2] 用户前端    http://localhost:%USER_WEB_PORT%
echo   [3] 管理前端    http://localhost:%ADMIN_WEB_PORT%
echo.
echo 说明：
echo   - 用户前端和管理前端会在新窗口中启动
echo   - 后端服务在当前窗口运行
echo   - 关闭当前窗口将停止后端服务
echo.
pause

echo.
echo 🚀 [1/3] 启动用户前端（新窗口）...
start "青启e城 - 用户前端" cmd /k "%~dp006-start-user-web.bat"

timeout /t 2 /nobreak >nul

echo 🚀 [2/3] 启动管理前端（新窗口）...
start "青启e城 - 管理前端" cmd /k "%~dp007-start-admin-web.bat"

timeout /t 2 /nobreak >nul

echo 🚀 [3/3] 启动后端服务（当前窗口）...
echo.
call "%~dp005-start-backend.bat"

echo.
echo 全部服务已停止。
pause
