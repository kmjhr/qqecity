@echo off
REM =============================================================
REM 青启e城 · 统一配置文件（Windows 版）
REM 所有脚本共用此配置，修改这里即可，不用改每个脚本
REM =============================================================

REM ====== 数据库配置 ======
set MYSQL_HOST=127.0.0.1
set MYSQL_PORT=3306
set MYSQL_USER=root
set MYSQL_PASSWORD=123456
set DB_NAME=qingqi

REM ====== Redis 配置 ======
set REDIS_HOST=127.0.0.1
set REDIS_PORT=6379
set REDIS_PASSWORD=

REM ====== 后端配置 ======
set BACKEND_PORT=8080
set JAVA_HOME=
set MAVEN_HOME=

REM ====== 前端配置 ======
set USER_WEB_PORT=5173
set ADMIN_WEB_PORT=5174
set NODE_HOME=

REM ====== SQL 文件路径（相对脚本目录） ======
set SCHEMA_SQL=..\..\backend\sql\schema.sql
set DATA_SQL=..\..\backend\sql\data.sql
