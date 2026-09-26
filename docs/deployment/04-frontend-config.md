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
frontend/
├── user-web/              # 用户端（面向 C 端用户）
│   ├── src/
│   │   ├── api/           # 接口请求封装
│   │   ├── router/        # 路由配置
│   │   ├── store/         # 状态管理（Pinia）
│   │   ├── views/         # 页面组件
│   │   ├── layout/        # 布局组件
│   │   └── main.js        # 入口文件
│   ├── .env.development   # 开发环境变量
│   ├── .env.production    # 生产环境变量
│   ├── vite.config.js     # Vite 配置
│   └── package.json
│
└── admin-web/             # 管理端（面向运营管理员）
    ├── src/
    │   ├── api/
    │   ├── router/
    │   ├── store/
    │   ├── views/
    │   ├── layout/
    │   └── main.js
    ├── .env.development
    ├── .env.production
    ├── vite.config.js
    └── package.json
```

### 1.2 技术栈

| 技术 | 版本 | 说明 |
|------|------|------|
| Vue | 3.x | 渐进式 JavaScript 框架 |
| Vite | 5.x | 下一代前端构建工具 |
| Vue Router | 4.x | Vue 官方路由 |
| Pinia | 2.x | Vue 状态管理 |
| Axios | 最新 | HTTP 请求库 |
| Element Plus | 最新 | Vue 3 UI 组件库 |

### 1.3 默认端口

| 项目 | 端口 | 访问地址 |
|------|------|----------|
| 用户前端（user-web） | 5173 | <http://localhost:5173> |
| 管理前端（admin-web） | 5174 | <http://localhost:5174> |

---

## 二、环境变量配置

Vite 使用 `.env` 文件管理环境变量，变量必须以 `VITE_` 开头才能在代码中访问。

### 2.1 环境变量文件说明

| 文件名 | 说明 | 生效时机 |
|--------|------|----------|
| `.env.development` | 开发环境变量 | `npm run dev` 时加载 |
| `.env.production` | 生产环境变量 | `npm run build` 时加载 |
| `.env` | 通用环境变量 | 所有环境都加载（优先级最低） |

> 优先级：`.env.[mode].local` > `.env.[mode]` > `.env.local` > `.env`

### 2.2 开发环境变量（.env.development）

**用户前端 `frontend/user-web/.env.development`：**

```env
# 接口请求基础路径（开发环境走代理，填 /api 即可）
VITE_API_BASE_URL=/api

# 应用标题
VITE_APP_TITLE=青启e城

# 开发服务器端口
VITE_PORT=5173

# 是否开启 mock（可选）
VITE_USE_MOCK=false
```

**管理前端 `frontend/admin-web/.env.development`：**

```env
# 接口请求基础路径
VITE_API_BASE_URL=/api

# 应用标题
VITE_APP_TITLE=青启e城管理后台

# 开发服务器端口
VITE_PORT=5174

# 是否开启 mock
VITE_USE_MOCK=false
```

### 2.3 生产环境变量（.env.production）

```env
# 生产环境接口地址（替换为实际后端域名）
VITE_API_BASE_URL=https://api.example.com

# 应用标题
VITE_APP_TITLE=青启e城

# 是否开启 mock
VITE_USE_MOCK=false
```

> 生产环境下 `VITE_API_BASE_URL` 应填写完整的后端 API 地址（含域名），因为生产环境没有 Vite 代理。

### 2.4 代码中使用环境变量

```javascript
// 读取环境变量
const apiBaseUrl = import.meta.env.VITE_API_BASE_URL
const appTitle = import.meta.env.VITE_APP_TITLE

// 常用模式判断
if (import.meta.env.DEV) {
  console.log('开发环境')
}
if (import.meta.env.PROD) {
  console.log('生产环境')
}
```

### 2.5 TypeScript 类型支持（可选）

如需类型提示，在 `src/env.d.ts` 中添加：

```typescript
/// <reference types="vite/client" />

interface ImportMetaEnv {
  readonly VITE_API_BASE_URL: string
  readonly VITE_APP_TITLE: string
  readonly VITE_PORT: string
}

interface ImportMeta {
  readonly env: ImportMetaEnv
}
```

---

## 三、Vite 代理配置

### 3.1 为什么需要代理

浏览器出于安全考虑，存在**跨域限制**（同源策略）。当前后端端口不同时（前端 5173，后端 8080），直接请求后端接口会报 CORS 错误。

Vite 开发服务器提供了代理功能，可以将前端的请求转发到后端，绕过浏览器跨域限制：

```
浏览器 → Vite 开发服务器（5173） → 后端服务器（8080）
         （同域，无跨域）           （服务器之间无跨域）
