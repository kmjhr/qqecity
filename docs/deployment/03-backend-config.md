# 青启e城 · 后端配置指南

> 本文档属于「本地部署与配置指南」系列，完整目录见 [README.md](./README.md)

> 适用场景：配置和修改 Spring Boot 后端的各项参数，包括数据库、Redis、JWT、接口文档、日志等。
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

以下是项目实际使用的 `application.yml`（与代码仓库保持一致），已带中文注释：

```yaml
# ==================== 服务配置 ====================
server:
  port: 8080                    # 服务端口
  servlet:
    context-path: /api          # 接口统一前缀（所有接口位于 /api 下，勿改为 / 否则前端代理失效）

# ==================== Spring 配置 ====================
spring:
  application:
    name: qingqi-backend        # 应用名称

  # ---------- 数据源配置 ----------
  # 注意：本地部署默认使用 root/123456（与部署文档、脚本 config 保持一致）。
  # 若你安装 MySQL 时设置了其他密码，请修改下方默认值，或通过环境变量 SPRING_DATASOURCE_PASSWORD 注入。
  datasource:
    url: ${SPRING_DATASOURCE_URL:jdbc:mysql://localhost:3306/qingqi?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai&useSSL=false&allowPublicKeyRetrieval=true}
    username: ${SPRING_DATASOURCE_USERNAME:root}
    password: ${SPRING_DATASOURCE_PASSWORD:123456}
    driver-class-name: com.mysql.cj.jdbc.Driver

  # ---------- Redis 配置（JWT 黑名单 + 缓存） ----------
  data:
    redis:
      host: ${SPRING_DATA_REDIS_HOST:localhost}
      port: 6379                # Redis 端口
      password: ${SPRING_DATA_REDIS_PASSWORD:}   # Redis 密码（无密码则留空）
      timeout: 3000ms           # 连接超时时间

  # ---------- JSON 序列化 ----------
  jackson:
    date-format: yyyy-MM-dd HH:mm:ss   # 日期格式
    time-zone: GMT+8                   # 时区
    default-property-inclusion: non_null # 空值不序列化

# ==================== MyBatis-Plus 配置 ====================
mybatis-plus:
  mapper-locations: classpath*:mapper/**/*.xml    # Mapper XML 路径
  configuration:
    map-underscore-to-camel-case: true            # 下划线转驼峰
    log-impl: org.apache.ibatis.logging.stdout.StdOutImpl  # SQL 日志（开发环境）
  global-config:
    db-config:
      id-type: auto              # 主键策略：自增
      logic-delete-field: deleted               # 逻辑删除字段名
      logic-delete-value: 1      # 逻辑删除值（已删除）
      logic-not-delete-value: 0  # 逻辑未删除值

# ==================== JWT 配置（双令牌） ====================
jwt:
  # 签名密钥（Base64 编码，生产环境请通过环境变量 JWT_SECRET 注入）
  secret: ${JWT_SECRET:cWluZ3FpLWVjaXR5LWp3dC1zZWNyZXQta2V5LTIwMjQ=}
  # 访问令牌有效期（秒）- 默认 2 小时
  access-token-ttl: 7200
  # 刷新令牌有效期（秒）- 默认 7 天
  refresh-token-ttl: 604800
  # Token 前缀
  token-prefix: "Bearer "
  # 请求头字段
  header: Authorization

# ==================== Knife4j / SpringDoc 接口文档 ====================
knife4j:
  enable: true
  setting:
    language: zh_cn
springdoc:
  api-docs:
    enabled: true
    path: /v3/api-docs
  swagger-ui:
    enabled: true
    path: /swagger-ui.html

# ==================== 日志配置 ====================
logging:
  level:
    com.icbc.qingqi: debug       # 项目包日志级别（开发环境 debug，生产建议 info）
```

> ⚠️ **不要整段覆盖项目中的 application.yml**：示例即项目当前配置，如需修改请基于仓库内文件做增量修改（如只改密码、端口）。

---

## 二、配置项详解

### 2.1 数据源配置

| 配置项 | 说明 | 默认值 | 注意事项 |
|--------|------|--------|----------|
| `url` | 数据库连接地址 | `jdbc:mysql://localhost:3306/qingqi` | 必须包含 `serverTimezone` 参数 |
| `username` | 数据库用户名 | `root` | 生产环境建议使用专用账号 |
| `password` | 数据库密码 | `123456` | 与部署文档/脚本默认一致；生产用环境变量注入 |
| `driver-class-name` | JDBC 驱动类 | `com.mysql.cj.jdbc.Driver` | MySQL 8.0 用 `cj` 包下的驱动 |

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
| `host` | Redis 主机地址 | `localhost` |
| `port` | Redis 端口 | `6379` |
| `password` | Redis 密码 | 空（无密码） |
| `timeout` | 连接超时 | `3000ms` |

> Redis 用于 **JWT 黑名单**（登出后 Token 立即失效）与缓存，**推荐安装**。
> 未启动 Redis 时，后端会自动降级：跳过黑名单检查并打印警告，登录与核心业务仍可运行，但登出后 Token 不会立即失效（仅影响演示项目的登出/强制下线体验）。

### 2.3 服务端口与上下文路径

