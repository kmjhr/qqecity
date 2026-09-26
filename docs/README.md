# 青启e城 · 文档中心

> 本文档目录与代码目录一一对应，便于开发查阅。所有业务需求以 `common/requirements.md` 为基准，不做内容修改，仅做结构重组。
>
> 🚀 **新手上路**：克隆项目后想快速跑起来？直接看 [部署与配置指南](./deployment/README.md)

---

## 📂 文档导航

### 后端开发（backend/）

| 文档 | 说明 |
| --- | --- |
| [architecture.md](backend/architecture.md) | 后端架构与分层说明 |
| [api-spec.md](backend/api-spec.md) | 接口规范（统一返回 / 错误码 / 鉴权） |
| [user-module.md](backend/user-module.md) | 用户模块需求说明（注册 / 登录 / 授权） |
| [guarantee-module.md](backend/guarantee-module.md) | 安居保函模块需求说明 |
| [loan-module.md](backend/loan-module.md) | 青创e贷模块需求说明 |
| [bookkeeping-module.md](backend/bookkeeping-module.md) | 经营赋能模块需求说明（占位） |
| [budget-module.md](backend/budget-module.md) | 预算消费模块需求说明 |
| [safety-module.md](backend/safety-module.md) | 金融安全模块需求说明 |
| [message-module.md](backend/message-module.md) | 消息中心模块需求说明 |

### 用户前端（user-web/）

| 文档 | 说明 |
| --- | --- |
| [page-structure.md](user-web/page-structure.md) | 页面结构、路由与菜单说明 |

### 管理前端（admin-web/）

| 文档 | 说明 |
| --- | --- |
| [page-structure.md](admin-web/page-structure.md) | 管理端页面结构与权限说明 |

### 公共文档（common/）

| 文档 | 说明 |
| --- | --- |
| [requirements.md](common/requirements.md) | **⭐ 需求清单（业务需求唯一基准）** |
| [tech-stack.md](common/tech-stack.md) | 技术选型明细 |
| [dev-roadmap.md](common/dev-roadmap.md) | MVP 开发排期与路线图 |
| [ai-dev-plan.md](common/ai-dev-plan.md) | AI 协助开发完整计划书 |

### 部署与配置（deployment/）

> 详细使用指南见 [部署文档总索引](./deployment/README.md)

| 文档 | 说明 |
| --- | --- |
| **本地配置指南系列** | |
| [README.md](deployment/README.md) | 📑 部署文档总索引（5 步上手流程图） |
| [01-env-preparation.md](deployment/01-env-preparation.md) | 🔧 环境准备（JDK / Node.js / MySQL / Redis / Git） |
| [02-project-setup.md](deployment/02-project-setup.md) | 🚀 项目克隆与初始化全流程（7 步操作） |
| [03-backend-config.md](deployment/03-backend-config.md) | ⚙️ 后端配置指南（application.yml 详解） |
| [04-frontend-config.md](deployment/04-frontend-config.md) | 🎨 前端配置指南（环境变量 / Vite 代理） |
| [05-troubleshooting.md](deployment/05-troubleshooting.md) | 🔍 常见问题排查（6 大类 25+ 问题） |
| **部署专题文档** | |
| [database-services.md](deployment/database-services.md) | 🗄️ 数据库服务安装参考（MySQL + Redis 双平台） |
| [docker-compose.md](deployment/docker-compose.md) | 🐳 Docker Compose 一键部署 |
| [database-design.md](deployment/database-design.md) | 📐 数据库设计与 ER 关系图（25 张表） |

### 适配报告

| 文档 | 说明 |
| --- | --- |
| [适配报告.md](./适配报告.md) | 代码骨架 ↔ 原始需求文档适配报告（7 章完整记录） |

---

## 🔧 配套脚本

`scripts/` 目录提供一键脚本，Windows / Linux 双平台支持：

| 脚本目录 | 说明 |
| --- | --- |
| [scripts/windows/](./scripts/windows/) | Windows 批处理脚本（环境检查 / 数据库初始化 / 服务启动等） |
| [scripts/linux/](./scripts/linux/) | Linux Shell 脚本（功能同上） |

详细使用说明见 [脚本集 README](./scripts/README.md)。

---

## 🔗 代码模块 → 文档对应关系

| 代码模块 | 对应文档 | 覆盖需求 |
| --- | --- | --- |
| `backend/module/user/` | [user-module.md](backend/user-module.md) | P-1 注册 / P-2 登录 / P-3 用户信息 |
| `backend/module/message/` | [message-module.md](backend/message-module.md) | P-4 消息中心 |
| `backend/module/guarantee/` | [guarantee-module.md](backend/guarantee-module.md) | G-1 ~ G-7 安居保函全流程 |
| `backend/module/loan/` | [loan-module.md](backend/loan-module.md) | L-1 ~ L-6 青创e贷 |
| `backend/module/bookkeeping/` | [bookkeeping-module.md](backend/bookkeeping-module.md) | B-1 ~ B-4 经营赋能 |
| `backend/module/budget/` | [budget-module.md](backend/budget-module.md) | C-1 ~ C-7 预算消费 |
| `backend/module/safety/` | [safety-module.md](backend/safety-module.md) | S-1 ~ S-5 金融安全 |
| `user-web/src/views/` | [user-web/page-structure.md](user-web/page-structure.md) | 用户端全部页面 |
| `admin-web/src/views/` | [admin-web/page-structure.md](admin-web/page-structure.md) | 管理端全部页面 |

---

## 📌 文档重构说明

> 本文档目录由原始编号扁平结构（01-技术选型 / 02-目录结构 / 03-排期 / 05-AI计划 / 06-需求清单）重构为按代码模块组织的分层结构。
>
> **重构原则**：
> 1. 原始设计文档中的业务功能、业务规则、用户角色、核心流程一律不修改
> 2. 文档仅调整目录层级、文件命名、章节结构、排版格式
> 3. docs 目录与代码目录一一对应
>
> 完整适配记录见 [《适配报告》](./适配报告.md)。
