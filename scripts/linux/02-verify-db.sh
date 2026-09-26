#!/bin/bash
# =============================================================
# 青启e城 · 02 数据库验证脚本（Linux）
# 功能：验证数据库连接、表数量、核心数据完整性
# =============================================================

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
source "$SCRIPT_DIR/config.sh"

echo "============================================================"
echo "   青启e城 · 数据库验证"
echo "   目标：${MYSQL_HOST}:${MYSQL_PORT} / ${DB_NAME}"
echo "============================================================"
echo ""

# 测试连接
echo "[1/5] 测试连接..."
if ! mysql -h"$MYSQL_HOST" -P"$MYSQL_PORT" -u"$MYSQL_USER" -p"$MYSQL_PASSWORD" -e "SELECT 1;" &> /dev/null; then
    echo "❌ 连接失败"
    exit 1
fi
echo "✅ 连接成功"
echo ""

# 检查数据库
echo "[2/5] 检查数据库..."
if ! mysql -h"$MYSQL_HOST" -P"$MYSQL_PORT" -u"$MYSQL_USER" -p"$MYSQL_PASSWORD" -e "USE $DB_NAME;" &> /dev/null; then
    echo "❌ 数据库 $DB_NAME 不存在"
    echo "   请先运行 01-init-db.sh 初始化"
    exit 1
fi
echo "✅ 数据库 $DB_NAME 存在"
echo ""

# 检查表数量
echo "[3/5] 检查表数量..."
TABLE_COUNT=$(mysql -h"$MYSQL_HOST" -P"$MYSQL_PORT" -u"$MYSQL_USER" -p"$MYSQL_PASSWORD" -N -e \
    "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema='${DB_NAME}';")
echo "  实际表数量：${TABLE_COUNT} 张"
echo "  预期表数量：25 张"
if [ "$TABLE_COUNT" -eq 25 ]; then
    echo "  ✅ 表数量正确"
else
    echo "  ❌ 表数量不符"
fi
echo ""

# 检查核心表
echo "[4/5] 检查核心表数据..."

USER_COUNT=$(mysql -h"$MYSQL_HOST" -P"$MYSQL_PORT" -u"$MYSQL_USER" -p"$MYSQL_PASSWORD" -N -e "SELECT COUNT(*) FROM ${DB_NAME}.sys_user;")
echo "  sys_user（用户表）：${USER_COUNT} 条"

ROLE_COUNT=$(mysql -h"$MYSQL_HOST" -P"$MYSQL_PORT" -u"$MYSQL_USER" -p"$MYSQL_PASSWORD" -N -e "SELECT COUNT(*) FROM ${DB_NAME}.sys_role;")
echo "  sys_role（角色表）：${ROLE_COUNT} 条"

MSG_COUNT=$(mysql -h"$MYSQL_HOST" -P"$MYSQL_PORT" -u"$MYSQL_USER" -p"$MYSQL_PASSWORD" -N -e "SELECT COUNT(*) FROM ${DB_NAME}.sys_message;")
echo "  sys_message（消息表）：${MSG_COUNT} 条"

GUAR_COUNT=$(mysql -h"$MYSQL_HOST" -P"$MYSQL_PORT" -u"$MYSQL_USER" -p"$MYSQL_PASSWORD" -N -e "SELECT COUNT(*) FROM ${DB_NAME}.biz_guarantee;")
echo "  biz_guarantee（保函表）：${GUAR_COUNT} 条"

LOAN_COUNT=$(mysql -h"$MYSQL_HOST" -P"$MYSQL_PORT" -u"$MYSQL_USER" -p"$MYSQL_PASSWORD" -N -e "SELECT COUNT(*) FROM ${DB_NAME}.biz_loan_application;")
echo "  biz_loan_application（贷款申请）：${LOAN_COUNT} 条"

TX_COUNT=$(mysql -h"$MYSQL_HOST" -P"$MYSQL_PORT" -u"$MYSQL_USER" -p"$MYSQL_PASSWORD" -N -e "SELECT COUNT(*) FROM ${DB_NAME}.biz_transaction;")
echo "  biz_transaction（交易记录）：${TX_COUNT} 条"
echo ""

# 检查字符集
echo "[5/5] 检查字符集..."
CHARSET=$(mysql -h"$MYSQL_HOST" -P"$MYSQL_PORT" -u"$MYSQL_USER" -p"$MYSQL_PASSWORD" -N -e \
    "SELECT DEFAULT_CHARACTER_SET_NAME FROM information_schema.SCHEMATA WHERE SCHEMA_NAME='${DB_NAME}';")
echo "  数据库字符集：${CHARSET}"
if [ "$CHARSET" = "utf8mb4" ]; then
    echo "  ✅ 字符集正确（utf8mb4）"
else
    echo "  ⚠️  字符集不是 utf8mb4，可能导致中文乱码"
fi
echo ""

echo "============================================================"
echo "   验证完成"
echo "============================================================"
echo ""
echo "💡 如全部 ✅，数据库运行正常。"
