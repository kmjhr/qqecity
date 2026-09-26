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
    └── package.json
```
