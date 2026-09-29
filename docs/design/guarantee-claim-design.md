# 《青启 e 城》违约索赔功能设计书

> 版本：v1.0 ｜ 日期：2026-09-29 ｜ 状态：设计定稿（待前端补缺评审）
> 需求来源：
>
> `docs/common/requirements.md`
>
>  
>
> **G-6 违约索赔（提交证据 → AI 初审 → 人工复核 → 申辩期）【模拟】**
>
>  P1 / S6
> 对应模块：模块 1・安居金融风控（租房履约保函全流程扩展）



***

## 1. 功能概述

租房履约保函场景中，当租客发生违约（如拖欠房租、提前退租、损坏房屋等）时，**房东（保函受益人）可凭已开立保函向银行发起违约索赔**；系统以模拟 AI 初审裁决，低风险小额索赔自动速赔，存疑索赔进入**租客申辩期**后转银行运营岗（banker）**人工复核**，最终赔付或拒绝并结案。全流程银行能力（赔付 / 扣款）均为**模拟桩**并全程标注 "模拟"。

### 1.1 与需求映射



| 需求项   | 内容                                 | 优先级     |
| ----- | ---------------------------------- | ------- |
| G-6   | 违约索赔：提交证据 → AI 初审 → 人工复核 → 申辩期【模拟】 | P1 / S6 |
| G-6-1 | 房东在 "索赔页" 提交违约证据                   | P1      |
| G-6-2 | 系统记录证据、展示初审结果（演示自动通过）              | P1      |
| G-6-3 | 租客申辩期（存疑索赔）                        | P2      |
| G-6-4 | banker 人工复核（赔付 / 拒绝）               | P1      |

### 1.2 现状盘点（代码基线核对）



| 层       | 状态         | 说明                                                                                         |
| ------- | ---------- | ------------------------------------------------------------------------------------------ |
| 后端      | ✅ 已闭环      | `GuaranteeClaimService/Controller` 完整实现状态机与 7 个接口（见 §6）                                    |
| 数据表     | ✅ 已建       | `biz_guarantee_claim`（预留表，schema.sql 已含，见 §5）                                              |
| 管理端     | ✅ 已实现      | 人工复核页 `admin-web/src/views/business/claim-review/index.vue` + `api/business/claim.ts`，路由已挂 |
| **用户端** | ❌ **完全缺失** | `Guarantee.vue` 仅页面描述含 "违约索赔" 文案，**无索赔入口、无发起页、无申辩页、无索赔记录**—— 即 "功能好像不存在" 的直接原因             |

**结论**：违约索赔后端与运营端已完备，**缺口集中在用户端三张页面**（房东发起索赔、租客申辩、双方索赔记录），本设计书据此展开。



***

## 2. 角色与权限



| 角色           | 场景  | 能力                               | 鉴权                              |
| ------------ | --- | -------------------------------- | ------------------------------- |
| 房东（保函受益人）    | 用户端 | 发起索赔（提交金额 / 原因 / 证据）、查看本人索赔记录与进度 | 须为该保函 `landlord_id` 归属人（后端越权校验） |
| 租客（被索赔人）     | 用户端 | 查看针对自己的索赔、申辩期内提交反证、查看结案通知        | 仅 `tenant_id` 本人                |
| 银行运营岗 banker | 管理端 | 人工复核队列、赔付 / 拒绝裁决                 | `ADMIN` / `BANK_OPERATOR`       |
| 系统管理员        | 管理端 | 同 banker（人工复核）                   | `ADMIN`                         |

> 越权保护（后端已实现）：发起索赔仅限保函归属房东；申辩仅限被索赔租客；人工复核仅限后台角色（修复普通用户越权漏洞）。



***

## 3. 业务流程与状态机

### 3.1 状态机



