#!/bin/bash
# =============================================================
# 青启e城 · 00 环境检查脚本（Linux）
# 功能：检查开发所需的全部依赖是否已安装
# 检查项：Java / Maven / Node.js / MySQL / Redis / Git
# =============================================================

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
source "$SCRIPT_DIR/config.sh"

echo "============================================================"
echo "   青启e城 · 开发环境检查"
echo "============================================================"
echo ""

PASS=0
FAIL=0

# ---------- 1. Java ----------
echo "[1/6] 检查 Java ..."
if command -v java &> /dev/null; then
    JAVA_VER=$(java -version 2>&1 | head -1 | awk -F'"' '{print $2}')
    echo "    ✅ Java 已安装：$JAVA_VER"
    PASS=$((PASS + 1))
else
    echo "    ❌ 未检测到 Java，请安装 JDK 17+"
    echo "       Ubuntu: sudo apt install openjdk-17-jdk"
    echo "       CentOS: sudo yum install java-17-openjdk-devel"
    FAIL=$((FAIL + 1))
fi
echo ""

# ---------- 2. Maven ----------
echo "[2/6] 检查 Maven ..."
if command -v mvn &> /dev/null; then
    MAVEN_VER=$(mvn -version 2>&1 | head -1 | awk '{print $3}')
    echo "    ✅ Maven 已安装：$MAVEN_VER"
    PASS=$((PASS + 1))
else
    echo "    ⚠️  未检测到 Maven（IDE 内置或 Maven Wrapper 可忽略）"
    echo "       Ubuntu: sudo apt install maven"
    echo "       CentOS: sudo yum install maven"
    FAIL=$((FAIL + 1))
fi
echo ""

# ---------- 3. Node.js ----------
echo "[3/6] 检查 Node.js ..."
if command -v node &> /dev/null; then
    NODE_VER=$(node -v)
    echo "    ✅ Node.js 已安装：$NODE_VER"
    if command -v npm &> /dev/null; then
        NPM_VER=$(npm -v)
        echo "    ✅ npm 已安装：$NPM_VER"
    fi
    PASS=$((PASS + 1))
else
    echo "    ❌ 未检测到 Node.js，请安装 Node.js 18+"
    echo "       Ubuntu: sudo apt install nodejs npm"
    echo "       推荐使用 nvm: curl -o- https://raw.githubusercontent.com/nvm-sh/nvm/v0.39.0/install.sh | bash"
    FAIL=$((FAIL + 1))
fi
echo ""

# ---------- 4. MySQL ----------
echo "[4/6] 检查 MySQL ..."
if command -v mysql &> /dev/null; then
    echo "    ✅ MySQL 命令行工具已找到"
    if mysql -h"$MYSQL_HOST" -P"$MYSQL_PORT" -u"$MYSQL_USER" -p"$MYSQL_PASSWORD" -e "SELECT 1;" &> /dev/null; then
        echo "    ✅ MySQL 连接成功（${MYSQL_HOST}:${MYSQL_PORT}）"
        if mysql -h"$MYSQL_HOST" -P"$MYSQL_PORT" -u"$MYSQL_USER" -p"$MYSQL_PASSWORD" -e "USE $DB_NAME;" &> /dev/null; then
            echo "    ✅ 数据库 $DB_NAME 已存在"
        else
            echo "    ⚠️  数据库 $DB_NAME 不存在，请运行 01-init-db.sh 初始化"
        fi
    else
        echo "    ❌ MySQL 连接失败，请检查服务是否启动及配置是否正确"
    fi
    PASS=$((PASS + 1))
else
    echo "    ⚠️  未找到 mysql 命令（如未加入 PATH 可忽略）"
    echo "       请确保 MySQL 服务已启动"
    FAIL=$((FAIL + 1))
fi
echo ""

# ---------- 5. Redis ----------
echo "[5/6] 检查 Redis ..."
if command -v redis-cli &> /dev/null; then
    echo "    ✅ redis-cli 已找到"
else
    echo "    ℹ️  未找到 redis-cli"
    echo "       Ubuntu: sudo apt install redis-tools"
    echo "       CentOS: sudo yum install redis"
fi

# 尝试连接 Redis
REDIS_OK=0
if [ -z "$REDIS_PASSWORD" ]; then
    if redis-cli -h "$REDIS_HOST" -p "$REDIS_PORT" ping &> /dev/null; then
        REDIS_OK=1
    fi
else
    if redis-cli -h "$REDIS_HOST" -p "$REDIS_PORT" -a "$REDIS_PASSWORD" --no-auth-warning ping &> /dev/null; then
        REDIS_OK=1
    fi
fi

if [ $REDIS_OK -eq 1 ]; then
    echo "    ✅ Redis 连接成功（${REDIS_HOST}:${REDIS_PORT}）"
else
    echo "    ⚠️  Redis 连接失败或未安装（MVP 阶段为可选依赖）"
    echo "       参考文档：docs/deployment/redis-deployment.md"
fi
PASS=$((PASS + 1))
echo ""

# ---------- 6. Git ----------
echo "[6/6] 检查 Git ..."
if command -v git &> /dev/null; then
    GIT_VER=$(git --version)
    echo "    ✅ $GIT_VER"
    PASS=$((PASS + 1))
else
    echo "    ❌ 未检测到 Git"
    echo "       Ubuntu: sudo apt install git"
    echo "       CentOS: sudo yum install git"
    FAIL=$((FAIL + 1))
fi
echo ""

# ---------- 汇总 ----------
echo "============================================================"
echo "   检查完成：通过 $PASS / 6 项，失败/警告 $FAIL / 6 项"
echo "============================================================"
echo ""

if [ $FAIL -eq 0 ]; then
    echo "🎉 环境全部就绪，可以开始运行项目了！"
    echo "   快速开始：运行 99-start-all.sh 一键启动全部服务"
else
    echo "⚠️  有 $FAIL 项未通过，请先安装缺失的依赖。"
    echo "   依赖安装完成后，可再次运行本脚本确认。"
fi
echo ""
