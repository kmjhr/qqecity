# 青启e城 · 后端配置指南

> 本文档属于「本地部署与配置指南」系列，完整目录见 [README.md](./README.md)

> 适用场景：配置和修改 Spring Boot 后端的各项参数，包括数据库、Redis、JWT、日志等。
> 配置文件位置：`backend/src/main/resources/application.yml`

---

## 目录

- [一、完整配置示例](#一完整配置示例)
- [二、配置项详解](#二配置项详解)
- [三、多环境配置说明](#三多环境配置说明)
- [四、常用配置修改场景](#四常用配置修改场景)
- [五、配置最佳实践](#五配置最佳实践)

---

## 一、完整配置示例

以下是 `application.yml` 的完整配置，带中文注释，可直接复制使用：

```yaml
# ==================== 服务配置 ====================
server:
  port: 8080                    # 服务端口
  servlet:
    context-path: /             # 上下文路径（默认根路径）
    encoding:
      charset: UTF-8            # 请求响应编码
      enabled: true
      force: true

# ==================== Spring 配置 ====================
spring:
  application:
    name: qingqi-ecity          # 应用名称

  # ---------- 数据源配置 ----------
  datasource:
    driver-class-name: com.mysql.cj.jdbc.Driver
    url: jdbc:mysql://127.0.0.1:3306/qingqi?useUnicode=true&characterEncoding=utf8&useSSL=false&serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=true
    username: root
    password: "123456"
    # 连接池配置（HikariCP）
    hikari:
      minimum-idle: 5           # 最小空闲连接
      maximum-pool-size: 20     # 最大连接数
      idle-timeout: 30000       # 空闲连接超时时间（毫秒）
      max-lifetime: 1800000     # 连接最大生命周期（毫秒）
      connection-timeout: 30000 # 获取连接超时时间（毫秒）

  # ---------- Redis 配置（可选） ----------
  data:
    redis:
      host: 127.0.0.1           # Redis 主机地址
      port: 6379                # Redis 端口
      password: ""              # Redis 密码（无密码则留空或删除该行）
      database: 0               # 数据库索引（0-15）
      timeout: 3000ms           # 连接超时时间
      client-type: lettuce      # 客户端类型（lettuce / jedis）
      # Lettuce 连接池
      lettuce:
        pool:
          max-active: 8         # 最大连接数
          max-idle: 8           # 最大空闲连接
          min-idle: 0           # 最小空闲连接
          max-wait: -1ms        # 最大阻塞等待时间（-1 不限制）

  # ---------- 文件上传 ----------
  servlet:
    multipart:
      max-file-size: 10MB       # 单个文件最大
      max-request-size: 50MB    # 请求最大

# ==================== MyBatis-Plus 配置 ====================
mybatis-plus:
  mapper-locations: classpath:mapper/**/*.xml    # Mapper XML 路径
  type-aliases-package: com.icbc.qingqi.module.**.entity  # 实体类包路径
  configuration:
    map-underscore-to-camel-case: true           # 下划线转驼峰
    log-impl: org.apache.ibatis.logging.stdout.StdOutImpl  # SQL 日志（开发环境）
  global-config:
    db-config:
      id-type: auto              # 主键策略：自增
      logic-delete-field: deleted               # 逻辑删除字段名
      logic-delete-value: 1     # 逻辑删除值（已删除）
      logic-not-delete-value: 0  # 逻辑未删除值

# ==================== JWT 配置 ====================
jwt:
  secret: "qingqi-ecity-jwt-secret-key-2024"   # JWT 签名密钥（生产环境请修改）
  expiration: 86400000        # Token 过期时间（毫秒），默认 24 小时
  header: Authorization       # 请求头名称
  prefix: "Bearer "           # Token 前缀
  # refresh-expiration: 604800000  # 刷新 Token 过期时间（7 天，可选）

# ==================== 日志配置 ====================
logging:
  level:
    root: INFO                 # 根日志级别
    com.icbc.qingqi: DEBUG     # 项目包日志级别（开发环境设 DEBUG）
    org.springframework.web: INFO
    com.zaxxer.hikari: INFO
  pattern:
    console: "%d{yyyy-MM-dd HH:mm:ss.SSS} [%thread] %-5level %logger{50} - %msg%n"
  # 日志文件（可选）
  # file:
  #   name: logs/qingqi.log
  #   max-size: 10MB
  #   max-history: 30

# ==================== 跨域配置（可选，代码中已有 CorsConfig） ====================
# 如需在配置文件中控制跨域，可自定义配置项
# cors:
#   allowed-origins: "*"
#   allowed-methods: "GET,POST,PUT,DELETE,OPTIONS"
#   allowed-headers: "*"
#   allow-credentials: true
#   max-age: 3600

# ==================== 其他业务配置 ====================
# 可根据业务需要扩展自定义配置项
# qingqi:
#   upload-path: /data/uploads/
#   default-avatar: /images/default-avatar.png
```

---

## 二、配置项详解

### 2.1 数据源配置

| 配置项 | 说明 | 默认值 | 注意事项 |
|--------|------|--------|----------|
| `driver-class-name` | JDBC 驱动类 | `com.mysql.cj.jdbc.Driver` | MySQL 8.0 用 `cj` 包下的驱动 |
| `url` | 数据库连接地址 | `jdbc:mysql://127.0.0.1:3306/qingqi` | 必须包含 `serverTimezone` 参数 |
| `username` | 数据库用户名 | `root` | 生产环境建议使用专用账号 |
| `password` | 数据库密码 | `123456` | 生产环境建议用环境变量注入 |

**URL 参数说明：**

| 参数 | 说明 |
|------|------|
| `useUnicode=true` | 启用 Unicode 字符集 |
| `characterEncoding=utf8` | 字符编码为 UTF-8 |
| `useSSL=false` | 关闭 SSL（开发环境，生产建议开启） |
| `serverTimezone=Asia/Shanghai` | 时区设置（避免时间差 8 小时） |
| `allowPublicKeyRetrieval=true` | 允许客户端获取公钥（解决 MySQL 8.0 认证问题） |

### 2.2 Redis 配置

| 配置项 | 说明 | 默认值 |
|--------|------|--------|
| `host` | Redis 主机地址 | `127.0.0.1` |
| `port` | Redis 端口 | `6379` |
| `password` | Redis 密码 | 空（无密码） |
| `database` | 数据库索引 | `0`（0-15） |
| `timeout` | 连接超时 | `3000ms` |
| `lettuce.pool.max-active` | 连接池最大连接数 | `8` |
| `lettuce.pool.max-idle` | 最大空闲连接 | `8` |
| `lettuce.pool.min-idle` | 最小空闲连接 | `0` |

> Redis 为可选依赖，不配置或连接失败不影响核心业务流程（MVP 阶段）。

### 2.3 服务端口配置

| 配置项 | 说明 | 默认值 |
|--------|------|--------|
| `server.port` | HTTP 服务端口 | `8080` |
| `server.servlet.context-path` | 上下文路径 | `/`（根路径） |

### 2.4 MyBatis-Plus 配置

| 配置项 | 说明 | 默认值 |
|--------|------|--------|
| `mapper-locations` | Mapper XML 文件路径 | `classpath:mapper/**/*.xml` |
| `type-aliases-package` | 实体类包扫描路径 | `com.icbc.qingqi.module.**.entity` |
| `map-underscore-to-camel-case` | 下划线转驼峰 | `true`（启用） |
| `log-impl` | SQL 日志实现 | `StdOutImpl`（控制台打印） |
| `id-type` | 主键生成策略 | `auto`（自增） |
| `logic-delete-field` | 逻辑删除字段 | `deleted` |
| `logic-delete-value` | 已删除标识 | `1` |
| `logic-not-delete-value` | 未删除标识 | `0` |

> 生产环境建议关闭 SQL 日志（`log-impl` 设为 `NO_LOGGING` 或使用 logger 级别控制），避免影响性能。

### 2.5 JWT 配置

| 配置项 | 说明 | 默认值 |
|--------|------|--------|
| `jwt.secret` | 签名密钥 | `qingqi-ecity-jwt-secret-key-2024` |
| `jwt.expiration` | Token 过期时间（毫秒） | `86400000`（24 小时） |
| `jwt.header` | 请求头名称 | `Authorization` |
| `jwt.prefix` | Token 前缀 | `Bearer ` |

> ⚠️ 生产环境必须修改 `secret` 为强密钥（建议 32 位以上随机字符串），并通过环境变量注入。

### 2.6 日志配置

| 配置项 | 说明 | 推荐值 |
|--------|------|--------|
| `logging.level.root` | 根日志级别 | `INFO` |
| `logging.level.com.icbc.qingqi` | 项目包日志级别 | 开发 `DEBUG`，生产 `INFO` |
| `logging.pattern.console` | 控制台输出格式 | 见完整示例 |
| `logging.file.name` | 日志文件名（可选） | `logs/qingqi.log` |

日志级别从低到高：`TRACE < DEBUG < INFO < WARN < ERROR`

---

## 三、多环境配置说明

Spring Boot 支持多环境配置，通过 `spring.profiles.active` 切换。

### 3.1 配置文件结构

```
resources/
├── application.yml           # 主配置文件（公共配置）
├── application-dev.yml       # 开发环境配置
├── application-test.yml      # 测试环境配置
└── application-prod.yml      # 生产环境配置
```

### 3.2 切换环境

在 `application.yml` 中指定：

```yaml
spring:
  profiles:
    active: dev    # 可选值：dev / test / prod
```

**或通过启动参数指定：**

```bash
# Maven 启动时指定
mvn spring-boot:run -Dspring-boot.run.profiles=test

# JAR 包启动时指定
java -jar qingqi-ecity.jar --spring.profiles.active=prod

# 环境变量方式
SPRING_PROFILES_ACTIVE=prod java -jar qingqi-ecity.jar
```

### 3.3 各环境配置差异示例

**application-dev.yml（开发环境）：**

```yaml
server:
  port: 8080

spring:
  datasource:
    url: jdbc:mysql://127.0.0.1:3306/qingqi?useSSL=false&serverTimezone=Asia/Shanghai
    username: root
    password: "123456"
  data:
    redis:
      host: 127.0.0.1
      password: ""

logging:
  level:
    com.icbc.qingqi: DEBUG

mybatis-plus:
  configuration:
    log-impl: org.apache.ibatis.logging.stdout.StdOutImpl
```

**application-prod.yml（生产环境）：**

```yaml
server:
  port: 8080

spring:
  datasource:
    url: jdbc:mysql://${DB_HOST}:${DB_PORT}/${DB_NAME}?useSSL=true&serverTimezone=Asia/Shanghai
    username: ${DB_USERNAME}
    password: ${DB_PASSWORD}
  data:
    redis:
      host: ${REDIS_HOST}
      password: ${REDIS_PASSWORD}

logging:
  level:
    com.icbc.qingqi: INFO
  file:
    name: /var/log/qingqi/qingqi.log

mybatis-plus:
  configuration:
    log-impl: org.apache.ibatis.logging.nologging.NoLoggingImpl
```

> 生产环境敏感信息通过环境变量注入，不要写死在配置文件中。

---

## 四、常用配置修改场景

### 4.1 修改数据库连接

**场景：** 数据库地址、端口、账号密码变更。

修改 `spring.datasource` 下的配置：

```yaml
spring:
  datasource:
    url: jdbc:mysql://新IP:新端口/qingqi?useUnicode=true&characterEncoding=utf8&useSSL=false&serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=true
    username: 新用户名
    password: "新密码"
```

修改后重启服务即可生效。

### 4.2 修改服务端口

**场景：** 8080 端口被占用，或需要部署多个实例。

```yaml
server:
  port: 8081    # 修改为可用端口
```

> 同时需要修改前端 Vite 代理配置中的目标端口。

### 4.3 启用 / 禁用 Redis

**启用 Redis：** 确保 `spring.data.redis` 配置正确，Redis 服务已启动。

**禁用 Redis（开发环境）：**
- 保持默认配置即可，Redis 连接失败不影响核心功能
- 如需完全禁用，可注释掉 Redis 相关配置和代码中的 `@EnableRedisRepositories` 等注解

### 4.4 修改 JWT 密钥

**场景：** 密钥泄露或生产环境部署。

```yaml
jwt:
  secret: "你的新密钥（建议32位以上随机字符串）"
  expiration: 86400000
```

> 修改后，已签发的所有 Token 将失效，用户需要重新登录。

### 4.5 关闭 SQL 日志

**场景：** 生产环境避免打印 SQL 影响性能。

```yaml
mybatis-plus:
  configuration:
    log-impl: org.apache.ibatis.logging.nologging.NoLoggingImpl
```

或通过日志级别控制：

```yaml
logging:
  level:
    com.icbc.qingqi: INFO
```

---

## 五、配置最佳实践

### 5.1 敏感信息安全

- **不要把密码、密钥等敏感信息提交到 Git**
- 生产环境使用**环境变量**注入敏感配置：

```yaml
spring:
  datasource:
    password: ${DB_PASSWORD:default_value}
  data:
    redis:
      password: ${REDIS_PASSWORD:}
jwt:
  secret: ${JWT_SECRET:default-secret}
```

- 使用 `.env` 文件或配置中心（如 Nacos、Apollo）管理敏感配置

### 5.2 配置文件管理

- `application.yml` 存放公共配置
- `application-dev.yml` 存放开发环境配置（可提交到 Git，使用弱密码和本地地址）
- `application-prod.yml` 只保留非敏感配置占位符，敏感值通过环境变量注入
- 个人本地配置差异使用 `application-local.yml`（加入 `.gitignore`）

### 5.3 .gitignore 建议

确保以下文件不被提交：

```gitignore
# 本地配置文件
application-local.yml
application-dev-local.yml

# 环境变量文件
.env
.env.local

# IDE 配置
.idea/
.vscode/
*.iml

# 日志文件
logs/
*.log
```

### 5.4 配置验证

修改配置后，可通过以下方式验证：

1. **启动验证**：启动服务，观察控制台有无报错
2. **健康检查**：访问 `http://localhost:8080/api/health`
3. **数据库验证**：调用一个查询接口，看是否正常返回数据
4. **Redis 验证**：登录后检查 Redis 中是否有 Token 相关数据（如启用）

### 5.5 其他建议

- 配置项命名统一使用小写字母 + 中划线（kebab-case）
- 自定义业务配置建议加前缀（如 `qingqi.xxx`），避免与框架配置冲突
- 重要配置修改后在团队内同步，避免其他人本地配置不一致
- 生产环境配置变更前先在测试环境验证

---

## 配置检查清单

- [ ] 数据库 URL、用户名、密码配置正确
- [ ] 数据库 URL 包含 `serverTimezone=Asia/Shanghai`
- [ ] Redis 配置正确（如启用）
- [ ] 服务端口未被占用
- [ ] JWT 密钥已修改（生产环境）
- [ ] 日志级别适合当前环境（开发 DEBUG / 生产 INFO）
- [ ] 敏感信息未硬编码，使用环境变量注入（生产环境）
- [ ] `application-local.yml` 已加入 `.gitignore`
- [ ] 配置修改后服务可正常启动
