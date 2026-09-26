# 青启e城 · 项目克隆与初始化全流程

> 本文档属于「本地部署与配置指南」系列，完整目录见 [README.md](./README.md)

> 适用场景：从 GitHub 克隆代码后，一步步完成项目配置、数据库初始化、前后端启动，最终本地跑起来。
> 预计耗时：30 ~ 60 分钟（取决于网络下载速度）

---

## 目录

- [第 1 步：克隆代码](#第-1-步克隆代码)
- [第 2 步：配置文件说明](#第-2-步配置文件说明)
- [第 3 步：数据库初始化](#第-3-步数据库初始化)
- [第 4 步：Redis 配置（推荐安装）](#第-4-步redis-配置推荐安装)
- [第 5 步：后端编译启动](#第-5-步后端编译启动)
- [第 6 步：前端启动](#第-6-步前端启动)
- [第 7 步：验证全链路](#第-7-步验证全链路)
- [Docker 方式快速启动](#docker-方式快速启动)
- [脚本方式快速启动](#脚本方式快速启动)

---

## 第 1 步：克隆代码

### 1.1 克隆仓库

```bash
# 进入工作目录
cd <你的工作目录>

# 克隆代码（替换为实际仓库地址）
git clone <仓库地址> qingqi-ecity

# 进入项目目录
cd qingqi-ecity
```

### 1.2 目录说明

```
qingqi-ecity/
├── backend/           # Spring Boot 后端项目
├── user-web/         # 用户前端（Vue 3 + JS）
├── admin-web/        # 管理前端（Vue 3 + TS）
├── docs/              # 文档目录
│   ├── deployment/    # 部署文档（即本目录）
│   └── scripts/      # 一键启动脚本（Windows / Linux）
├── docker-compose.yml # Docker Compose 编排文件
└── README.md          # 项目说明
```

更多目录结构说明见根目录 [README.md](../../README.md)「项目结构」章节。

---

## 第 2 步：配置文件说明

以下文件需要根据本地环境进行修改，请逐一检查：

### 2.1 后端配置文件

**文件位置：** `backend/src/main/resources/application.yml`

需要修改的核心配置：

| 配置项 | 说明 | 默认值 |
|--------|------|--------|
| `spring.datasource.url` | 数据库连接地址 | `jdbc:mysql://127.0.0.1:3306/qingqi` |
| `spring.datasource.username` | 数据库用户名 | `root` |
| `spring.datasource.password` | 数据库密码 | `123456` |
| `spring.data.redis.host` | Redis 地址 | `127.0.0.1` |
| `spring.data.redis.port` | Redis 端口 | `6379` |
| `spring.data.redis.password` | Redis 密码 | （空） |
| `server.port` | 服务端口 | `8080` |

> 详细配置说明见 [03-backend-config.md](./03-backend-config.md)。

### 2.2 前端配置文件

**用户前端：** `user-web/`（本项目未使用 .env 文件，端口与 API 代理直接配置在 `user-web/vite.config.js`）

```env
VITE_API_BASE_URL=/api
VITE_APP_TITLE=青启e城
VITE_PORT=5173
```

**管理前端：** `admin-web/`（同上，配置在 `admin-web/vite.config.ts`）

```env
VITE_API_BASE_URL=/api
VITE_APP_TITLE=青启e城管理后台
VITE_PORT=5174
```

Vite 代理配置文件：`user-web/vite.config.js`（管理端为 `admin-web/vite.config.ts`）中的 `server.proxy`，将 `/api` 代理到后端 `http://localhost:8080`。

> 详细配置说明见 [04-frontend-config.md](./04-frontend-config.md)。

### 2.3 脚本配置文件

- **Windows：** `docs/scripts/windows/config.bat` — 配置 MySQL 连接等参数
- **Linux：** `docs/scripts/linux/config.sh` — 配置 MySQL 连接等参数

使用脚本启动时需要修改对应配置文件。

---

## 第 3 步：数据库初始化

> 详细步骤见 [database-services.md](./database-services.md#三创建数据库与导入数据)

### 3.1 确保 MySQL 服务已启动

```cmd
# Windows
net start mysql

# 验证
mysql -u root -p123456 -e "SELECT 1;"
```

### 3.2 创建数据库

```sql
CREATE DATABASE IF NOT EXISTS qingqi
  DEFAULT CHARACTER SET utf8mb4
  DEFAULT COLLATE utf8mb4_general_ci;
```

### 3.3 导入表结构和数据

SQL 文件位置：`backend/src/main/resources/db/` 或 `backend/sql/`

```cmd
# 进入 SQL 目录
cd backend/sql

# 导入表结构
mysql -u root -p123456 qingqi < schema.sql

# 导入演示数据
mysql -u root -p123456 qingqi < data.sql
```

### 3.4 验证导入结果

```sql
USE qingqi;

-- 查看表数量（应返回 25）
SELECT COUNT(*) AS table_count FROM information_schema.tables WHERE table_schema = 'qingqi';

-- 查看演示用户（应返回 5 条）
SELECT id, username, role FROM sys_user LIMIT 5;
```

---

## 第 4 步：Redis 配置（推荐安装）

> Redis 用于 JWT 黑名单与缓存，推荐安装；未启动时后端会自动降级（跳过黑名单检查），仅登出后 Token 立即失效功能受影响。
> 详细部署见 [database-services.md](./database-services.md#五redis-部署)

### 4.1 确保 Redis 服务已启动

```cmd
# Windows
redis-cli ping
# 返回 PONG 即为正常
```

### 4.2 修改后端配置

编辑 `backend/src/main/resources/application.yml`：

```yaml
spring:
  data:
    redis:
      host: 127.0.0.1
      port: 6379
      password: ""    # 无密码则留空
      database: 0
```

> 如果不安装 Redis，保持默认配置即可，后端会打印警告并自动跳过黑名单检查（仅影响登出/强制下线功能）。

---

## 第 5 步：后端编译启动

### 5.1 方式一：Maven 命令启动

```cmd
# 进入后端目录
cd backend

# 编译项目（首次执行会下载依赖，需要几分钟）
mvn clean compile

# 启动服务
mvn spring-boot:run
```

### 5.2 方式二：使用 IDE 或系统 Maven 启动

```cmd
# Windows
> 项目未内置 Maven Wrapper，请使用系统 Maven（`mvn spring-boot:run`）或 IDE 启动。

# Linux / Mac
```

### 5.3 方式三：IDEA 启动（推荐开发时使用）

1. 用 IDEA 打开 `backend/` 目录
2. 等待 Maven 依赖下载完成
3. 找到启动类 `QingqiApplication.java`
4. 点击类左侧的绿色三角按钮启动

### 5.4 验证后端启动

启动成功后，控制台输出类似：

```
Started QingqiApplication in xx.xxx seconds
```

打开浏览器访问：

- 接口健康检查：<http://localhost:8080/api/health>
- 接口文档：<http://localhost:8080/api/doc.html>

> 如果端口被占用，见 [05-troubleshooting.md](./05-troubleshooting.md) 排查。

---

## 第 6 步：前端启动

项目包含两个前端：用户端（user-web）和管理端（admin-web），分别启动。

### 6.1 启动用户前端

```cmd
# 进入用户前端目录
cd user-web

# 安装依赖（首次执行需要几分钟）
npm install

# 启动开发服务
npm run dev
```

启动成功后输出：

```
  VITE v5.x.x  ready in xxx ms

  ➜  Local:   http://localhost:5173/
```

### 6.2 启动管理前端

新开一个终端窗口：

```cmd
# 进入管理前端目录
cd admin-web

# 安装依赖
npm install

# 启动开发服务
npm run dev
```

启动成功后输出：

```
  VITE v5.x.x  ready in xxx ms

  ➜  Local:   http://localhost:5174/
```

> 详细前端配置说明见 [04-frontend-config.md](./04-frontend-config.md)。

---

## 第 7 步：验证全链路

### 7.1 登录测试

**用户端：**

1. 打开 <http://localhost:5173>
2. 使用演示账号登录：`testuser` / `123456`
3. 登录成功后跳转到首页，显示用户信息

**管理端：**

1. 打开 <http://localhost:5174>
2. 使用管理员账号登录：`admin` / `123456`
3. 登录成功后进入管理后台

### 7.2 接口测试

**方式一：浏览器访问**

```
http://localhost:8080/api/health
```

返回 `{"code":0,"message":"success","data":"ok"}` 即为正常。

**方式二：接口文档**

打开 <http://localhost:8080/api/doc.html>，在 Knife4j 页面测试登录接口：

- 接口：`POST /api/v1/auth/login`
- 参数：`{ "username": "admin", "password": "123456" }`
- 预期：返回 token

### 7.3 功能验证

登录后可测试以下核心功能：

| 模块 | 功能点 | 验证方式 |
|------|--------|----------|
| 用户模块 | 登录 / 登出 / 获取用户信息 | 登录后查看个人信息 |
| 安居金融 | 租房保函申请 | 提交保函申请，查看申请记录 |
| 青创e贷 | 贷款申请 / 授信 | 提交贷款申请，查看授信额度 |
| 预算消费 | 分类预算 / 消费记录 | 创建预算，记一笔消费 |
| 金融安全 | 反诈甄别 / 骗局库 | 测试链接甄别，查看骗局列表 |

---

## Docker 方式快速启动

> 详细说明见 [docker-compose.md](./docker-compose.md)

如果已安装 Docker 和 Docker Compose，可以一键启动全部服务：

```bash
# 在项目根目录执行
docker compose up -d --build
```

启动后访问：
- 用户前端：<http://localhost:8081>
- 管理前端：<http://localhost:8083>
- 后端 API：<http://localhost:8082/api>

---

## 脚本方式快速启动

> 详细说明见 `docs/scripts/README.md`

项目提供了 Windows 和 Linux 的一键启动脚本，可自动完成数据库初始化、后端编译启动、前端启动等操作。

**Windows：**

```cmd
cd docs\scripts\windows
99-start-all.bat
```

**Linux：**

```bash
cd docs/scripts/linux
chmod +x 99-start-all.sh
./99-start-all.sh
```

---

## 初始化完成检查清单

- [ ] 代码已克隆到本地，目录结构完整
- [ ] MySQL 服务已启动，数据库 `qingqi` 已创建
- [ ] schema.sql 已导入，data.sql 已导入
- [ ] Redis 服务已启动（推荐安装；未安装时后端自动降级）
- [ ] 后端 application.yml 配置正确
- [ ] 后端启动成功，端口 8080 正常监听
- [ ] 接口文档 /api/doc.html 可访问
- [ ] 用户前端 npm install 完成
- [ ] 用户前端启动成功，端口 5173 可访问
- [ ] 管理前端 npm install 完成
- [ ] 管理前端启动成功，端口 5174 可访问
- [ ] 演示账号可正常登录
- [ ] 核心功能模块可正常访问和操作

> 如遇问题，先查阅 [05-troubleshooting.md](./05-troubleshooting.md) 进行排查。
