# 青启e城 · 前端配置指南

> 本文档属于「本地部署与配置指南」系列，完整目录见 [README.md](./README.md)

> 适用场景：配置和修改 Vue 3 前端项目的各项参数，包括环境变量、Vite 代理、API 地址等。
> 项目包含两个前端：用户端（user-web）和管理端（admin-web），配置方式基本一致。

---

## 目录

- [一、前端项目概览](#一前端项目概览)
- [二、环境变量配置](#二环境变量配置)
- [三、Vite 代理配置](#三vite-代理配置)
- [四、用户前端配置说明](#四用户前端配置说明)
- [五、管理前端配置说明](#五管理前端配置说明)
- [六、常见修改场景](#六常见修改场景)
- [七、构建生产版本](#七构建生产版本)

---

## 一、前端项目概览

### 1.1 项目结构

```
qingqi-ecity/
├── user-web/                  # 用户端（面向 C 端用户，Vue 3 + JS）
│   ├── src/
│   │   ├── api/               # 接口请求封装
│   │   ├── router/            # 路由配置
│   │   ├── store/             # 状态管理（Pinia）
│   │   ├── views/             # 页面组件
│   │   ├── layout/            # 布局组件
│   │   └── main.js            # 入口文件
│   ├── vite.config.js         # Vite 配置（端口/API 代理直接在此配置）
│   ├── nginx.conf             # Docker 部署时的 Nginx 配置
│   ├── Dockerfile
│   └── package.json
│
└── admin-web/                 # 管理端（面向运营管理员，Vue 3 + TS）
    ├── src/
    │   ├── api/
    │   ├── router/
    │   ├── store/
    │   ├── views/
    │   ├── layout/
    │   └── main.ts
    ├── vite.config.ts         # Vite 配置（同左，TS 版）
    ├── nginx.conf             # Docker 部署时的 Nginx 配置
    ├── Dockerfile
    └── package.json
```

### 1.2 两个前端一览

| 工程 | 技术栈 | 开发端口 | Docker 端口 | 访问对象 |
| --- | --- | --- | --- | --- |
| user-web | Vue 3 + JavaScript + Vite | 5173 | 8081 | 青年用户 |
| admin-web | Vue 3 + TypeScript + Vite | 5174 | 8082 | 运营/管理员 |

---

## 二、环境变量配置

**本项目前端未使用 `.env` 环境变量文件**，所有运行参数（端口、API 代理地址）直接在各自的 `vite.config.js` / `vite.config.ts` 中硬编码配置，改动即生效（Vite 启动时读取）。

如需按环境区分配置（例如区分开发/测试/生产后端地址），可自行添加环境变量文件（Vite 原生支持）：

```bash
# user-web/.env.development
VITE_API_BASE=/api
```

```bash
# user-web/.env.production
VITE_API_BASE=/api
```

注意：
- 只有以 `VITE_` 前缀的变量才会暴露给前端代码（通过 `import.meta.env.VITE_XXX` 读取）
- 本项目代码未引用 `import.meta.env`，保持默认即可；若要切换 API 地址，直接改 Vite 代理（见第三节）比引入 `.env` 更简单

---

## 三、Vite 代理配置

### 3.1 原理

开发模式下，前端请求以 `/api` 开头的接口时，Vite 开发服务器会把请求**转发到后端 8080**，从而避免跨域问题：

```js
// user-web/vite.config.js（admin-web/vite.config.ts 相同）
server: {
  port: 5173,                    // 本前端开发端口
  proxy: {
    '/api': {
      target: 'http://localhost:8080',   // 后端地址（后端直跑时）
      changeOrigin: true                 // 修改请求 Host 为 target
    }
  }
}
```

### 3.2 后端地址不同时怎么改

| 场景 | target 值 |
| --- | --- |
| 后端本地直跑（IDE / Maven） | `http://localhost:8080` |
| 后端在 Docker（qqecity-backend） | 本地开发仍是 `http://localhost:8080`（compose 已映射 8080:8080） |
| 后端在远程服务器 | 改为 `http://<服务器IP>:8080` |

> 生产环境（Docker 部署）不走 Vite 代理，由 Nginx 反向代理：`location /api/ { proxy_pass http://backend:8080/api/; }`（见 user-web/nginx.conf）。

---

## 四、用户前端配置说明（user-web）

配置文件：`user-web/vite.config.js`

| 配置项 | 当前值 | 说明 |
| --- | --- | --- |
| 开发端口 | `5173` | `npm run dev` 后访问 http://localhost:5173 |
| API 代理 | `/api` → `http://localhost:8080` | 见第三节 |
| 构建输出 | `dist/` | `npm run build` 产物目录 |
| Docker 端口 | 8081（宿主机） | 由 docker-compose.yml 的 `ports` 决定，改这里不影响 Vite 配置 |

启动命令：

```bash
cd user-web
npm install        # 首次
npm run dev        # 开发模式
```

---

## 五、管理前端配置说明（admin-web）

配置文件：`admin-web/vite.config.ts`

| 配置项 | 当前值 | 说明 |
| --- | --- | --- |
| 开发端口 | `5174` | `npm run dev` 后访问 http://localhost:5174 |
| API 代理 | `/api` → `http://localhost:8080` | 与用户端一致 |
| 构建输出 | `dist/`（sourcemap 关闭） | `npm run build` 产物目录 |
| Docker 端口 | 8082（宿主机） | 由 docker-compose.yml 的 `ports` 决定 |

启动命令：

```bash
cd admin-web
npm install        # 首次
npm run dev        # 开发模式
```

---

## 六、常见修改场景

### 6.1 修改前端开发端口

以用户端为例，将 5173 改为 5174（注意与管理端冲突）：

```js
// user-web/vite.config.js
server: {
  port: 5174,   // 改这里
  ...
}
```

### 6.2 修改后端 API 地址

```js
proxy: {
  '/api': {
    target: 'http://192.168.1.100:8080',   // 改成实际后端地址
    changeOrigin: true
  }
}
```

### 6.3 修改 Docker 部署端口（8081 / 8082）

Docker 模式下前端端口由 `docker-compose.yml` 控制（**不要改 vite.config，改 compose**）：

```yaml
user-web:
  ports:
    - "8081:80"     # 改成 "9091:80" 则通过 http://localhost:9091 访问
```

改完执行 `docker compose up -d --build user-web` 生效。

### 6.4 修改页面级联动的接口前缀

本项目所有接口统一 `/api` 前缀（axios baseURL），不要单独改某页面的请求地址，全局只需改代理/Nginx 一处即可。

---

## 七、构建生产版本

### 7.1 本地构建（产出 dist 静态文件）

```bash
cd user-web
npm run build        # 产物在 user-web/dist/

cd ../admin-web
npm run build        # 产物在 admin-web/dist/
```

产物为纯静态文件，可用任意静态服务器托管（如 Nginx、nginx preview）。

### 7.2 Docker 方式（推荐）

每个前端目录自带 `Dockerfile`（多阶段构建：Node 编译 → Nginx 托管）+ `nginx.conf`：

1. `Dockerfile` 内执行 `npm run build` 生成 dist；
2. 将 dist 复制进 Nginx 镜像，由 `nginx.conf` 托管；
3. Nginx 将 `/api/` 反向代理到后端容器 `http://backend:8080/api/`（前端 history 路由已配置回退到 index.html）。

启动：

```bash
# 项目根目录
docker compose up -d --build user-web admin-web
```

### 7.3 本地预览构建产物

```bash
cd user-web
npm run preview      # 预览 dist（默认端口 4173）
```

> 注意：`npm run preview` 的代理与 dev 一致（读取 vite.config 的 proxy），但 Docker 模式下由 Nginx 代理，无需 preview。