```
&#x20;                    ┌──────────────────────────────────────────────┐

&#x20;                    │                                              │

&#x20; SUBMITTED ──▶ AI\_REVIEW ──┬─ 低风险速赔(证据齐全且小额≤¥2000) ─▶ APPROVED ─▶ CLOSED

&#x20;    (房东提交)   (自动AI初审) │                                              (赔付完成/结案)

&#x20;                    │       └─ 存疑(大额/证据不足/命中风控规则) ─▶ DEFENSE\_PERIOD

&#x20;                    │                                              (租客申辩期)

&#x20;                    │                                                    │ 租客提交反证

&#x20;                    │                                                    ▼

&#x20;                    │                                              MANUAL\_REVIEW

&#x20;                    │                                          (banker 人工复核)

&#x20;                    │                                                ├─ 赔付 ─▶ APPROVED ─▶ CLOSED

&#x20;                    │                                                └─ 拒绝 ─▶ REJECTED ─▶ CLOSED

&#x20;                    └──────────────────────────────────────────────────────────────────────────────
```

### 3.2 步骤说明



| 步骤       | 触发             | 动作                                                                   | 状态变化                         |
| -------- | -------------- | -------------------------------------------------------------------- | ---------------------------- |
| 1 发起索赔   | 房东（保函 ACTIVE）  | 提交 `claim_amount / claim_reason / evidence_files`（JSON 数组，支持图片 + 文件） | → SUBMITTED                  |
| 2 AI 初审  | 提交即自动          | 模拟 AI 按 §4 规则计算置信度，记录 `ai_review_result / ai_review_detail`          | → AI\_REVIEW                 |
| 3a 低风险速赔 | 置信度 ≥ 阈值且无命中规则 | 自动通过：`payout_amount`= 索赔额，保函置 CLAIMED                                | → APPROVED → CLOSED          |
| 3b 存疑转申辩 | 置信度 < 阈值或命中规则  | 通知租客申辩（站内信）                                                          | → DEFENSE\_PERIOD            |
| 4 租客申辩   | 申辩期内           | 提交 `defense_content` 反证                                              | → MANUAL\_REVIEW             |
| 5 人工复核   | banker / ADMIN | 裁决 APPROVED（填赔付额）或 REJECTED（填原因）；赔付时保函置 CLAIMED                      | → APPROVED/REJECTED → CLOSED |
| 6 结案通知   | 复核完成           | 站内信通知租客结案结果                                                          | CLOSED                       |

> 说明：
>
> `DEFENSE_PERIOD`
>
>  无自动超时强制（演示口径可扩展），申辩提交即转人工复核。



***

## 4. AI 初审规则（模拟，不查真实征信 / 司法）



| 因子     | 规则                               | 置信度影响                    |
| ------ | -------------------------------- | ------------------------ |
| 证据完整性  | 证据材料齐全（evidence\_files 非空且 ≥1 项） | 充足 +，缺失 −                |
| 索赔金额   | 小额（≤ ¥2000）→ 低风险速赔               | +，大额 −                   |
| 风控规则命中 | 原因含 欺诈 / 勒索 / 无理 等异常词            | 命中即转人工                   |
| 综合置信度  | 阈值 60 分                          | ≥60 且无命中 → 自动通过；否则存疑转申辩期 |

**速赔上限**：单笔索赔不超过保函金额（后端校验，`claim_amount ≤ guarantee_amount`）。



***

## 5. 数据模型 `biz_guarantee_claim`



