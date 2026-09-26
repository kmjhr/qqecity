@echo off
chcp 65001 >nul
REM =============================================================
REM 青启e城 · 01 数据库一键初始化（Windows）
REM 功能：创建数据库 + 导入表结构 + 导入演示数据
REM 幂等设计：schema.sql 使用 CREATE TABLE IF NOT EXISTS
REM         data.sql 使用 INSERT IGNORE
REM =============================================================

setlocal enabledelayedexpansion
cd /d "%~dp0"
call config.bat

echo =============================================================
echo    青启e城 · 数据库初始化
echo    目标：%MYSQL_HOST%:%MYSQL_PORT% / %DB_NAME%
echo =============================================================
echo.

REM 检查 SQL 文件是否存在
if not exist "%SCHEMA_SQL%" (
    echo ❌ 找不到 schema.sql：%SCHEMA_SQL%
    echo    请确认脚本在 scripts\windows\ 目录下运行
    pause
    exit /b 1
)

if not exist "%DATA_SQL%" (
    echo ❌ 找不到 data.sql：%DATA_SQL%
    pause
    exit /b 1
)

REM 测试连接
echo [0/3] 测试 MySQL 连接...
mysql -h%MYSQL_HOST% -P%MYSQL_PORT% -u%MYSQL_USER% -p%MYSQL_PASSWORD% -e "SELECT 1;" >nul 2>nul
if %errorlevel% neq 0 (
    echo ❌ MySQL 连接失败！
    echo    请检查：
    echo    1. MySQL 服务是否已启动
    echo    2. config.bat 中的地址/端口/账号密码是否正确
    echo    3. 防火墙是否放行 3306 端口
    pause
    exit /b 1
)
echo ✅ 连接成功
echo.

REM 第1步：创建数据库
echo [1/3] 创建数据库 %DB_NAME% ...
mysql -h%MYSQL_HOST% -P%MYSQL_PORT% -u%MYSQL_USER% -p%MYSQL_PASSWORD% -e "CREATE DATABASE IF NOT EXISTS %DB_NAME% DEFAULT CHARACTER SET utf8mb4 DEFAULT COLLATE utf8mb4_general_ci;"
if %errorlevel% equ 0 (
    echo ✅ 数据库已创建（或已存在）
) else (
    echo ❌ 创建数据库失败
    pause
    exit /b 1
)
echo.

REM 第2步：导入表结构
echo [2/3] 导入表结构 schema.sql ...
mysql -h%MYSQL_HOST% -P%MYSQL_PORT% -u%MYSQL_USER% -p%MYSQL_PASSWORD% %DB_NAME% < "%SCHEMA_SQL%"
if %errorlevel% equ 0 (
    echo ✅ 表结构导入完成
) else (
    echo ❌ 表结构导入失败
    pause
    exit /b 1
)
echo.

REM 第3步：导入演示数据
echo [3/3] 导入演示数据 data.sql ...
mysql -h%MYSQL_HOST% -P%MYSQL_PORT% -u%MYSQL_USER% -p%MYSQL_PASSWORD% %DB_NAME% < "%DATA_SQL%"
if %errorlevel% equ 0 (
    echo ✅ 演示数据导入完成
) else (
    echo ❌ 演示数据导入失败
    pause
    exit /b 1
)
echo.

REM 验证
echo =============================================================
echo    初始化完成！正在验证...
echo =============================================================

for /f "usebackq tokens=1" %%a in (`mysql -h%MYSQL_HOST% -P%MYSQL_PORT% -u%MYSQL_USER% -p%MYSQL_PASSWORD% -N -e "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema='%DB_NAME%';"`) do set TABLE_COUNT=%%a

echo 表数量：%TABLE_COUNT% 张（应为 25 张）

for /f "usebackq tokens=1" %%a in (`mysql -h%MYSQL_HOST% -P%MYSQL_PORT% -u%MYSQL_USER% -p%MYSQL_PASSWORD% -N -e "SELECT COUNT(*) FROM %DB_NAME%.sys_user;"`) do set USER_COUNT=%%a

echo 用户数：%USER_COUNT% 个（演示账号 5 个）
echo.

echo 演示账号（统一密码：123456）：
echo   admin         系统管理员
echo   testuser      青年用户（在校生）
echo   entrepreneur  青年创业者
echo   landlord01    房东
echo   banker01      银行运营岗
echo.

if "%TABLE_COUNT%"=="25" (
    echo ✅ 验证通过！数据库初始化成功。
) else (
    echo ⚠️  表数量不是 25，可能导入不完整，请检查错误信息。
)
echo.
pause
