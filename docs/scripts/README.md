# 青启e城 · 部署脚本集

> 从 GitHub 克隆项目后，使用本目录下的脚本可快速完成环境检查、数据库初始化、项目启动等操作。
> 支持 Windows（`.bat`）和 Linux（`.sh`）双平台。

---

## 📁 目录结构

```
scripts/
├── README.md                    # 本文档
├── windows/                     # Windows 脚本（.bat）
│   ├── config.bat               # ⚙️ 统一配置文件（数据库/Redis/端口等）
│   ├── 00-check-env.bat         # 🔍 环境检查（Java/Maven/Node/MySQL/Redis/Git）
│   ├── 01-init-db.bat           # 🗄️  数据库一键初始化（建库+建表+演示数据）
│   ├── 02-verify-db.bat         # ✅ 数据库验证（表数量/数据/字符集）
│   ├── 03-verify-redis.bat      # ✅ Redis 验证（连接/读写/内存/持久化）
│   ├── 04-init-project.bat      # 📦 项目初始化（后端编译+前端依赖安装）
│   ├── 05-start-backend.bat     # 🚀 启动后端（Spring Boot）
│   ├── 06-start-user-web.bat    # 🚀 启动用户前端（Vue3 + Vite）
│   ├── 07-start-admin-web.bat   # 🚀 启动管理前端（Vue3 + Vite）
│   └── 99-start-all.bat         # 🎯 一键全部启动（后端+两个前端）
└── linux/                       # Linux 脚本（.sh）
    ├── config.sh                # ⚙️ 统一配置文件
    ├── 00-check-env.sh          # 🔍 环境检查
    ├── 01-init-db.sh            # 🗄️  数据库一键初始化
    ├── 02-verify-db.sh          # ✅ 数据库验证
    ├── 03-verify-redis.sh       # ✅ Redis 验证
    ├── 04-init-project.sh       # 📦 项目初始化
    ├── 05-start-backend.sh      # 🚀 启动后端
    ├── 06-start-user-web.sh     # 🚀 启动用户前端
    ├── 07-start-admin-web.sh    # 🚀 启动管理前端
    └── 99-start-all.sh          # 🎯 一键全部启动
```

---

## 🚀 快速上手（从零到启动）

### 第 1 步：克隆代码

```bash
git clone <你的仓库地址>
cd qingqi-ecity
```

### 第 2 步：修改配置（可选，默认即可）

根据你的系统，编辑对应的配置文件：

- **Windows**：`scripts/windows/config.bat`
- **Linux**：`scripts/linux/config.sh`

默认配置（本地开发用）：
- MySQL：`127.0.0.1:3306` / `root` / `123456` / 数据库名 `qingqi`
- Redis：`127.0.0.1:6379` / 无密码
- 后端端口：`8080`
- 用户前端端口：`5173`
- 管理前端端口：`5174`

### 第 3 步：检查环境

```bash
# Windows
scripts\windows\00-check-env.bat

# Linux
chmod +x scripts/linux/*.sh
scripts/linux/00-check-env.sh
```

确保至少 Java、Node.js、MySQL 通过检查（Redis MVP 阶段可选）。

### 第 4 步：初始化数据库

确保 MySQL 服务已启动，然后：

```bash
# Windows
scripts\windows\01-init-db.bat

# Linux
scripts/linux/01-init-db.sh
```

完成后会显示 25 张表和 5 个演示账号。

### 第 5 步：初始化项目（编译+安装依赖）

```bash
# Windows
scripts\windows\04-init-project.bat

# Linux
scripts/linux/04-init-project.sh
```

### 第 6 步：启动全部服务

```bash
# Windows
scripts\windows\99-start-all.bat

# Linux
scripts/linux/99-start-all.sh
```

或者分别启动：

```bash
# 启动后端
scripts\windows\05-start-backend.bat     # Windows
scripts/linux/05-start-backend.sh        # Linux

# 启动用户前端
scripts\windows\06-start-user-web.bat    # Windows
scripts/linux/06-start-user-web.sh       # Linux

# 启动管理前端
scripts\windows\07-start-admin-web.bat   # Windows
scripts/linux/07-start-admin-web.sh      # Linux
```