| 字段                                        | 类型                   | 说明                                                                                       |
| ----------------------------------------- | -------------------- | ---------------------------------------------------------------------------------------- |
| id                                        | BIGINT PK            | 主键                                                                                       |
| claim\_no                                 | VARCHAR(50) UNIQUE   | 索赔编号（CLM 前缀 + 时间戳）                                                                       |
| guarantee\_id / guarantee\_no             | BIGINT / VARCHAR(50) | 关联保函                                                                                     |
| claimant\_id / claimant\_name             | BIGINT / VARCHAR(50) | 索赔人（房东）                                                                                  |
| tenant\_id                                | BIGINT               | 被索赔租客                                                                                    |
| claim\_amount                             | DECIMAL(18,2)        | 索赔金额（≤ 保函额）                                                                              |
| claim\_reason                             | TEXT                 | 索赔原因                                                                                     |
| evidence\_files                           | TEXT                 | 证据材料（JSON 数组：名称 / 内容 /data URL，图片 + 文件）                                                  |
| claim\_status                             | VARCHAR(30)          | SUBMITTED / AI\_REVIEW / DEFENSE\_PERIOD / MANUAL\_REVIEW / APPROVED / REJECTED / CLOSED |
| ai\_review\_result / ai\_review\_detail   | VARCHAR(20) / TEXT   | AI 初审结果与逐条详情                                                                             |
| reject\_reason                            | VARCHAR(500)         | 拒绝原因                                                                                     |
| defense\_content                          | TEXT                 | 租客申辩内容                                                                                   |
| payout\_amount                            | DECIMAL(18,2)        | 实际赔付金额                                                                                   |
| submit\_time / review\_time / close\_time | DATETIME             | 时间戳                                                                                      |
| deleted / create\_time / update\_time     | —                    | 逻辑删除与审计                                                                                  |



***

## 6. 接口清单（后端已实现，`/api/v1/guarantee/claims`）



| # | 方法   | 路径                     | 说明                   | 角色           | 状态 |
| - | ---- | ---------------------- | -------------------- | ------------ | -- |
| 1 | POST | `/`                    | 房东发起索赔（自动触发 AI 初审）   | 房东           | ✅  |
| 2 | GET  | `/`                    | 分页查询索赔（房东看发起、租客看被索赔） | 双方           | ✅  |
| 3 | GET  | `/{id}`                | 索赔详情                 | 双方           | ✅  |
| 4 | PUT  | `/{id}/defense`        | 租客提交申辩               | 租客           | ✅  |
| 5 | PUT  | `/{id}/review`         | banker 人工复核（赔付 / 拒绝） | ADMIN/banker | ✅  |
| 6 | GET  | `/manual-review-queue` | 人工复核队列               | ADMIN/banker | ✅  |
| 7 | GET  | `/status-flow`         | 状态流转说明               | 公开           | ✅  |

**curl 示例**：



```
\# 房东发起索赔

curl -X POST http://localhost:8080/api/v1/guarantee/claims \\

&#x20; -H "Authorization: Bearer \<JWT>" -H "Content-Type: application/json" \\

&#x20; -d '{"guaranteeId":1,"claimAmount":1000,"claimReason":"租客拖欠房租","evidenceFiles":"\[{\\"name\\":\\"合同.jpg\\",\\"data\\":\\"data:image/jpeg;base64,...\\",\\"isImage\\":true}]"}'

\# 租客申辩

curl -X PUT http://localhost:8080/api/v1/guarantee/claims/{id}/defense \\

&#x20; -H "Authorization: Bearer \<JWT>" -H "Content-Type: application/json" \\

&#x20; -d '{"defenseContent":"已提前 7 天告知退租，非违约"}'

\# banker 人工复核（赔付）

curl -X PUT http://localhost:8080/api/v1/guarantee/claims/{id}/review \\

&#x20; -H "Authorization: Bearer \<JWT>" -H "Content-Type: application/json" \\

&#x20; -d '{"decision":"APPROVED","payoutAmount":1000,"remark":"证据充分"}'

\# 管理端人工复核队列

curl -X GET "http://localhost:8080/api/v1/guarantee/claims/manual-review-queue?pageNum=1\&pageSize=10" \\

&#x20; -H "Authorization: Bearer \<JWT>"
```



***

## 7. 页面设计（本次补缺重点：用户端）

### 7.1 入口



