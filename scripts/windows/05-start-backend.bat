@echo off
chcp 65001 >nul
REM =============================================================
REM 青启e城 · 05 启动后端（Windows）
REM 功能：启动 Spring Boot 后端服务
REM =============================================================

setlocal enabledelayedexpansion
cd /d "%~dp0"
call config.bat

echo =============================================================
echo    青启e城 · 启动后端服务
echo    端口：%BACKEND_PORT%
echo =============================================================
echo.

REM 定位项目根目录
cd ..\..
set PROJECT_ROOT=%cd%
cd /d "%~dp0"

set JAR_FILE=%PROJECT_ROOT%\backend\target\qingqi-backend.jar

REM 检查 jar 包是否存在
if exist "%JAR_FILE%" (
    echo ✅ 找到已编译的 jar 包
) else (
    echo ⚠️  未找到 jar 包，尝试编译...
    cd "%PROJECT_ROOT%\backend"
    
    where mvn >nul 2>nul
    if %errorlevel%==0 (
        call mvn clean package -DskipTests -q
    ) else if exist mvnw.cmd (
        call mvnw.cmd clean package -DskipTests -q
    ) else (
        echo ❌ 未找到 Maven，请先手动编译后端
        pause
        exit /b 1
    )
    
    if exist "%JAR_FILE%" (
        echo ✅ 编译成功
    ) else (
        echo ❌ 编译失败，请检查错误信息
        pause
        exit /b 1
    )
)
echo.

REM 启动
echo 🚀 正在启动后端服务...
echo    访问地址：http://localhost:%BACKEND_PORT%
echo    接口文档：http://localhost:%BACKEND_PORT%/api/doc.html
echo.
echo    按 Ctrl+C 停止服务
echo.

cd "%PROJECT_ROOT%\backend"
java -jar "%JAR_FILE%" --server.port=%BACKEND_PORT%

echo.
echo 后端服务已停止。
pause