---

## 🔑 演示账号

所有演示账号统一密码：**`123456`**

| 账号 | 角色 | 说明 |
|------|------|------|
| `admin` | 系统管理员 | 管理后台全部权限 |
| `testuser` | 青年用户（在校生） | 体验全部用户功能 |
| `entrepreneur` | 青年创业者 | 体验青创e贷、经营赋能 |
| `landlord01` | 房东 | 体验保函确认、索赔 |
| `banker01` | 银行运营岗 | 体验人工审核 |

---

## 📖 脚本说明

### 环境检查类

| 脚本 | 功能 | 输出 |
|------|------|------|
| `00-check-env` | 检查 6 项开发依赖 | 通过/失败数量 + 安装建议 |

### 数据库类

| 脚本 | 功能 | 说明 |
|------|------|------|
| `01-init-db` | 一键初始化数据库 | 创建库 → 导入表结构 → 导入演示数据，幂等可重复运行 |
| `02-verify-db` | 数据库完整性验证 | 检查连接/表数量/核心数据/字符集 |

### Redis 类

| 脚本 | 功能 | 说明 |
|------|------|------|
| `03-verify-redis` | Redis 验证 | 连接/读写/内存/持久化/淘汰策略检查 |

> 💡 Redis 为 MVP 可选依赖，不启用也能跑通核心业务。

### 项目初始化类

| 脚本 | 功能 | 说明 |
|------|------|------|
| `04-init-project` | 项目初始化 | Maven 编译后端 + npm install 两个前端 |

### 服务启动类

| 脚本 | 功能 | 说明 |
|------|------|------|
| `05-start-backend` | 启动后端 | 自动检测 jar 包，不存在则先编译 |
| `06-start-user-web` | 启动用户前端 | 自动检测 node_modules，不存在则先 install |
| `07-start-admin-web` | 启动管理前端 | 同上 |
| `99-start-all` | 一键全部启动 | 依次启动后端 + 用户前端 + 管理前端 |

---

## 🌐 访问地址

启动后访问以下地址：

| 服务 | 地址 | 默认账号 |
|------|------|----------|
| 后端 API | http://localhost:8080 | — |
| 用户前端 | http://localhost:5173 | testuser / 123456 |
| 管理前端 | http://localhost:5174 | admin / 123456 |

---

## 📚 相关文档

更详细的部署说明见：

- [MySQL 数据库部署文档](../docs/deployment/mysql-deployment.md)
- [Redis 部署文档](../docs/deployment/redis-deployment.md)
- [Docker Compose 部署指南](../docs/deployment/docker-compose.md)

---

## ❓ 常见问题

### Q: 运行 .bat 脚本闪退？
A: 请在 cmd 窗口中运行脚本（而非直接双击），这样可以看到错误信息。
或者右键 → 在此处打开 PowerShell → 输入脚本名运行。

### Q: Linux 脚本报 Permission denied？
A: 加执行权限：
```bash
chmod +x scripts/linux/*.sh
```

### Q: MySQL 连接失败？
A: 依次检查：
1. MySQL 服务是否启动
2. `config.bat` / `config.sh` 中的地址端口账号密码是否正确
3. 防火墙是否放行 3306
4. 远程连接时 bind-address 是否为 0.0.0.0

### Q: npm install 很慢？
A: 切换国内镜像：
```bash
npm config set registry https://registry.npmmirror.com
```

### Q: Maven 下载依赖很慢？
A: 配置阿里云 Maven 镜像，在 `settings.xml` 的 `<mirrors>` 中添加：
```xml
<mirror>
  <id>aliyunmaven</id>
  <mirrorOf>*</mirrorOf>
  <name>阿里云公共仓库</name>
  <url>https://maven.aliyun.com/repository/public</url>
</mirror>
```

### Q: Redis 必须启动吗？
A: MVP 阶段 Redis 为可选依赖，不启动也能跑通核心业务流程。
项目配置中 Redis 连接失败不会导致应用启动失败。