* **安居保函页（**`/guarantee`**）**：在保函申请记录操作列新增「**索赔**」按钮（仅该保函归属房东、且保函状态 = 已开立 ACTIVE 时可见可点）；页面新增 tab / 区块「**我的索赔**」展示索赔记录与进度。

### 7.2 房东・发起索赔页



* 选择已开立保函（自动带出保函编号 / 金额）

* 表单：索赔金额（≤ 保函额校验、超限红字提示）、索赔原因（必填 textarea）、证据材料上传（**图片 + 文件**，复用佐证材料组件，预览区小尺寸，JSON 数组存 `evidence_files`）

* 提交后自动触发 AI 初审，直接展示**初审结果卡**（速赔通过绿 / 转申辩黄，附置信度与逐条原因）—— 样式对齐注册审核 / AI 审核留痕语言

### 7.3 租客・申辩页



* 站内信 / 索赔记录进入「待申辩」详情：展示索赔信息（金额 / 原因 / 证据预览）与 AI 初审详情

* 申辩期内可提交 `defense_content` 反证；提交后状态转人工复核

* 结案后展示赔付 / 拒绝结论与原因

### 7.4 双方・索赔记录页



* 分页表格：索赔编号、保函编号、对方、索赔金额、AI 初审结论、状态（速赔 / 申辩中 / 人工复核 / 已赔付 / 已拒绝）、提交时间、操作（详情）

* 状态徽章配色：SUBMITTED 灰 / AI\_REVIEW 蓝 / DEFENSE\_PERIOD 橙 / MANUAL\_REVIEW 紫 / APPROVED 绿 / REJECTED 红 / CLOSED 灰

### 7.5 管理端・人工复核页（已实现，按 AI 留痕语言微调可选）



* `admin-web/src/views/business/claim-review/index.vue`：复核队列 + 赔付 / 拒绝弹窗（赔付金额上限 = 保函额），已路由挂载



***

## 8. 演示账号与用例



| 账号                           | 角色           | 用例                             |
| ---------------------------- | ------------ | ------------------------------ |
| landlord01/123456            | 房东           | 对已开立保函发起小额索赔（证据齐全）→ 演示 AI 速赔通过 |
| tenant 对应账号                  | 租客           | 收到索赔通知 → 申辩期内提交反证 → 转人工复核      |
| banker01/123456、admin/123456 | banker/ADMIN | 人工复核队列 → 赔付 / 拒绝 → 结案          |

**演示链路**：房东索赔 → AI 初审速赔 / 转申辩 → 租客申辩 → banker 复核 → 赔付 / 拒绝 → 双方收到结案通知。



***

## 9. 模拟口径与合规标注



* 所有赔付金额为**模拟**，不产生真实资金流转；页面 / 接口文案全程标注「模拟」。

* AI 初审不查真实征信、不接司法系统，仅为演示规则引擎。

* 索赔金额不得超过保函金额（真实业务边界，模拟中保留校验）。

* 证据材料仅存于演示库（JSON data URL），不对外传输。



***

## 10. 实施计划（补齐用户端缺口）



| 步骤 | 内容                                         | 产出               |
| -- | ------------------------------------------ | ---------------- |
| 1  | 用户端 API 封装 `api/guarantee/claim.ts`（7 个接口） | 前端 API 层         |
| 2  | 安居保函页新增「我的索赔」tab + 保函记录「索赔」入口              | Guarantee.vue 扩展 |
| 3  | 房东发起索赔表单 + 证据上传 + AI 初审结果卡                 | ClaimSubmit 组件   |
| 4  | 租客申辩页（待申辩详情 + 反证表单）                        | ClaimDefense 组件  |
| 5  | 双方索赔记录页（分页 + 状态徽章 + 详情）                    | ClaimList 组件     |
| 6  | 全链路联调（landlord01 索赔 → 申辩 → banker 复核）      | 实测通过             |

> 后端与管理端无需改动；如需超时自动流转（申辩期 N 天自动转人工 / 驳回）可另议扩展。