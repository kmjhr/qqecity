# 青启e城

> 服务青年成长的综合金融服务平台 —— 安居保函 + 青创e贷 + 经营赋能 + 预算消费 + 金融安全，五大模块一站式搞定。
>
> 单后端 + 用户前端 + 管理前端，后端接口天然兼容微信小程序。

---

## ✨ 项目特性

- 🎯 **五大业务模块**：安居保函、青创e贷、经营赋能、预算消费、金融安全
- 👥 **多角色体系**：青年用户（在校生/毕业生/创业者）、房东、银行运营岗、系统管理员
- 📱 **三端复用**：用户网页 + 管理后台 + 微信小程序（共用后端 API）
- 🔐 **JWT 鉴权**：Token 双令牌 + Redis 黑名单 + 角色权限控制
- 🗄️ **25 张数据表**：完整覆盖业务全链路，ER 关系清晰
- 🐳 **Docker 一键启动**：MySQL + Redis + 后端 + 双前端全部容器化
- 🧪 **内置演示数据**：5 个演示账号 + 各模块样例数据，开箱即用
- 📖 **完整文档体系**：需求文档 + 模块设计 + 部署指南 + 问题排查

---

## 🛠️ 技术栈

### 后端

| 技术 | 版本 | 说明 |
| --- | --- | --- |
| Spring Boot | 3.x | 核心框架 |
| MyBatis-Plus | 3.5.x | ORM + 分页 + 逻辑删除 |
| JWT | — | 无状态鉴权（双令牌 + 黑名单） |
| MySQL | 8.0 | 关系型数据库 |
| Redis | 7.x | 缓存 + Token 黑名单 + 验证码 |
| BCrypt | — | 密码加密 |

### 前端

| 技术 | 用户端（user-web） | 管理端（admin-web） |
| --- | --- | --- |
| 框架 | Vue 3 | Vue 3 |
| 语言 | JavaScript | TypeScript |
| UI 库 | Element Plus | Element Plus |
| 构建工具 | Vite | Vite |
| 路由 | Vue Router 4 | Vue Router 4 |
| 状态管理 | Pinia | Pinia |
| HTTP 客户端 | Axios | Axios |

---

## 📂 项目结构

```
qingqi-ecity/
├── backend/                        # 后端（Spring Boot 3）
│   ├── pom.xml
│   ├── Dockerfile
│   ├── sql/                        # SQL 脚本
│   │   ├── schema.sql              # 建表 SQL（25 张表）
│   │   └── data.sql                # 演示数据（幂等可重复导入）
│   └── src/main/
│       ├── java/com/icbc/qingqi/
│       │   ├── QingqiApplication.java      # 启动类
│       │   ├── common/                     # 公共层（统一响应/错误码/异常处理）
│       │   ├── config/                     # 配置层（跨域/MyBatis/Redis/安全）
│       │   ├── security/                   # 安全层（JWT工具/过滤器/用户上下文）
│       │   └── module/                     # 业务模块（按领域划分）
│       │       ├── user/                   # 用户模块（注册/登录/个人信息）
│       │       ├── message/                # 消息中心
│       │       ├── guarantee/              # 安居保函
│       │       ├── loan/                   # 青创e贷
│       │       ├── bookkeeping/            # 经营赋能
│       │       ├── budget/                 # 预算消费
│       │       └── safety/                 # 金融安全
│       └── resources/
│           ├── application.yml             # 应用配置
│           └── db/                         # 资源目录 SQL 副本
│
├── user-web/                       # 用户前端（Vue 3 + JS）
│   ├── package.json
│   ├── vite.config.js
│   ├── index.html
│   ├── Dockerfile
│   ├── nginx.conf
│   └── src/
│       ├── api/                    # 接口层（按模块划分）
│       ├── router/                 # 路由 + 登录守卫
│       ├── store/                  # Pinia 状态管理
│       ├── layout/                 # 布局组件（顶部导航）
│       └── views/                  # 页面（首页/登录/注册/5大模块/消息/个人中心）
│
├── admin-web/                      # 管理前端（Vue 3 + TS）
│   ├── package.json
│   ├── vite.config.ts
│   ├── tsconfig.json
│   ├── index.html
│   ├── Dockerfile
│   ├── nginx.conf
│   └── src/
│       ├── api/                    # 接口层
│       ├── router/                 # 路由 + 权限守卫
│       ├── store/                  # Pinia 状态管理
│       ├── layout/                 # 布局组件（侧边栏 + 顶栏）
│       └── views/                  # 页面（登录/看板/用户管理等）
│
├── docs/                           # 文档中心
│   ├── README.md                   # 文档导航总索引
│   ├── backend/                    # 后端开发文档（8个模块）
│   ├── user-web/                   # 用户前端文档
│   ├── admin-web/                  # 管理前端文档
│   ├── common/                     # 公共文档（需求/技术选型/排期/AI计划）
│   └── deployment/                 # 部署配置指南（10 篇）
│
├── scripts/                        # 部署脚本（Windows + Linux 双平台）
│   ├── README.md                   # 脚本使用说明
│   ├── windows/                    # Windows 批处理脚本（10个）
│   └── linux/                      # Linux Shell 脚本（10个）
│
├── prototype/                      # HTML 静态原型（可点击）
│
├── docker-compose.yml              # Docker Compose 一键编排
└── .gitignore
```

