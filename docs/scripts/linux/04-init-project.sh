#!/bin/bash
# =============================================================
# 青启e城 · 04 项目初始化（Linux）
# 功能：克隆后首次运行，安装前端依赖、后端编译
# =============================================================

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
source "$SCRIPT_DIR/config.sh"

# 项目根目录（往上两级）
PROJECT_ROOT="$(cd "$SCRIPT_DIR/../.." && pwd)"

echo "============================================================"
echo "   青启e城 · 项目初始化"
echo "   项目目录：$PROJECT_ROOT"
echo "============================================================"
echo ""

# 第1步：确认数据库已就绪
echo "[1/4] 检查数据库是否就绪..."
if mysql -h"$MYSQL_HOST" -P"$MYSQL_PORT" -u"$MYSQL_USER" -p"$MYSQL_PASSWORD" -e "USE $DB_NAME;" &> /dev/null; then
    echo "✅ 数据库 $DB_NAME 已就绪"
else
    echo "❌ 数据库未就绪，请先运行 01-init-db.sh 初始化"
    exit 1
fi
echo ""

# 第2步：后端编译
echo "[2/4] 编译后端（Maven）..."
cd "$PROJECT_ROOT/backend"

if command -v mvn &> /dev/null; then
    echo "  使用系统 Maven 编译..."
    mvn clean package -DskipTests -q
    if [ $? -eq 0 ]; then
        echo "  ✅ 后端编译成功"
    else
        echo "  ⚠️  Maven 编译返回非零，可能有警告或错误"
    fi
elif [ -f mvnw ]; then
    echo "  使用 Maven Wrapper 编译..."
    chmod +x mvnw
    ./mvnw clean package -DskipTests -q
    if [ $? -eq 0 ]; then
        echo "  ✅ 后端编译成功（Maven Wrapper）"
    else
        echo "  ⚠️  编译返回非零"
    fi
else
    echo "  ⚠️  未找到 Maven，请手动编译或使用 IDE 打开项目"
fi
echo ""

# 第3步：用户前端依赖安装
echo "[3/4] 安装用户前端依赖（user-web）..."
cd "$PROJECT_ROOT/user-web"

if command -v npm &> /dev/null; then
    npm install
    if [ $? -eq 0 ]; then
        echo "  ✅ user-web 依赖安装成功"
    else
        echo "  ⚠️  npm install 返回非零"
    fi
else
    echo "  ❌ 未检测到 npm，请先安装 Node.js"
fi
echo ""

# 第4步：管理前端依赖安装
echo "[4/4] 安装管理前端依赖（admin-web）..."
cd "$PROJECT_ROOT/admin-web"

if command -v npm &> /dev/null; then
    npm install
    if [ $? -eq 0 ]; then
        echo "  ✅ admin-web 依赖安装成功"
    else
        echo "  ⚠️  npm install 返回非零"
    fi
else
    echo "  ❌ 未检测到 npm，请先安装 Node.js"
fi
echo ""

echo "============================================================"
echo "   项目初始化完成！"
echo "============================================================"
echo ""
echo "🚀 下一步："
echo "   启动全部服务：运行 99-start-all.sh"
echo "   或分别启动："
echo "     05-start-backend.sh     启动后端"
echo "     06-start-user-web.sh    启动用户前端"
echo "     07-start-admin-web.sh   启动管理前端"
echo ""
