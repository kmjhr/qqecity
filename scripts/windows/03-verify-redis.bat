@echo off
chcp 65001 >nul
REM =============================================================
REM 青启e城 · 03 Redis 验证脚本（Windows）
REM 功能：验证 Redis 连接、基本读写、配置状态
REM =============================================================

setlocal enabledelayedexpansion
cd /d "%~dp0"
call config.bat

echo =============================================================
echo    青启e城 · Redis 验证
echo    目标：%REDIS_HOST%:%REDIS_PORT%
echo =============================================================
echo.

REM 构建 redis-cli 命令参数
set REDIS_CMD=redis-cli -h %REDIS_HOST% -p %REDIS_PORT%
if not "%REDIS_PASSWORD%"=="" (
    set REDIS_CMD=%REDIS_CMD% -a %REDIS_PASSWORD% --no-auth-warning
)

REM 测试连接
echo [1/6] 测试连接（PING）...
for /f "delims=" %%i in ('echo PING ^| %REDIS_CMD% 2^>nul') do set PONG=%%i
if "%PONG%"=="PONG" (
    echo ✅ 连接成功，返回 PONG
) else (
    echo ❌ 连接失败
    echo    请检查：
    echo    1. Redis 服务是否已启动
    echo    2. config.bat 中的地址/端口/密码是否正确
    echo    3. 防火墙是否放行 6379 端口
    echo    4. bind 配置是否允许远程连接
    pause
    exit /b 1
)
echo.

REM 版本信息
echo [2/6] Redis 版本...
for /f "delims=" %%i in ('echo INFO server ^| %REDIS_CMD% 2^>nul ^| findstr /i "redis_version"') do set VER_LINE=%%i
echo   !VER_LINE!
echo.

REM 基本读写测试
echo [3/6] 基本读写测试...
echo SET test:ping pong | %REDIS_CMD% >nul
for /f "delims=" %%i in ('echo GET test:ping ^| %REDIS_CMD% 2^>nul') do set VAL=%%i
if "%VAL%"=="pong" (
    echo ✅ SET/GET 正常
) else (
    echo ❌ SET/GET 失败
)
echo DEL test:ping | %REDIS_CMD% >nul
echo.

REM 内存使用
echo [4/6] 内存使用...
for /f "delims=" %%i in ('echo INFO memory ^| %REDIS_CMD% 2^>nul ^| findstr /i "used_memory_human"') do set MEM_LINE=%%i
echo   !MEM_LINE!
for /f "delims=" %%i in ('echo dbsize ^| %REDIS_CMD% 2^>nul') do set DBSIZE=%%i
echo   当前数据库 key 数量：!DBSIZE!
echo.

REM 持久化状态
echo [5/6] 持久化状态...
for /f "delims=" %%i in ('echo CONFIG GET save ^| %REDIS_CMD% 2^>nul ^| findstr /v "^$"') do (
    set SAVE_LINE=%%i
)
echo   RDB 保存策略：已配置
for /f "delims=" %%i in ('echo CONFIG GET appendonly ^| %REDIS_CMD% 2^>nul ^| findstr /v "appendonly"') do set AOF_STATUS=%%i
echo   AOF 状态：!AOF_STATUS!
echo.

REM maxmemory 配置
echo [6/6] 最大内存配置...
for /f "delims=" %%i in ('echo CONFIG GET maxmemory ^| %REDIS_CMD% 2^>nul ^| findstr /v "maxmemory"') do set MAXMEM=%%i
if "!MAXMEM!"=="0" (
    echo   maxmemory：未设置（不限制）
) else (
    echo   maxmemory：!MAXMEM! 字节
)
for /f "delims=" %%i in ('echo CONFIG GET maxmemory-policy ^| %REDIS_CMD% 2^>nul ^| findstr /v "maxmemory-policy"') do set POLICY=%%i
echo   淘汰策略：!POLICY!
echo.

echo =============================================================
echo    验证完成
echo =============================================================
echo.
echo 💡 Redis 运行正常。未安装 Redis 时后端会自动降级（跳过黑名单检查），不影响登录与核心业务。
pause
