# 青启e城（qingqi-ecity）演示系统

城市新场景下青年就业创业与碎片消费一体化金融智能服务平台 —— 公用演示系统（课程设计/参赛演示版）。

本项目依据《青启e城项目系统设计说明书》开发，是设计文档的可运行骨架：以 Web 系统演示三大场景（入城安居、城市轻创业、碎片化消费）与五大功能模块（安居金融风控、轻创业智能授信、创业经营赋能、碎片消费治理、青年金融安全），银行侧能力（放款、保函开立、征信查询等）以模拟桩实现，**不承诺任何真实金融功能**。

## 技术栈

| 层 | 选型 |
| --- | --- |
| 后端 | Java 17 · Spring Boot 3.2 · MyBatis-Plus · MySQL 8 · JWT |
| 前端 | Vue 3 · Vite 5 · Element Plus · Pinia · Vue Router · Axios |
| 部署 | Docker Compose（MySQL / 后端 / 前端 Nginx） |

详见 `docs/01-技术选型明细.md`。

## 快速启动

### 方式一：Docker Compose（推荐）

```bash
docker compose up -d --build
# 前端 http://localhost:8088  ，后端 http://localhost:8080/api
```

### 方式二：本地开发

1. 后端：准备 JDK 17 与 MySQL 8，创建数据库 `qingqi` 并执行 `backend/src/main/resources/db/schema.sql` 与 `data.sql`，用 IDEA 打开 `backend/` 运行 `QingqiApplication`（默认端口 8080）。
2. 前端：`cd frontend && npm install && npm run dev`（默认 5173，已代理 `/api` 到 8080）。

演示账号（见 `db/data.sql`）：`13800000000 / 123456`

## 目录结构

```
qingqi-ecity/
├── docs/        # 技术选型、目录结构、MVP 排期、开发路线图
├── backend/     # Spring Boot 后端
└── frontend/    # Vue 3 前端
```

## 与设计文档的关系

- 数据库 25 张表：见 `backend/src/main/resources/db/schema.sql`（与设计说明书第 6 章一致）
- 接口规范：统一返回 `{code, message, data}`，错误码见设计说明书 8.2
- Sprint 排期：见 `docs/03-MVP开发排期.md` 与 `docs/04-开发路线图.html`

## 安全与合规说明

- 演示系统仅用于课程设计/参赛演示，密码使用 SHA-256 散列存储（骨架阶段简化，正式化需引入加盐与加密算法）；
- 不实现真实征信查询、受托支付放款、保函开立等银行能力，相关接口均为模拟逻辑；
- 接入真实银行系统与对外发布前，须完成金融合规、个人信息保护与安全评审。
