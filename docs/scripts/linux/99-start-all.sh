#!/bin/bash
# =============================================================
# 青启e城 · 99 一键全部启动（Linux）
# 功能：依次启动 后端 + 用户前端 + 管理前端
# 注意：前端会在新终端打开，后端在当前终端运行
# =============================================================

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
source "$SCRIPT_DIR/config.sh"

echo "============================================================"
echo "   青启e城 · 一键启动全部服务"
echo "============================================================"
echo ""
echo "即将启动以下服务："
echo "  [1] 后端服务    http://localhost:${BACKEND_PORT}"
echo "  [2] 用户前端    http://localhost:${USER_WEB_PORT}"
echo "  [3] 管理前端    http://localhost:${ADMIN_WEB_PORT}"
echo ""
echo "说明："
echo "  - 用户前端和管理前端会在后台运行（输出重定向到日志文件）"
echo "  - 后端服务在当前终端运行"
echo "  - 停止后端按 Ctrl+C"
echo "  - 停止前端请使用 kill 命令或关闭终端"
echo ""
read -p "按 Enter 继续..." -r

echo ""
echo "🚀 [1/3] 启动用户前端（后台运行）..."
cd "$(dirname "$0")"
nohup bash 06-start-user-web.sh > /tmp/qingqi-user-web.log 2>&1 &
USER_WEB_PID=$!
echo "   PID: $USER_WEB_PID"
echo "   日志: /tmp/qingqi-user-web.log"
sleep 2

echo ""
echo "🚀 [2/3] 启动管理前端（后台运行）..."
nohup bash 07-start-admin-web.sh > /tmp/qingqi-admin-web.log 2>&1 &
ADMIN_WEB_PID=$!
echo "   PID: $ADMIN_WEB_PID"
echo "   日志: /tmp/qingqi-admin-web.log"
sleep 2

echo ""
echo "🚀 [3/3] 启动后端服务（当前终端）..."
echo ""

bash 05-start-backend.sh

# 后端停止后，提示停止前端
echo ""
echo "后端服务已停止。"
echo ""
echo "前端进程仍在后台运行："
echo "  用户前端 PID: $USER_WEB_PID"
echo "  管理前端 PID: $ADMIN_WEB_PID"
echo ""
echo "如需停止前端，执行："
echo "  kill $USER_WEB_PID $ADMIN_WEB_PID"
echo ""
