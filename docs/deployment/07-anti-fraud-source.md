# 青启e城 · 反诈预警实时源配置（模块5 金融安全）

> 本文档属于「本地部署与配置指南」系列，完整目录见 [README.md](./README.md)

> 适用场景：金融安全页「实时反诈预警」数据源配置。默认"模拟实时"（演示稳定），可选"真实爬取"（公开反诈源），抓取失败自动回退模拟数据（不空页）。

---

## 目录

- [一、双模式说明](#一双模式说明)
- [二、.env 配置](#二env-配置)
- [三、前端自动刷新](#三前端自动刷新)
- [四、验证方法](#四验证方法)
- [五、常见问题](#五常见问题)

---

## 一、双模式说明

「实时反诈预警」支持两种数据源（`ANTI_FRAUD_SOURCE` 控制，默认 `mock`）：

| 模式 | 行为 | 适用场景 |
|------|------|----------|
| `mock`（默认） | 后端定时（默认每 30 分钟）从模板池轮换生成一条带**当前时间戳**的预警；前端每 30 秒自动轮询刷新，列表顶部持续滚动新条目 | **答辩/演示推荐**：观感实时、离线稳定、不依赖外网 |
| `real` | 后端定时爬取公开反诈信息源（默认中国警察网反诈频道，URL 可覆盖），解析含"诈/骗/预警"的标题入库；**抓取失败自动回退**——表内既有模拟数据继续展示 | 需要真实外网信息时选用；依赖目标站点可访问性 |

> 无论哪种模式，预警数据都存在 `biz_anti_fraud_alert` 表（人工数据 + 自动生成混合展示，按发布时间倒序）。
> 自动生成的数据条数超过 30 条时自动清理最旧记录，防止无限膨胀。

---

## 二、.env 配置

在**项目根目录** `.env` 中配置（`docker-compose.yml` 已透传，修改后需重建后端容器生效）：

```bash
# 反诈预警数据源：mock=模拟实时轮换（默认，演示稳定）/ real=真实爬取公开反诈源
ANTI_FRAUD_SOURCE=mock

# 轮询间隔（分钟）：mock 轮换与 real 爬取的执行周期，默认 30
ANTI_FRAUD_POLL_MINUTES=30

# 真实爬取源 URL（仅 ANTI_FRAUD_SOURCE=real 时生效），默认中国警察网反诈频道
ANTI_FRAUD_SOURCE_URL=https://www.cpd.com.cn/n15729503/
```

修改 `.env` 后重建后端：

```bash
docker compose up -d --build backend
```

### 本地开发（非 Docker）模式

在后端环境变量或启动参数中注入同名变量即可（`application.yml` 已有兜底默认值，缺省即 mock/30 分钟）。

---

## 三、前端自动刷新

用户端金融安全页（`user-web/src/views/Safety.vue`）：

- 每 **30 秒**自动轮询 `GET /api/v1/safety/alerts`，无需手动刷新即可看到新预警滚动；
- 区块右上角标签显示「实时滚动 · 30秒自动刷新 · 模拟实时」；
- 页面卸载时自动清除定时器（不影响性能）。

---

## 四、验证方法

### 1）模拟实时（mock）

```bash
# 查看预警列表（需登录 token），确认列表顶部时间戳持续更新
curl -H "Authorization: Bearer <token>" http://localhost:8080/api/v1/safety/alerts

# 后端日志出现定时轮换记录
docker logs qqecity-backend | grep 反诈预警
# 期望: [反诈预警] mock 轮换生成：xxx（当前时间）
```

### 2）真实爬取（real）

```bash
# .env 设置 ANTI_FRAUD_SOURCE=real 后重建 backend
docker compose up -d --build backend

# 后端日志
docker logs qqecity-backend | grep 反诈预警
# 成功: [反诈预警] real 爬取完成，新增 N 条（来源 https://...）
# 失败: [反诈预警] 真实爬取失败，回退模拟数据：...
```

失败时页面不空：预警列表仍展示表内既有模拟数据（兜底）。

---

## 五、常见问题

| 现象 | 原因 | 处理 |
|------|------|------|
| 列表不滚动、时间戳不更新 | 后端未重建 / 前端未刷新 | `docker compose up -d --build backend` 后硬刷新前端（Ctrl+F5） |
| `real` 模式没有新增条目 | 目标站点不可访问 / 结构变更 / 无含"诈/骗/预警"标题 | 换 `ANTI_FRAUD_SOURCE_URL`（如 12321、国家反诈中心公开页）；确认后端能出网（容器内 curl 测试） |
| 预警数量异常增多 | 轮询间隔误设过小 | `ANTI_FRAUD_POLL_MINUTES` 单位是**分钟**（后端按 ISO-8601 PTnM 解析）；自动清理上限 30 条 |
| 人工维护的预警不见了 | 自动生成条目把最旧人工数据挤出 30 条上限 | 人工预警的 `publish_time` 保持较新，或在管理端重新新增 |

---

## 相关文档

- [docker-compose.md](./docker-compose.md) — 一键编排与容器环境变量
- [03-backend-config.md](./03-backend-config.md) — 后端 application.yml 配置
- [06-ai-llm.md](./06-ai-llm.md) — AI/LLM 配置（外部密钥统一由 .env 提供）