---

## 🚀 快速启动

### ⚡ 方式一：脚本一键启动（推荐，新手首选）

**Windows：**
```cmd
cd scripts\windows
00-check-env.bat      # 1. 检查环境
01-init-db.bat        # 2. 初始化数据库（建库+建表+演示数据）
04-init-project.bat   # 3. 项目初始化（编译后端+安装前端依赖）
99-start-all.bat      # 4. 一键启动全部服务
```

**Linux / macOS：**
```bash
cd scripts/linux
chmod +x *.sh
./00-check-env.sh     # 1. 检查环境
./01-init-db.sh       # 2. 初始化数据库
./04-init-project.sh  # 3. 项目初始化
./99-start-all.sh     # 4. 一键启动全部服务
```

> 详细说明见 [脚本集 README](scripts/README.md)

---

### 🐳 方式二：Docker Compose 一键启动（最快）

**前置条件**：已安装 Docker 和 Docker Compose

```bash
# 一键启动全部服务（MySQL + Redis + 后端 + 用户前端 + 管理前端）
docker compose up -d --build
```

启动后访问：
- 用户前端：http://localhost:8081
- 管理前端：http://localhost:8082
- 后端 API：http://localhost:8080/api（接口文档：http://localhost:8080/api/doc.html）
- MySQL：localhost:3306（root / 123456）
- Redis：localhost:6379（无密码）

> 详细说明见 [Docker 部署文档](docs/deployment/docker-compose.md)

---

### 🔧 方式三：手动启动（开发调试用）

#### 前置依赖

| 依赖 | 版本要求 | 是否必须 |
| --- | --- | --- |
| JDK | 17+ | ✅ 必须 |
| Node.js | 18+ | ✅ 必须 |
| MySQL | 8.0 | ✅ 必须 |
| Redis | 6.0+ | ✅ 推荐安装（JWT 黑名单依赖；未安装时后端自动降级，不检查黑名单，仅限演示） |
| Maven Wrapper | 3.9.x（内置） | ✅ 项目自带，无需安装 |
| Git | 最新 | ✅ 必须 |

> 环境安装详细步骤见 [环境准备指南](docs/deployment/01-env-preparation.md)

#### 第 1 步：克隆代码

```bash
git clone <你的仓库地址>
cd qingqi-ecity
```

#### 第 2 步：启动数据库

确保 MySQL 服务已启动，然后初始化数据库：

```bash
# 创建数据库 + 导入表结构 + 导入演示数据
mysql -uroot -p123456 -e "CREATE DATABASE IF NOT EXISTS qingqi DEFAULT CHARSET utf8mb4;"
mysql -uroot -p123456 qingqi < backend/sql/schema.sql
mysql -uroot -p123456 qingqi < backend/sql/data.sql
```

> 完整数据库部署说明见 [MySQL 部署文档](docs/deployment/mysql-deployment.md) + [Redis 部署文档](docs/deployment/redis-deployment.md)

#### 第 3 步：启动后端

```bash
cd backend

# 方式 A：Maven Wrapper（推荐，无需安装 Maven）
# Windows
mvnw.cmd spring-boot:run
# Linux / macOS
./mvnw spring-boot:run

# 方式 B：IDE 运行
# 用 IDEA 打开项目，运行 QingqiApplication.java
```

后端默认端口：**8080**

> 配置修改见 [后端配置指南](docs/deployment/03-backend-config.md)

#### 第 4 步：启动用户前端

```bash
cd user-web
npm install
npm run dev
```

用户前端地址：http://localhost:5173

#### 第 5 步：启动管理前端

```bash
cd admin-web
npm install
npm run dev
```

管理前端地址：http://localhost:5174

> 前端配置修改见 [前端配置指南](docs/deployment/04-frontend-config.md)

---

## 👤 演示账号

所有演示账号统一密码：**`123456`**

