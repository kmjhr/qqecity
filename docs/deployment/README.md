# 青启e城 · 本地部署与配置指南

> 从 GitHub 克隆到本地运行的完整指南，覆盖环境准备、项目初始化、配置详解与问题排查全流程。

---

## 快速导航

### 入门必读（按顺序阅读）

| 文档 | 说明 | 适用阶段 |
|------|------|----------|
| [01-env-preparation.md](./01-env-preparation.md) | 环境准备：JDK / Node.js / MySQL / Redis / Git 安装与验证 | 环境准备阶段 |
| [02-project-setup.md](./02-project-setup.md) | 项目初始化：从 git clone 到服务启动的 7 步操作 | 项目初始化阶段 |
| [03-backend-config.md](./03-backend-config.md) | 后端配置：application.yml 完整配置说明 | 后端配置阶段 |
| [04-frontend-config.md](./04-frontend-config.md) | 前端配置：Vite 代理、双前端项目配置说明 | 前端配置阶段 |
| [05-troubleshooting.md](./05-troubleshooting.md) | 问题排查：环境 / 数据库 / Redis / 前后端 / 功能类 | 问题排查阶段 |
| [06-ai-llm.md](./06-ai-llm.md) | AI/LLM 配置：Ollama / 云端 API / "AI 降级本地"排障 | AI 功能配置与排障 |
| [07-anti-fraud-source.md](./07-anti-fraud-source.md) | 反诈预警实时源：mock 模拟实时 / real 真实爬取与回退 | 金融安全页数据源配置 |

### 专项部署文档

| 文档 | 说明 |
|------|------|
| [mysql-deployment.md](./mysql-deployment.md) | MySQL 8.0 部署（Windows / Linux 双平台 + 远程访问 + 数据导入） |
| [redis-deployment.md](./redis-deployment.md) | Redis 7.x 部署（Windows / Linux 双平台 + 远程访问 + 安全加固） |
| [docker-compose.md](./docker-compose.md) | Docker Compose 一键部署（MySQL + Redis + 后端 + 双前端） |
| [database-design.md](./database-design.md) | 数据库设计与 ER 图（43 张表结构说明） |

---

## 5 步快速上手

```
克隆代码 → 环境准备 → 数据库初始化 → 项目初始化 → 启动服务
   ↓          ↓            ↓              ↓            ↓
git clone  JDK/Node/     建库+导入       编译+装依赖    后端8080
         MySQL/Redis    schema+data     Maven/npm     前端5173/5174
```

**第 1 步：克隆代码**

```bash
git clone <仓库地址> qingqi-ecity
cd qingqi-ecity
```

**第 2 步：环境准备**

安装 JDK 17+、Node.js 18+、MySQL 8.0（必须）、Redis（推荐）。详见 [01-env-preparation.md](./01-env-preparation.md)。

**第 3 步：数据库初始化**

创建数据库 `qingqi`，导入表结构和演示数据。最快方式用脚本：

```bash
# Windows
scripts\windows\01-init-db.bat

# Linux
scripts/linux/01-init-db.sh
```

手动导入方式见 [MySQL 部署文档 · 第三章](./mysql-deployment.md#三创建数据库与导入数据)。

**第 4 步：项目初始化**

后端编译 + 前端依赖安装：

```bash
# Windows
scripts\windows\04-init-project.bat

# Linux
scripts/linux/04-init-project.sh
```

**第 5 步：启动服务**

```bash
# Windows — 一键全部启动
scripts\windows\99-start-all.bat

# Linux — 一键全部启动
scripts/linux/99-start-all.sh
```

或手动分别启动：
- 后端：在 `backend/` 目录运行 `./mvnw spring-boot:run`（Linux/macOS）或 `mvnw.cmd spring-boot:run`（Windows），端口 8080
- 用户前端：在 `user-web/` 目录运行 `npm run dev`（端口 5173）
- 管理前端：在 `admin-web/` 目录运行 `npm run dev`（端口 5174）

---

## 演示账号速查表

> 所有演示账号密码统一为：**`123456`**

| 账号 | 角色 | 说明 | 登录端 |
|------|------|------|--------|
| `admin` | 系统管理员 | 管理后台全部权限 | 管理端 |
| `testuser` | 青年用户（在校生） | 体验全部用户功能 | 用户端 |
| `entrepreneur` | 青年创业者 | 体验青创e贷、经营赋能 | 用户端 |
| `landlord01` | 房东 | 体验保函确认、索赔 | 用户端 |
| `banker01` | 银行运营岗 | 体验人工审核 | 管理端 |

---

## 访问地址速查表

### 本地开发模式

| 服务 | 地址 | 说明 |
|------|------|------|
| 后端 API | `http://localhost:8080/api` | Spring Boot 服务，接口前缀 `/api` |
| 接口文档 | `http://localhost:8080/api/doc.html` | Knife4j / Swagger 接口文档 |
| 用户前端 | `http://localhost:5173` | 青启e城用户端（Vue 3 + Vite） |
| 管理前端 | `http://localhost:5174` | 青启e城管理端（Vue 3 + Vite） |
| MySQL | `127.0.0.1:3306` | 数据库名：`qingqi`，账号：`root / 123456` |
| Redis | `127.0.0.1:6379` | 默认无密码（本地开发用） |

### Docker 模式

| 服务 | 地址 |
|------|------|
| 后端 API | `http://localhost:8080/api` |
| 用户前端 | `http://localhost:8081` |
| 管理前端 | `http://localhost:8082` |
| MySQL | `127.0.0.1:3307`（宿主机映射，容器内 3306） |
| Redis | `127.0.0.1:6379` |

---

## 相关资源

- **脚本目录**：[`scripts/`](../../scripts/) — Windows / Linux 一键脚本
  - `scripts/windows/` — Windows 平台（.bat）
  - `scripts/linux/` — Linux 平台（.sh）
  - [`scripts/README.md`](../../scripts/README.md) — 脚本使用完整说明
- **技术选型文档**：[tech-stack.md](../common/tech-stack.md)
- **需求清单**：[requirements.md](../common/requirements.md)
- **数据库设计**：[database-design.md](./database-design.md)
- **Docker 部署**：[docker-compose.md](./docker-compose.md)

---

## 部署检查清单

本地部署完成后，按以下清单逐项确认：

- [ ] JDK 17+ 已安装，`java -version` 正常
- [ ] Node.js 18+ 已安装，`node -v` 正常
- [ ] MySQL 8.0 已启动，数据库 `qingqi` 已创建
- [ ] schema.sql 已导入，表数量 = 43
- [ ] data.sql 已导入，5 个演示账号可登录
- [ ] Redis 已启动（推荐安装；未安装时后端自动降级，仅黑名单功能失效）
- [ ] 后端 application.yml 数据库 / Redis 配置正确
- [ ] 后端启动成功，端口 8080 无报错
- [ ] `npm install` 无报错，两个前端依赖安装完成
- [ ] 用户前端启动成功，端口 5173 可访问
- [ ] 管理前端启动成功，端口 5174 可访问
- [ ] 演示账号可正常登录
