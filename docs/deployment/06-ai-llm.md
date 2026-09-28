# 06 · AI / LLM 调用配置与排障

> 本系统两处功能调用 LLM（Agent）：**智能客服 agent 模式** 与 **反诈情景演练**。
> 其余标"AI"的能力（保函 AI 合同复审、注册学生证 OCR）为**本地模拟/规则引擎，不依赖 LLM**。
>
> 本文说明：如何配置 LLM 依赖、如何验证生效、以及"AI 降级本地"的排查方法。

---

## 一、哪些功能真的调用 LLM

| 功能 | 模块 | LLM 用途 | 无 LLM 时的表现 |
|------|------|----------|-----------------|
| 智能客服（agent 模式） | 智能中台 → 对话 | 知识库召回 + 用户画像 + LLM 组织回答 | 降级本地知识库模板回答，消息标注"AI 降级本地" |
| 反诈情景演练 | 金融安全 → 情景演练 | LLM 扮演诈骗分子 / 反诈导师（角色人设） | 降级本地固定剧本 |
| 保函 AI 合同复审 | 安居保函 → 申请流转 | **不调用 LLM**（关键词规则引擎，置信度 95% 模拟） | 无影响 |
| 学生证识别（注册审核） | 注册 → 学籍审查 | **不调用 LLM**（模拟 OCR，按演示字段返回） | 无影响 |

**核心依赖：后端通过 `AgentLLMClient` 调用 OpenAI 兼容的 `/v1/chat/completions` 接口。**
该客户端同时服务于上面两处 LLM 功能，配置、排障方式完全相同。

---

## 二、LLM 依赖选择（二选一）

### 方案 A：本机 Ollama（离线免费，推荐演示用）

**1. 安装 Ollama**

- Windows：下载安装包安装，或绿色解压（例如 `D:\Ollama\ollama.exe`）
- Linux / macOS：
  ```bash
  curl -fsSL https://ollama.com/install.sh | sh
  ```

**2. 拉取模型（约 1.9 GB）**

```bash
ollama pull qwen2.5:3b-instruct
```

> 模型越小越快：演示机性能一般建议 `qwen2.5:3b-instruct`；追求速度可 `qwen2.5:0.5b`；追求质量可 `qwen2.5:7b-instruct`（更慢，注意超时）。

**3. 启动服务**

```bash
ollama serve        # 默认监听 127.0.0.1:11434
```

验证：`curl http://localhost:11434/` 应返回 `HTTP 200`。

**4. 模型常驻内存（关键，避免每次请求冷加载 1.9GB）**

设置环境变量 `OLLAMA_KEEP_ALIVE=-1`（-1 = 常驻，不卸载）：

```bash
# Windows（永久）
setx OLLAMA_KEEP_ALIVE "-1"

# Linux / macOS（写入 shell 配置后重启）
export OLLAMA_KEEP_ALIVE=-1
```

不设置时 Ollama 空闲约 5 分钟即卸载模型，下次请求要重新加载 1.9GB，单次可能 40+ 秒甚至超时降级——**这是"AI 降级本地"的高发原因之一**。

**5. 开机自启（防止重启后 AI 静默失效）**

- **Windows**：在启动文件夹 `Win+R → shell:startup` 放置 `Ollama-start.vbs`：
  ```vbs
  Set WshShell = CreateObject("WScript.Shell")
  Set env = WshShell.Environment("Process")
  env("OLLAMA_KEEP_ALIVE") = "-1"
  WshShell.Run """<Ollama安装路径>\ollama.exe"" serve", 0, False
  ```
- **Linux**：建议使用 systemd 服务或 `crontab @reboot` 启动 `ollama serve`。

**6. 后端 .env 指向本机 Ollama**

在项目根 `.env` 配置（Docker 模式用 `host.docker.internal`，本地开发模式用 `localhost`）：

```ini
CHAT_ENGINE=agent
CHAT_LLM_API_KEY=ollama
CHAT_LLM_ENDPOINT=http://host.docker.internal:11434/v1/chat/completions
CHAT_LLM_MODEL=qwen2.5:3b-instruct
```

> `CHAT_LLM_API_KEY` 填任意非空值即可（Ollama 不校验 Key；该值仅用于后端判断"是否已配置 AI"）。
> 修改 `.env` 后需重建后端容器生效：`docker compose up -d --build backend`。

