# ---------- 构建阶段 ----------
FROM maven:3.9-eclipse-temurin-17 AS builder
WORKDIR /app
# 阿里云 Maven 镜像（避免容器内直连 Central TLS 握手失败）
COPY settings.xml /root/.m2/settings.xml
COPY pom.xml .
COPY src ./src
# BuildKit 缓存挂载：Maven 依赖缓存在宿主，源码改动不再全量重下依赖
RUN --mount=type=cache,target=/root/.m2 mvn package -DskipTests -B

# ---------- 运行阶段 ----------
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY --from=builder /app/target/qingqi-backend.jar app.jar
ENV TZ=Asia/Shanghai
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
