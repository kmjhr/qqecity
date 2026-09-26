# 青启e城 · 本地部署与配置指南

> 从 GitHub 克隆到本地运行的完整指南，覆盖环境准备、项目初始化、配置详解与问题排查全流程。

---

## 快速导航

| 文档 | 说明 | 适用阶段 |
|------|------|----------|
| [01-env-preparation.md](./01-env-preparation.md) | 环境准备指南：JDK / Node.js / MySQL / Redis 等软件安装与验证 | 环境准备阶段 |
| [02-project-setup.md](./02-project-setup.md) | 项目克隆与初始化全流程：从 git clone 到服务启动的 7 步操作 | 项目初始化阶段 |
| [03-backend-config.md](./03-backend-config.md) | 后端配置指南：application.yml 完整配置说明与常见修改场景 | 后端配置阶段 |
| [04-frontend-config.md](./04-frontend-config.md) | 前端配置指南：环境变量、Vite 代理、双前端项目配置说明 | 前端配置阶段 |
| [05-troubleshooting.md](./05-troubleshooting.md) | 常见问题排查总览：环境 / 数据库 / Redis / 前后端启动 / 功能类问题 | 问题排查阶段 |

### 相关部署文档

| 文档 | 说明 |
|------|------|
| [mysql-deployment.md](./mysql-deployment.md) | MySQL 数据库部署文档（Windows / Linux 双平台 + 数据导入） |
| [redis-deployment.md](./redis-deployment.md) | Redis 缓存服务部署文档（Windows / Linux 双平台） |
| [docker-compose.md](./docker-compose.md) | Docker Compose 一键部署指南 |
| [database-design.md](./database-design.md) | 数据库设计与 ER 图说明 |
| [project-structure.md](./project-structure.md) | 项目目录结构说明 |

---

## 5 步快速上手

```
克隆代码 → 环境准备 → 数据库初始化 → 项目初始化 → 启动服务
   ↓          ↓            ↓              ↓            ↓
git clone  JDK/Node/     建库+导入       配置文件     后端8080
          MySQL/Redis    schema+data    application   前端5173/5174
                                        yml / .env
```

**第 1 步：克隆代码**

```bash
git clone <仓库地址> qingqi-ecity
cd qingqi-ecity
```

**第 2 步：环境准备**

安装 JDK 17+、Node.js 18+、MySQL 8.0、Redis 6.0+（可选）。详见 [01-env-preparation.md](./01-env-preparation.md)。

**第 3 步：数据库初始化**

创建数据库 `qingqi`，导入 `schema.sql` 和 `data.sql`。详见 [mysql-deployment.md](./mysql-deployment.md#三创建数据库与导入数据)。

**第 4 步：项目初始化**

修改后端 `application.yml` 和前端 `.env` 配置文件。详见 [02-project-setup.md](./02-project-setup.md)。

**第 5 步：启动服务**

- 后端：`mvn spring-boot:run`（端口 8080）
- 用户前端：`cd frontend/user-web && npm run dev`（端口 5173）
- 管理前端：`cd frontend/admin-web && npm run dev`（端口 5174）

---

## 演示账号速查表

> 所有演示账号密码统一为：`123456`

| 账号 | 角色 | 说明 | 登录端 |
|------|------|------|--------|
| `admin` | 超级管理员 | 拥有全部权限，可管理用户、角色、菜单等 | 管理端 |
| `manager` | 运营管理员 | 负责业务数据审核、内容管理等 | 管理端 |
| `user01` | 普通用户（青年创业者） | 租房保函 / 青创e贷 / 预算消费 / 金融安全 | 用户端 |
| `user02` | 普通用户（在校大学生） | 预算消费 / 金融安全为主 | 用户端 |
| `user03` | 普通用户（新市民） | 租房保函 / 金融安全为主 | 用户端 |

---

## 访问地址速查表

| 服务 | 地址 | 说明 |
|------|------|------|
| 后端 API | `http://localhost:8080` | Spring Boot 服务，接口前缀 `/api` |
| 接口文档 | `http://localhost:8080/doc.html` | Knife4j / Swagger 接口文档 |
| 用户前端 | `http://localhost:5173` | 青启e城用户端（Vue 3 + Vite） |
| 管理前端 | `http://localhost:5174` | 青启e城管理端（Vue 3 + Vite） |
| MySQL | `127.0.0.1:3306` | 数据库名：`qingqi`，账号：`root/123456` |
| Redis | `127.0.0.1:6379` | 默认无密码（内网测试用） |

---

## 相关资源

- **脚本目录**：`scripts/` — Windows / Linux 一键启动脚本、数据库导入脚本
  - `scripts/windows/` — Windows 平台脚本
  - `scripts/linux/` — Linux 平台脚本
  - `scripts/README.md` — 脚本使用说明
- **技术选型文档**：`docs/01-技术选型明细.md`
- **项目目录结构**：[project-structure.md](./project-structure.md)
- **数据库设计**：[database-design.md](./database-design.md)
- **Docker 部署**：[docker-compose.md](./docker-compose.md)

---

## 部署检查清单

本地部署完成后，按以下清单逐项确认：

- [ ] JDK 17+ 已安装，`java -version` 正常
- [ ] Node.js 18+ 已安装，`node -v` 正常
- [ ] MySQL 8.0 已启动，数据库 `qingqi` 已创建
- [ ] schema.sql 已导入，表数量 = 25
- [ ] data.sql 已导入，5 个演示账号可登录
- [ ] Redis 已启动（可选，MVP 阶段可不启用）
- [ ] 后端 application.yml 数据库 / Redis 配置正确
- [ ] 后端启动成功，端口 8080 无报错
- [ ] 前端 .env 配置正确，`npm install` 无报错
- [ ] 用户前端启动成功，端口 5173 可访问
- [ ] 管理前端启动成功，端口 5174 可访问
- [ ] 演示账号可正常登录，功能可正常使用
