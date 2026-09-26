#!/bin/bash
# =============================================================
# 青启e城 · 06 启动用户前端（Linux）
# 功能：启动 user-web 开发服务器
# =============================================================

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
source "$SCRIPT_DIR/config.sh"

PROJECT_ROOT="$(cd "$SCRIPT_DIR/../.." && pwd)"
WEB_DIR="$PROJECT_ROOT/user-web"

echo "============================================================"
echo "   青启e城 · 启动用户前端（user-web）"
echo "   端口：$USER_WEB_PORT"
echo "============================================================"
echo ""

# 检查 node_modules
if [ ! -d "$WEB_DIR/node_modules" ]; then
    echo "⚠️  未检测到 node_modules，正在安装依赖..."
    cd "$WEB_DIR"
    if npm install; then
        echo "✅ 依赖安装完成"
    else
        echo "❌ 依赖安装失败"
        exit 1
    fi
    echo ""
fi

# 启动
echo "🚀 正在启动用户前端..."
echo "   访问地址：http://localhost:${USER_WEB_PORT}"
echo ""
echo "   按 Ctrl+C 停止服务"
echo ""

cd "$WEB_DIR"
npm run dev

echo ""
echo "用户前端已停止。"
