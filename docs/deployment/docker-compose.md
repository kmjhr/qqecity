> 【文档新增】Docker Compose 部署指南

# Docker Compose 部署指南

本文档说明如何通过 Docker Compose 一键启动青启e城演示系统（MySQL + Redis + 后端 + 用户前端 + 管理前端）。

## 服务概览

| 服务 | 镜像/构建 | 容器名 | 端口 | 说明 |
| --- | --- | --- | --- | --- |
| mysql | mysql:8.0 | qqecity-mysql | 3307:3306 | 数据库（宿主机 3307，容器内 3306），自动初始化 schema + 演示数据 |
| redis | redis:7-alpine | qqecity-redis | 6379:6379 | JWT 黑名单（backend 运行时依赖） |
| backend | 构建自 ./backend/Dockerfile | qqecity-backend | 8080:8080 | Spring Boot 后端 API |
| user-web | 构建自 ./user-web/Dockerfile | qqecity-user-web | 8081:80 | 用户端前端（Nginx 静态托管） |
| admin-web | 构建自 ./admin-web/Dockerfile | qqecity-admin-web | 8082:80 | 管理端前端（Nginx 静态托管） |

## 前置要求

- 已安装 Docker 与 Docker Compose（Docker Desktop 自带）
- 项目代码已克隆到本地

## 快速启动

> ⚠️ **第 1 步必须先准备 `.env` 文件**（重要）：
> `.env` 已被 `.gitignore` 忽略、**不在代码库中**，而 `docker-compose.yml` 强制要求 `JWT_SECRET` 变量（缺失会直接报错退出）。项目已提供模板 `.env.example`，复制一份即可：

```bash
# Windows
copy .env.example .env

# Linux / macOS
cp .env.example .env
```

`.env.example` 自带演示用 `JWT_SECRET`，直接复制即可启动；正式使用请改成自己的随机密钥（可用 `openssl rand -base64 32` 生成）。**关于 AI/LLM（可选）**：`.env` 中 `CHAT_ENGINE`、`CHAT_LLM_API_KEY`、`CHAT_LLM_ENDPOINT`、`CHAT_LLM_MODEL` 四项控制 AI 增强。全部留空则后端使用本地规则模式（离线稳定、AI 不生效）；如需启用 AI（智能客服 agent 模式 + 反诈情景演练），请按 [06-ai-llm.md](./06-ai-llm.md) 配置本机 Ollama 或云端 API，并确认 LLM 服务已启动——**LLM 服务未启动时 AI 会静默降级本地，聊天消息标注"AI 降级本地"**。

```bash
# 第 2 步：在项目根目录执行
docker compose up -d --build
```

启动顺序：mysql → redis → backend → user-web / admin-web

## 访问地址

启动成功后，可通过以下地址访问：

| 入口 | 地址 | 说明 |
| --- | --- | --- |
| 用户端 | http://localhost:8081 | 青年用户使用的主前端 |
| 管理端 | http://localhost:8082 | 运营/管理员使用的后台 |
| 后端 API | http://localhost:8080/api | Spring Boot 服务（接口统一 /api 前缀） |
| 接口文档 | http://localhost:8080/api/doc.html | Knife4j 接口在线文档 |
| MySQL | localhost:3307 | 数据库（用户：root / 密码：123456） |
| Redis | localhost:6379 | 缓存（无密码） |

> 端口说明：MySQL 宿主机映射为 **3307**（容器内仍为 3306），避免与本机已安装的 MySQL 服务（占用 3306）冲突；后端容器通过内部网络 `mysql:3306` 连接，不受影响。

## 常用命令

```bash
# 启动全部服务（后台运行）
docker compose up -d --build

# 查看服务状态
docker compose ps

# 查看全部日志
docker compose logs -f

# 查看指定服务日志
docker compose logs -f backend

# 停止并移除容器（保留数据卷）
docker compose down

# 停止并移除容器 + 数据卷（慎用！会清空数据库）
docker compose down -v

# 重启某个服务
docker compose restart backend
```

## 数据持久化

- MySQL 数据通过命名卷 `mysql-data` 持久化，容器删除后数据保留
- Redis 数据通过命名卷 `redis-data` 持久化，开启 AOF
- 如需重置数据库，执行 `docker compose down -v` 后重新启动

> ⚠️ **改过 SQL 后必须重建卷**：MySQL 初始化脚本（`backend/sql/*.sql`）**只在数据卷首次创建时执行一次**。如果之前已经启动过系统（已存在 `mysql-data` 卷），再更新 `schema.sql` / `data.sql` 后直接 `docker compose up` 不会生效，数据库仍是旧结构（表现为缺新表、缺演示数据）。
> 更新 SQL 后需要：
> ```bash
> docker compose down -v      # 删除容器 + 数据卷（会清空现有数据，注意备份）
> docker compose up -d --build
> ```

## 环境变量说明

| 变量 | 所在服务 | 默认值 | 说明 |
| --- | --- | --- | --- |
| MYSQL_ROOT_PASSWORD | mysql | 123456 | MySQL root 密码 |
| MYSQL_DATABASE | mysql | qingqi | 数据库名 |
| SPRING_DATASOURCE_URL | backend | jdbc:mysql://mysql:3306/qingqi... | 数据源连接串 |
| SPRING_DATASOURCE_USERNAME / PASSWORD | backend | root / 123456 | 数据源账号（复用 MySQL root） |
| SPRING_DATA_REDIS_HOST / PORT / PASSWORD | backend | redis / 6379 /（无密码） | Redis 连接信息 |

> 生产部署请务必修改默认密码，使用 `.env` 文件或环境变量覆盖。

## 初始化脚本

MySQL 首次启动时会自动执行 `backend/sql/` 目录下的 SQL：

1. `schema.sql` — 建表（41 张表）
2. `data.sql` — 演示数据（含演示账号）

演示账号（用户名登录，统一密码 `123456`）：`admin`、`testuser`、`entrepreneur`、`landlord01`、`banker01`

## 生产部署建议

1. 使用 `.env` 文件管理敏感信息（密码、JWT 密钥等），不要提交到代码库
2. 数据库端口不对外映射（去掉 `ports` 配置，仅容器间网络访问）
3. 前端通过 Nginx 反向代理统一入口，配置 HTTPS
4. 配置 `restart: unless-stopped` 保证服务器重启后服务自动恢复
5. 定期备份 MySQL 数据（使用 `mysqldump` 或 Docker 卷备份）