| 配置项 | 说明 | 默认值 |
|--------|------|--------|
| `server.port` | HTTP 服务端口 | `8080` |
| `server.servlet.context-path` | 上下文路径 | `/api`（所有接口统一前缀，前端代理依赖此路径，**勿改为 /**） |

### 2.4 MyBatis-Plus 配置

| 配置项 | 说明 | 默认值 |
|--------|------|--------|
| `mapper-locations` | Mapper XML 文件路径 | `classpath*:mapper/**/*.xml` |
| `map-underscore-to-camel-case` | 下划线转驼峰 | `true`（启用） |
| `log-impl` | SQL 日志实现 | `StdOutImpl`（控制台打印） |
| `id-type` | 主键生成策略 | `auto`（自增） |
| `logic-delete-field` | 逻辑删除字段 | `deleted` |
| `logic-delete-value` | 已删除标识 | `1` |
| `logic-not-delete-value` | 未删除标识 | `0` |

> 生产环境建议关闭 SQL 日志（`log-impl` 设为 `NO_LOGGING` 或使用 logger 级别控制），避免影响性能。

### 2.5 JWT 配置（双令牌）

| 配置项 | 说明 | 默认值 |
|--------|------|--------|
| `jwt.secret` | 签名密钥（Base64 编码） | `cWluZ3FpLWVjaXR5LWp3dC1zZWNyZXQta2V5LTIwMjQ=` |
| `jwt.access-token-ttl` | 访问令牌有效期（秒） | `7200`（2 小时） |
| `jwt.refresh-token-ttl` | 刷新令牌有效期（秒） | `604800`（7 天） |
| `jwt.token-prefix` | Token 前缀 | `Bearer ` |
| `jwt.header` | 请求头名称 | `Authorization` |

> ⚠️ 生产环境必须通过环境变量 `JWT_SECRET` 注入新的强密钥（Base64 编码，建议 32 字节以上随机值），不要使用默认值。

### 2.6 接口文档（Knife4j / SpringDoc）

| 配置项 | 说明 | 默认值 |
|--------|------|--------|
| `knife4j.enable` | 是否启用 Knife4j | `true` |
| `springdoc.api-docs.path` | OpenAPI JSON 路径 | `/v3/api-docs` |
| `springdoc.swagger-ui.path` | Swagger UI 路径 | `/swagger-ui.html` |

**访问地址**：`http://localhost:8080/api/doc.html`（本地手动启动时）。

### 2.7 日志配置

| 配置项 | 说明 | 推荐值 |
|--------|------|--------|
| `logging.level.com.icbc.qingqi` | 项目包日志级别 | 开发 `debug`，生产 `info` |

日志级别从低到高：`TRACE < DEBUG < INFO < WARN < ERROR`

---

## 三、多环境配置说明

> **当前项目仅提供 `application.yml` 单文件配置**（未拆分 dev/test/prod）。
> 以下为 Spring Boot 通用的多环境机制说明，需要时可自行扩展。

### 3.1 配置文件结构（可选扩展）

```
resources/
├── application.yml           # 主配置文件（公共配置）
├── application-dev.yml       # 开发环境配置（可选）
├── application-test.yml      # 测试环境配置（可选）
└── application-prod.yml      # 生产环境配置（可选）
```

### 3.2 切换环境（可选扩展）

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
java -jar qingqi-backend.jar --spring.profiles.active=prod

# 环境变量方式
SPRING_PROFILES_ACTIVE=prod java -jar qingqi-backend.jar
```

> 生产环境敏感信息通过环境变量注入，不要写死在配置文件中。

---

## 四、常用配置修改场景

### 4.1 修改数据库连接

**场景：** 数据库地址、端口、账号密码变更（与本地安装的 MySQL 不一致时必改）。

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

> 同时需要修改前端 Vite 代理配置中的目标端口（`user-web/vite.config.js`、`admin-web/vite.config.ts`）。

### 4.3 关于 Redis

**启用 Redis：** 确保 `spring.data.redis` 配置正确，Redis 服务已启动。

**未安装 Redis（开发/演示）：** 保持默认配置即可。后端会打印 `Redis 不可用，跳过 Token 黑名单检查` 警告并自动降级，登录与核心业务可正常运行。

### 4.4 修改 JWT 密钥

**场景：** 密钥泄露或生产环境部署。

```yaml
jwt:
  secret: ${JWT_SECRET:你的新Base64密钥}
```

> 生成 Base64 密钥示例：`echo -n "你的随机字符串" | base64`。
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
    com.icbc.qingqi: info
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

### 5.2 配置修改原则

- 基于仓库内现有 `application.yml` 做**增量修改**，不要整体覆盖
- 本地部署最常见需要改的是 `spring.datasource.password`（与你安装 MySQL 时设置的密码保持一致）

### 5.3 配置验证

修改配置后，可通过以下方式验证：

1. **启动验证**：启动服务，观察控制台有无报错
2. **健康检查**：访问 `http://localhost:8080/api/health`
3. **数据库验证**：调用一个查询接口，看是否正常返回数据
4. **接口文档**：访问 `http://localhost:8080/api/doc.html` 确认 Knife4j 正常加载

---

## 配置检查清单

- [ ] 数据库 URL、用户名、密码配置正确（`password` 与你安装 MySQL 时设置的一致）
- [ ] 数据库 URL 包含 `serverTimezone=Asia/Shanghai`
- [ ] Redis 配置正确（未安装时确认后端已打印降级警告而非报错退出）
- [ ] 服务端口未被占用
- [ ] JWT 密钥已通过环境变量注入（生产环境）
- [ ] 日志级别适合当前环境（开发 debug / 生产 info）
- [ ] 配置修改后服务可正常启动
