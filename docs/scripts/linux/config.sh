#!/bin/bash
# =============================================================
# 青启e城 · 统一配置文件（Linux 版）
# 所有脚本共用此配置，修改这里即可，不用改每个脚本
# =============================================================

# ====== 数据库配置 ======
export MYSQL_HOST=127.0.0.1
export MYSQL_PORT=3306
export MYSQL_USER=root
export MYSQL_PASSWORD=123456
export DB_NAME=qingqi

# ====== Redis 配置 ======
export REDIS_HOST=127.0.0.1
export REDIS_PORT=6379
export REDIS_PASSWORD=

# ====== 后端配置 ======
export BACKEND_PORT=8080
export JAVA_HOME=
export MAVEN_HOME=

# ====== 前端配置 ======
export USER_WEB_PORT=5173
export ADMIN_WEB_PORT=5174
export NODE_HOME=

# ====== SQL 文件路径（相对脚本目录） ======
export SCHEMA_SQL="../../backend/sql/schema.sql"
export DATA_SQL="../../backend/sql/data.sql"
