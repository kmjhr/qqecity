#!/bin/bash
# =============================================================
# 青启e城 · 03 Redis 验证脚本（Linux）
# 功能：验证 Redis 连接、基本读写、配置状态
# =============================================================

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
source "$SCRIPT_DIR/config.sh"

echo "============================================================"
echo "   青启e城 · Redis 验证"
echo "   目标：${REDIS_HOST}:${REDIS_PORT}"
echo "============================================================"
echo ""

# 构建 redis-cli 命令参数
REDIS_CMD="redis-cli -h $REDIS_HOST -p $REDIS_PORT"
if [ -n "$REDIS_PASSWORD" ]; then
    REDIS_CMD="$REDIS_CMD -a $REDIS_PASSWORD --no-auth-warning"
fi

# 测试连接
echo "[1/6] 测试连接（PING）..."
PONG=$($REDIS_CMD ping 2>/dev/null)
if [ "$PONG" = "PONG" ]; then
    echo "  ✅ 连接成功，返回 PONG"
else
    echo "  ❌ 连接失败"
    echo "     请检查："
    echo "     1. Redis 服务是否已启动"
    echo "     2. config.sh 中的地址/端口/密码是否正确"
    echo "     3. 防火墙是否放行 6379 端口"
    echo "     4. bind 配置是否允许远程连接"
    exit 1
fi
echo ""

# 版本信息
echo "[2/6] Redis 版本..."
VER_LINE=$($REDIS_CMD INFO server 2>/dev/null | grep "redis_version")
echo "  $VER_LINE"
echo ""

# 基本读写测试
echo "[3/6] 基本读写测试..."
$REDIS_CMD SET test:ping pong > /dev/null
VAL=$($REDIS_CMD GET test:ping 2>/dev/null)
if [ "$VAL" = "pong" ]; then
    echo "  ✅ SET/GET 正常"
else
    echo "  ❌ SET/GET 失败"
fi
$REDIS_CMD DEL test:ping > /dev/null
echo ""

# 内存使用
echo "[4/6] 内存使用..."
MEM_LINE=$($REDIS_CMD INFO memory 2>/dev/null | grep "used_memory_human")
echo "  $MEM_LINE"
DBSIZE=$($REDIS_CMD dbsize 2>/dev/null)
echo "  当前数据库 key 数量：$DBSIZE"
echo ""

# 持久化状态
echo "[5/6] 持久化状态..."
SAVE_LINE=$($REDIS_CMD CONFIG GET save 2>/dev/null | tail -1)
echo "  RDB 保存策略：已配置"
AOF_STATUS=$($REDIS_CMD CONFIG GET appendonly 2>/dev/null | tail -1)
echo "  AOF 状态：$AOF_STATUS"
echo ""

# maxmemory 配置
echo "[6/6] 最大内存配置..."
MAXMEM=$($REDIS_CMD CONFIG GET maxmemory 2>/dev/null | tail -1)
if [ "$MAXMEM" = "0" ]; then
    echo "  maxmemory：未设置（不限制）"
else
    echo "  maxmemory：${MAXMEM} 字节"
fi
POLICY=$($REDIS_CMD CONFIG GET maxmemory-policy 2>/dev/null | tail -1)
echo "  淘汰策略：$POLICY"
echo ""

echo "============================================================"
echo "   验证完成"
echo "============================================================"
echo ""
echo "💡 Redis 运行正常。MVP 阶段为可选依赖，不启用也能跑通核心功能。"