| 账号 | 角色 | 说明 | 适用端 |
| --- | --- | --- | --- |
| `admin` | 系统管理员 | 管理后台全部权限 | 管理前端 |
| `testuser` | 青年用户（在校生） | 体验全部用户功能 | 用户前端 |
| `entrepreneur` | 青年创业者 | 体验青创e贷、经营赋能 | 用户前端 |
| `landlord01` | 房东 | 体验保函确认、索赔 | 用户前端 |
| `banker01` | 银行运营岗 | 体验人工审核 | 管理前端 |

---

## 🌐 访问地址

### 本地开发模式

| 服务 | 地址 | 说明 |
| --- | --- | --- |
| 后端 API | http://localhost:8080/api | 所有接口统一 /api 前缀 |
| 用户前端 | http://localhost:5173 | 普通用户访问 |
| 管理前端 | http://localhost:5174 | 管理员访问 |

### Docker 模式

| 服务 | 地址 |
| --- | --- |
| 后端 API | http://localhost:8082/api |
| 用户前端 | http://localhost:8081 |
| 管理前端 | http://localhost:8083 |

---

## 📚 文档导航

完整文档见 [docs/](docs/) 目录，推荐按以下顺序阅读：

### 新手上路

| 文档 | 说明 |
| --- | --- |
| [文档中心总索引](docs/README.md) | 所有文档的导航入口 |
| [环境准备指南](docs/deployment/01-env-preparation.md) | JDK / Node / MySQL / Redis 安装 |
| [项目初始化全流程](docs/deployment/02-project-setup.md) | 从克隆到启动的 7 步操作 |
| [常见问题排查](docs/deployment/05-troubleshooting.md) | 6 大类 25+ 问题及解法 |

### 开发参考

| 文档 | 说明 |
| --- | --- |
| [后端架构说明](docs/backend/architecture.md) | 分层架构、模块划分、代码规范 |
| [接口规范](docs/backend/api-spec.md) | 统一返回格式、错误码、鉴权规则 |
| [数据库设计与 ER 图](docs/deployment/database-design.md) | 25 张表的字段、关系、ER 说明 |
| [用户前端页面结构](docs/user-web/page-structure.md) | 路由、菜单、页面布局 |
| [管理前端页面结构](docs/admin-web/page-structure.md) | 管理端页面与权限说明 |

### 业务需求

| 文档 | 说明 |
| --- | --- |
| [⭐ 需求清单](docs/common/requirements.md) | **业务需求唯一基准** |
| [技术选型](docs/common/tech-stack.md) | 技术栈选型说明 |
| [MVP 开发排期](docs/common/dev-roadmap.md) | 开发路线图与里程碑 |
| [AI 开发计划](docs/common/ai-dev-plan.md) | AI 协助开发计划书 |

### 部署运维

| 文档 | 说明 |
| --- | --- |
| [MySQL 部署文档](docs/deployment/mysql-deployment.md) | MySQL 8.0 双平台安装、建库导入、远程访问 |
| [Redis 部署文档](docs/deployment/redis-deployment.md) | Redis 7.x 双平台安装、安全加固 |
| [后端配置指南](docs/deployment/03-backend-config.md) | application.yml 完整详解 |
| [前端配置指南](docs/deployment/04-frontend-config.md) | 端口 / Vite 代理配置 |
| [Docker 部署文档](docs/deployment/docker-compose.md) | Docker Compose 一键部署 |

---

## 🧪 验证清单

启动完成后，可按以下清单验证：

- [ ] 后端启动无报错，端口 8080 正常监听
- [ ] 访问 `http://localhost:8080/api/...` 接口返回正常 JSON
- [ ] MySQL 中有 25 张表，数据库字符集为 utf8mb4
- [ ] 用户前端可以正常打开（http://localhost:5173）
- [ ] 用户端登录成功（testuser / 123456）
- [ ] 管理前端可以正常打开（http://localhost:5174）
- [ ] 管理端登录成功（admin / 123456）
- [ ] 消息中心页面可以正常加载消息列表
- [ ] Redis 连接正常，Token 黑名单机制可用（未安装 Redis 时黑名单降级，不影响登录）

---

## ❓ 遇到问题？

1. 先查 [常见问题排查](docs/deployment/05-troubleshooting.md) —— 6 大类 25+ 常见问题及解法
2. 再查对应部署文档（数据库服务 / 后端 / 前端 / Docker）
3. 查看后端日志和前端控制台报错信息
4. 参考 [适配报告](docs/适配报告.md) 了解项目架构演变历史

---

## 📌 说明

- 本项目基于 [youlai-boot](https://github.com/youlaitech/youlai-boot) 和 [vue3-element-admin](https://github.com/un-pany/vue3-element-admin) 精简改造
- 数据库设计、业务规则、角色体系以原始需求文档为基准，详见 [需求清单](docs/common/requirements.md)
- 代码骨架已完成 5 大业务模块的目录搭建和 Controller 占位，具体业务逻辑待后续迭代开发