```

### 3.2 代理配置示例

文件位置：`frontend/user-web/vite.config.js`（用户前端）
文件位置：`frontend/admin-web/vite.config.js`（管理前端）

```javascript
import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import { fileURLToPath, URL } from 'node:url'

// https://vitejs.dev/config/
export default defineConfig({
  plugins: [vue()],

  // 路径别名
  resolve: {
    alias: {
      '@': fileURLToPath(new URL('./src', import.meta.url))
    }
  },

  // 开发服务器配置
  server: {
    // 端口号（从环境变量读取，默认 5173）
    port: parseInt(import.meta.env.VITE_PORT) || 5173,

    // 启动时自动打开浏览器
    open: false,

    // 允许跨域
    cors: true,

    // 监听所有地址（方便局域网访问）
    host: '0.0.0.0',

    // ---------- 代理配置 ----------
    proxy: {
      // 将 /api 开头的请求代理到后端
      '/api': {
        // 后端目标地址
        target: 'http://localhost:8080',

        // 是否改变请求头中的 origin（设为 true 可以避免跨域）
        changeOrigin: true,

        // 是否支持 WebSocket
        ws: false,

        // 路径重写（如果后端接口没有 /api 前缀，需要去掉）
        // 例如：前端请求 /api/user/login → 后端 /user/login
        // rewrite: (path) => path.replace(/^\/api/, '')
        // 注意：本项目后端接口统一前缀 /api，所以不需要重写
      },

      // 如需代理其他路径，可继续添加
      // '/upload': {
      //   target: 'http://localhost:8080',
      //   changeOrigin: true
      // }
    }
  },

  // 构建配置
  build: {
    // 输出目录
    outDir: 'dist',
    // 静态资源目录
    assetsDir: 'assets',
    // 构建时是否生成 source map
    sourcemap: false,
    // 压缩方式
    minify: 'esbuild'
  }
})
```

### 3.3 如何修改代理目标地址

当后端地址或端口变化时，修改 `server.proxy['/api'].target` 即可：

```javascript
proxy: {
  '/api': {
    // 修改为实际后端地址
    target: 'http://192.168.1.100:8080',
    changeOrigin: true
  }
}
```

> 修改代理配置后需要重启前端开发服务器才能生效。

### 3.4 多个代理规则

如果需要代理多个前缀，可以配置多个规则：

```javascript
proxy: {
  '/api': {
    target: 'http://localhost:8080',
    changeOrigin: true
  },
  '/static': {
    target: 'http://localhost:8080',
    changeOrigin: true
  },
  '/ws': {
    target: 'ws://localhost:8080',
    ws: true,
    changeOrigin: true
  }
}
```

---

## 四、用户前端配置说明

### 4.1 路由配置

文件位置：`frontend/user-web/src/router/index.js`

```javascript
import { createRouter, createWebHistory } from 'vue-router'

