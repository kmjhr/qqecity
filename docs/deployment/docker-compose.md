> 【文档新增】Docker Compose 部署指南

# Docker Compose 部署指南

本文档说明如何通过 Docker Compose 一键启动青启e城演示系统（MySQL + Redis + 后端 + 用户前端 + 管理前端）。

## 服务概览

| 服务 | 镜像/构建 | 容器名 | 端口 | 说明 |
| --- | --- | --- | --- | --- |
| mysql | mysql:8.0 | qqecity-mysql | 3306 | 数据库，自动初始化 schema + 演示数据 |
| redis | redis:7-alpine | qqecity-redis | 6379 | 缓存（一期预留，非强依赖） |
| backend | 构建自 ./backend/Dockerfile | qqecity-backend | 8080 | Spring Boot 后端 API |
| user-web | 构建自 ./user-web/Dockerfile | qqecity-user-web | 8081 | 用户端前端（Nginx 静态托管） |
| admin-web | 构建自 ./admin-web/Dockerfile | qqecity-admin-web | 8082 | 管理端前端（Nginx 静态托管） |

## 前置要求

- 已安装 Docker 与 Docker Compose（Docker Desktop 自带）
- 项目代码已克隆到本地

## 快速启动

```bash
# 在项目根目录执行
docker compose up -d --build
```

启动顺序：mysql → redis → backend → user-web / admin-web

## 访问地址

启动成功后，可通过以下地址访问：

| 入口 | 地址 | 说明 |
| --- | --- | --- |
| 用户端 | http://localhost:8081 | 青年用户使用的主前端 |
| 管理端 | http://localhost:8082 | 运营/管理员使用的后台 |
| 后端 API | http://localhost:8080 | Spring Boot 服务 |
| Swagger 文档 | http://localhost:8080/swagger-ui.html | 接口在线文档 |
| MySQL | localhost:3306 | 数据库（用户：qingqi / 密码：qingqi123） |
| Redis | localhost:6379 | 缓存（密码：redis123） |

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

## 环境变量说明

| 变量 | 所在服务 | 默认值 | 说明 |
| --- | --- | --- | --- |
| MYSQL_ROOT_PASSWORD | mysql | root123456 | MySQL root 密码 |
| MYSQL_DATABASE | mysql | qingqi | 数据库名 |
| MYSQL_USER / MYSQL_PASSWORD | mysql | qingqi / qingqi123 | 业务库用户名密码 |
| SPRING_DATASOURCE_URL | backend | jdbc:mysql://mysql:3306/qingqi... | 数据源连接串 |
| SPRING_DATASOURCE_USERNAME / PASSWORD | backend | qingqi / qingqi123 | 数据库账号 |
| SPRING_DATA_REDIS_HOST / PASSWORD | backend | redis / redis123 | Redis 连接信息 |

> 生产部署请务必修改默认密码，使用 `.env` 文件或环境变量覆盖。

## 初始化脚本

MySQL 首次启动时会自动执行 `backend/sql/` 目录下的 SQL：

1. `schema.sql` — 建表（25 张表）
2. `data.sql` — 演示数据（含演示账号）

演示账号：`13800000000 / 123456`

## 生产部署建议

1. 使用 `.env` 文件管理敏感信息（密码、JWT 密钥等），不要提交到代码库
2. 数据库端口不对外映射（去掉 `ports` 配置，仅容器间网络访问）
3. 前端通过 Nginx 反向代理统一入口，配置 HTTPS
4. 配置 `restart: unless-stopped` 保证服务器重启后服务自动恢复
5. 定期备份 MySQL 数据（使用 `mysqldump` 或 Docker 卷备份）
