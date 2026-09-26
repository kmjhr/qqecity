#!/bin/bash
# =============================================================
# 青启e城 · 05 启动后端（Linux）
# 功能：启动 Spring Boot 后端服务
# =============================================================

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
source "$SCRIPT_DIR/config.sh"

PROJECT_ROOT="$(cd "$SCRIPT_DIR/../.." && pwd)"
JAR_FILE="$PROJECT_ROOT/backend/target/qingqi-backend.jar"

echo "============================================================"
echo "   青启e城 · 启动后端服务"
echo "   端口：$BACKEND_PORT"
echo "============================================================"
echo ""

# 检查 jar 包是否存在
if [ -f "$JAR_FILE" ]; then
    echo "✅ 找到已编译的 jar 包"
else
    echo "⚠️  未找到 jar 包，尝试编译..."
    cd "$PROJECT_ROOT/backend"

    if command -v mvn &> /dev/null; then
        mvn clean package -DskipTests -q
    elif [ -f mvnw ]; then
        chmod +x mvnw
        ./mvnw clean package -DskipTests -q
    else
        echo "❌ 未找到 Maven，请先手动编译后端"
        exit 1
    fi

    if [ -f "$JAR_FILE" ]; then
        echo "✅ 编译成功"
    else
        echo "❌ 编译失败，请检查错误信息"
        exit 1
    fi
fi
echo ""

# 启动
echo "🚀 正在启动后端服务..."
echo "   访问地址：http://localhost:${BACKEND_PORT}"
echo "   接口文档：http://localhost:${BACKEND_PORT}/doc.html（如集成 knife4j）"
echo ""
echo "   按 Ctrl+C 停止服务"
echo ""

cd "$PROJECT_ROOT/backend"
java -jar "$JAR_FILE" --server.port="$BACKEND_PORT"

echo ""
echo "后端服务已停止。"