const routes = [
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/views/Login.vue'),
    meta: { title: '登录', requiresAuth: false }
  },
  {
    path: '/',
    component: () => import('@/layout/MainLayout.vue'),
    redirect: '/home',
    children: [
      {
        path: 'home',
        name: 'Home',
        component: () => import('@/views/Home.vue'),
        meta: { title: '首页', requiresAuth: true }
      },
      // 更多路由...
    ]
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

// 路由守卫（登录校验）
router.beforeEach((to, from, next) => {
  const token = localStorage.getItem('token')
  if (to.meta.requiresAuth && !token) {
    next('/login')
  } else {
    next()
  }
})

export default router
```

### 4.2 API 请求封装

文件位置：`frontend/user-web/src/api/request.js`

```javascript
import axios from 'axios'
import router from '@/router'

// 创建 axios 实例
const request = axios.create({
  // 从环境变量读取基础路径
  baseURL: import.meta.env.VITE_API_BASE_URL,
  // 请求超时时间
  timeout: 15000,
  // 请求头
  headers: {
    'Content-Type': 'application/json'
  }
})

// 请求拦截器
request.interceptors.request.use(
  (config) => {
    // 在请求头中添加 token
    const token = localStorage.getItem('token')
    if (token) {
      config.headers.Authorization = `Bearer ${token}`
    }
    return config
  },
  (error) => {
    return Promise.reject(error)
  }
)

// 响应拦截器
request.interceptors.response.use(
  (response) => {
    const res = response.data
    // 根据后端约定的状态码处理
    if (res.code !== 200) {
      // token 过期或无效，跳转到登录页
      if (res.code === 401) {
        localStorage.removeItem('token')
        router.push('/login')
      }
      return Promise.reject(new Error(res.message || '请求失败'))
    }
    return res
  },
  (error) => {
    return Promise.reject(error)
  }
)

export default request
```

### 4.3 状态管理（Pinia）

文件位置：`frontend/user-web/src/store/user.js`

```javascript
import { defineStore } from 'pinia'
import { ref } from 'vue'
import { loginApi, getUserInfoApi } from '@/api'

export const useUserStore = defineStore('user', () => {
  const token = ref(localStorage.getItem('token') || '')
  const userInfo = ref(null)

  // 登录
  const login = async (username, password) => {
    const res = await loginApi({ username, password })
    token.value = res.data.token
    localStorage.setItem('token', res.data.token)
    return res
  }

  // 获取用户信息
  const getUserInfo = async () => {
    const res = await getUserInfoApi()
    userInfo.value = res.data
    return res
  }

  // 登出
  const logout = () => {
    token.value = ''
    userInfo.value = null
    localStorage.removeItem('token')
  }

  return { token, userInfo, login, getUserInfo, logout }
})
```

### 4.4 菜单配置

用户端菜单通常为底部 Tab 或侧边栏导航，在布局组件中配置：

文件位置：`frontend/user-web/src/layout/MainLayout.vue`

```javascript
const menuList = [
  { path: '/home', name: '首页', icon: 'HomeFilled' },
  { path: '/guarantee', name: '安居保函', icon: 'House' },
  { path: '/loan', name: '青创e贷', icon: 'Money' },
  { path: '/budget', name: '预算消费', icon: 'Wallet' },
  { path: '/safety', name: '金融安全', icon: 'Warning' },
  { path: '/profile', name: '我的', icon: 'User' }
]
```

---

## 五、管理前端配置说明

### 5.1 路由配置

文件位置：`frontend/admin-web/src/router/index.js`

管理端路由通常包含更多页面，且有嵌套层级：

```javascript
import { createRouter, createWebHistory } from 'vue-router'
import Layout from '@/layout/AdminLayout.vue'

const routes = [
  {
    path: '/login',
    component: () => import('@/views/Login.vue'),
    meta: { requiresAuth: false }
  },
  {
    path: '/',
    component: Layout,
    redirect: '/dashboard',
    children: [
      {
        path: 'dashboard',
        name: 'Dashboard',
        component: () => import('@/views/Dashboard.vue'),
        meta: { title: '数据概览', icon: 'DataLine' }
      },
      {
        path: 'user',
        name: 'User',
        component: () => import('@/views/user/UserList.vue'),
        meta: { title: '用户管理', icon: 'User' }
      },
      // 更多管理菜单...
    ]
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

// 路由守卫...
export default router
```

### 5.2 权限控制

管理端通常需要根据角色控制菜单和页面访问权限，在路由守卫中实现：

```javascript
router.beforeEach((to, from, next) => {
  const token = localStorage.getItem('token')
  const userStore = useUserStore()

  if (to.path === '/login') {
    next()
    return
  }

  if (!token) {
    next('/login')
    return
  }

  // 如果没有用户信息，先获取
  if (!userStore.userInfo) {
    userStore.getUserInfo().then(() => {
      // 检查权限
      if (to.meta.roles && !to.meta.roles.includes(userStore.userInfo.role)) {
        next('/403')
      } else {
        next()
      }
    }).catch(() => {
      next('/login')
    })
  } else {
    next()
  }
})
```

### 5.3 菜单配置

文件位置：`frontend/admin-web/src/config/menu.js`

```javascript
export const menuList = [
  {
    path: '/dashboard',
    title: '数据概览',
    icon: 'DataLine',
    roles: ['admin', 'manager']
  },
  {
    path: '/user',
    title: '用户管理',
    icon: 'User',
    roles: ['admin']
  },
  {
    path: '/business',
    title: '业务管理',
    icon: 'Briefcase',
    roles: ['admin', 'manager'],
    children: [
      { path: '/business/guarantee', title: '保函审核', roles: ['admin', 'manager'] },
      { path: '/business/loan', title: '贷款审核', roles: ['admin', 'manager'] }
    ]
  }
  // ...
]
```

---

## 六、常见修改场景

### 6.1 修改后端接口地址

**开发环境：**

修改 `.env.development` 中的 `VITE_API_BASE_URL`（通常保持 `/api`，通过代理转发）：

```env
VITE_API_BASE_URL=/api
```

同时修改 `vite.config.js` 中代理的 `target`：

```javascript
proxy: {
  '/api': {
    target: 'http://后端IP:端口',  // 修改这里
    changeOrigin: true
  }
}
```

> 修改后需要重启前端服务。

**生产环境：**

修改 `.env.production` 中的 `VITE_API_BASE_URL` 为完整后端地址：

```env
VITE_API_BASE_URL=https://api.yourdomain.com
```

### 6.2 修改前端启动端口

修改 `.env.development` 中的 `VITE_PORT`：

```env
VITE_PORT=5175
```

> 用户前端默认 5173，管理前端默认 5174，两个项目端口不能相同。

### 6.3 配置不同环境的 API 地址

按环境分别配置对应的 `.env` 文件：

| 环境 | 配置文件 | VITE_API_BASE_URL 示例 |
|------|----------|----------------------|
| 本地开发 | `.env.development` | `/api`（走代理） |
| 测试环境 | `.env.test` | `https://test-api.example.com` |
| 生产环境 | `.env.production` | `https://api.example.com` |

> 自定义环境需要在 `package.json` 中添加启动脚本：
> ```json
> "scripts": {
>   "dev": "vite",
>   "dev:test": "vite --mode test",
>   "build": "vite build",
>   "build:test": "vite build --mode test"
> }
> ```

### 6.4 修改页面标题

修改 `.env.development` / `.env.production` 中的 `VITE_APP_TITLE`：

```env
VITE_APP_TITLE=青启e城 - 青年创业金融服务平台
```

在 `index.html` 中使用（如果配置了的话）：

```html
<title><%= VITE_APP_TITLE %></title>
```

或在 `main.js` 中动态设置：

```javascript
document.title = import.meta.env.VITE_APP_TITLE
```

### 6.5 配置路径别名

`vite.config.js` 中已配置 `@` 指向 `src` 目录：

```javascript
import { fileURLToPath, URL } from 'node:url'

resolve: {
  alias: {
    '@': fileURLToPath(new URL('./src', import.meta.url))
  }
}
```

使用方式：

```javascript
import request from '@/api/request'
import Home from '@/views/Home.vue'
```

### 6.6 开启 / 关闭自动打开浏览器

修改 `vite.config.js` 中的 `server.open`：

```javascript
server: {
  open: true,   // 启动时自动打开浏览器
  // 或指定 URL
  // open: 'http://localhost:5173/login'
}
```

---

## 七、构建生产版本

### 7.1 构建命令

```bash
# 进入前端目录
cd frontend/user-web     # 或 admin-web

# 构建生产版本
npm run build
```

构建完成后，产物输出到 `dist/` 目录：

```
dist/
├── index.html
├── assets/
│   ├── index.xxx.js
│   ├── index.xxx.css
│   └── 图片/字体等静态资源
└── favicon.ico
```

### 7.2 构建预览

构建完成后，可以本地预览生产版本效果：

```bash
npm run preview
```

默认在 `4173` 端口启动预览服务器。

### 7.3 部署到 Nginx

将 `dist/` 目录上传到服务器，配置 Nginx：

```nginx
server {
    listen 80;
    server_name yourdomain.com;
    root /var/www/qingqi/dist;
    index index.html;

    # 前端路由 history 模式需要配置
    location / {
        try_files $uri $uri/ /index.html;
    }

    # 接口反向代理
    location /api/ {
        proxy_pass http://backend-server:8080;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
    }

    # 静态资源缓存
    location ~* \.(js|css|png|jpg|jpeg|gif|ico|svg)$ {
        expires 7d;
        add_header Cache-Control "public, immutable";
    }
}
```

### 7.4 构建优化建议

- 开启 **Gzip** 压缩（Nginx 或构建时生成 .gz 文件）
- 配置 **CDN** 加速静态资源
- 路由**懒加载**（已配置，`() => import('@/views/xxx.vue')`）
- 使用 **vite-plugin-compression** 插件预生成 gzip 文件
- 大图片资源压缩后再使用
- 生产环境关闭 `sourcemap`（已默认关闭）

---

## 前端配置检查清单

- [ ] Node.js 版本 >= 18，npm 版本 >= 9
- [ ] `.env.development` 环境变量配置正确
- [ ] `.env.production` 生产环境变量配置正确
- [ ] `vite.config.js` 代理目标地址正确（指向后端）
- [ ] 前端端口未被占用（用户端 5173 / 管理端 5174）
- [ ] `npm install` 安装依赖无报错
- [ ] `npm run dev` 启动成功，页面可访问
- [ ] 代理转发正常，接口请求无跨域错误
- [ ] 演示账号可正常登录
- [ ] `npm run build` 构建成功，dist 目录生成正常
- [ ] 生产环境 API 地址配置正确