### 方案 B：云端 API（豆包 / DeepSeek 等，响应快、更稳）

在 `.env` 配置：

```ini
CHAT_ENGINE=agent
CHAT_LLM_API_KEY=sk-你的真实APIKey
CHAT_LLM_ENDPOINT=https://ark.cn-beijing.volces.com/api/v3/chat/completions
CHAT_LLM_MODEL=doubao-pro-32k
```

> 默认 `application.yml` 的兜底即火山方舟豆包；用 DeepSeek 时把 ENDPOINT 换成 `https://api.deepseek.com/v1/chat/completions`、MODEL 换成 `deepseek-chat`。

---

## 三、.env 变量说明

| 变量 | 必填 | 默认值 | 说明 |
|------|------|--------|------|
| `CHAT_ENGINE` | 否 | `local` | `local`=仅本地知识库（离线稳定）；`agent`=启用 LLM 增强 |
| `CHAT_LLM_API_KEY` | agent 时必须 | 空 | 任意非空即视为已配置 LLM；空则 agent 自动禁用 |
| `CHAT_LLM_ENDPOINT` | 否 | 火山方舟 `/chat/completions` | OpenAI 兼容接口地址 |
| `CHAT_LLM_MODEL` | 否 | `doubao-pro-32k` | 模型名，Ollama 用 `qwen2.5:3b-instruct` 等 |

后端内部性能参数（`backend/src/main/resources/application.yml` → `chat.llm`）：

| 参数 | 当前值 | 说明 |
|------|--------|------|
| `timeout-seconds` | `60` | 单次 LLM 调用超时；超时自动降级本地 |
| `max-tokens` | `256` | 输出上限；过大（如 1024）在本地 CPU 上会拖到 40-50 秒 |
| 历史截断 | 最近 3 轮 | 防聊天越长、输入越长、越慢（在 `AgentLLMClient` 内实现） |

---

## 四、验证 AI 是否生效

```bash
# 1) Ollama 服务（方案 A）
curl http://localhost:11434/          # 期望 HTTP 200

# 2) 后端引擎状态（需登录 token）
curl -H "Authorization: Bearer <token>" http://localhost:8080/api/v1/chat/engine-status
# 期望: activeEngine=agent, agentAvailable=true

# 3) 发一条 agent 聊天
curl -X POST http://localhost:8080/api/v1/chat/messages \
  -H "Authorization: Bearer <token>" -H "Content-Type: application/json" \
  -d '{"message":"你好","mode":"agent","scene":"GENERAL"}'
# 期望: engineMode=agent（而非 fallback），内容为 LLM 生成
```

用户端聊天页消息标签：**AI 增强** = agent 生效；**AI 降级本地** = LLM 调用失败走了兜底。

---

## 五、故障排查："AI 降级本地" / LLM 一直失败

| 现象 | 原因 | 处理 |
|------|------|------|
| 消息标"AI 降级本地"，且从未成功 | **Ollama 服务未启动 / 未安装**（最常见） | `curl localhost:11434` 探活；启动 `ollama serve`；装好开机自启 |
| 重启电脑后失效 | 未设置开机自启 | 按上文"开机自启"配置；并确认 `OLLAMA_KEEP_ALIVE=-1` 已设置 |
| 首次请求很慢（40+ 秒），随后正常 | 模型冷加载（keep_alive 到期卸载） | 设置 `OLLAMA_KEEP_ALIVE=-1` 常驻内存 |
| 每次请求都 30-50 秒，接近超时 | 模型过大 / `max-tokens` 过大 / 历史过长 | 换 `qwen2.5:0.5b`；确认 `max-tokens=256`；后端已自动截断历史 3 轮 |
| 后端日志 `[AgentLLM] 调用失败` | endpoint 不通 / Key 无效 / 模型名不存在 | 核对 `.env` 四项；方案 B 检查 Key 是否有效 |
| `engine-status` 返回 `agentAvailable=false` | `CHAT_LLM_API_KEY` 为空 | 填任意非空值并重建 backend |

> 后端降级是**自动且静默**的（catch → 本地兜底），所以"看起来能回消息"不代表 AI 生效，务必按第四节验证。
