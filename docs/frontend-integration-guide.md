# 青启e城 · 前后端联调步骤指南（未联调模块全覆盖）

> 本文档梳理**所有后端已实现但前端未联调**的模块，给出每个模块的联调步骤（API 文件 → View 页面 → Router → Menu）、接口契约、验证方式。
>
> 已联调模块（不在本文档范围）：user-web 的 auth / user / guarantee(G-1~G-5) / loan(L-1~L-3) / budget / bookkeeping / safety(S-1~S-2) / message；admin-web 的 auth / system/user。
>
> 技术栈：后端 Spring Boot 3.2 + MyBatis-Plus；前端 Vue3 + Vite5 + Element Plus + Pinia；接口统一 `/api/v1/**`，返回 `{code,message,data}`，鉴权 `Authorization: Bearer <JWT>`。

---

## 一、联调现状总览

### 1.1 user-web 已联调模块

| 模块 | API 文件 | View | 后端接口前缀 | 状态 |
|---|---|---|---|---|
| 认证 | auth.js | Login/Register | /v1/auth | ✅ |
| 用户 | user.js | Profile | /v1/user | ✅ |
| 安居保函 G-1~G-5 | guarantee.js | Guarantee | /v1/guarantee | ✅ |
| 青创e贷 L-1~L-3 | loan.js | Loan | /v1/loan | ✅ |
| 预算消费 C-1~C-4 | budget.js | Budget | /v1/budget | ✅ |
| 经营记账 B-1 | bookkeeping.js | Bookkeeping | /v1/bookkeeping | ✅ |
| 反诈 S-1~S-2 | safety.js | Safety | /v1/safety | ✅ |
| 消息中心 | message.js | Message | /v1/message | ✅ |

### 1.2 user-web 未联调模块（本文档覆盖）

| # | 模块 | 对应补充计划 | 后端接口前缀 | 优先级 |
|---|---|---|---|---|
| 1 | 保函索赔闭环 G-6 | 步骤1 | /v1/guarantee/claims | P0 |
| 2 | 保函电子签约（G-2 增强） | 步骤1 | /v1/guarantee/{id}/landlord-confirm | P0 |
| 3 | 贷款增强（提款/还款/循环贷流水/观察期/商户审核） | 步骤3 | /v1/loan/* | P0 |
| 4 | 政策库匹配与推送 | 步骤2 | /v1/policy | P1 |
| 5 | 保险代销匹配 | 步骤4 | /v1/insurance | P1 |
| 6 | 现金流风险预警 | 步骤4 | /v1/operation/cashflow-warning | P1 |
| 7 | 财税科普与个转企引导 | 步骤4 | /v1/operation/finance | P2 |
| 8 | 高频借贷预警 | 步骤5 | /v1/consumption/high-freq-borrow | P0 |
| 9 | 征信监测 | 步骤5 | /v1/consumption/credit-monitor | P0 |
| 10 | 三层消费引导 | 步骤5 | /v1/consumption/guide | P1 |
| 11 | 风险测评 | 步骤5 | /v1/consumption/risk-assessment | P0 |
| 12 | 理财匹配 | 步骤5 | /v1/consumption/finance-product | P0 |
| 13 | 青年成长信用画像 | 步骤7 | /v1/profile | P1 |
| 14 | AI 对话引擎 | 步骤7 | /v1/chat | P2 |
| 15 | 金融安全增强（征信解读/逾期预判/征信健康/情景教学） | 步骤6 | /v1/safety/* | P1 |
| 16 | 多渠道流水聚合 + 防刷单 | 步骤7 | /v1/cashflow/* | P2 |

### 1.3 admin-web 未联调模块（本文档覆盖）

| # | 模块 | 后端接口前缀 | 优先级 |
|---|---|---|---|
| 1 | 数据看板（动态数据） | /admin/v1/user 等聚合 | P1 |
| 2 | 保函管理 + 人工复核 | /v1/guarantee, /v1/guarantee/claims | P0 |
| 3 | 贷款管理 + 商户审核 | /v1/loan, /admin/v1/... | P1 |
| 4 | 消息管理 | /v1/message（管理端扩展） | P2 |
| 5 | 风险预警管理 | /v1/operation/cashflow-warning, /v1/consumption/* | P1 |
| 6 | 保险/政策/理财 产品管理 | /v1/insurance, /v1/policy, /v1/consumption/finance-product | P2 |

---

## 二、统一联调约定

### 2.1 API 文件结构（user-web）

每个模块对应 `src/api/{模块名}.js`，统一 `import request from './request'`，导出命名函数：

```js
// src/api/consumption.js 示例
import request from './request'

/** 高频借贷检测 */
export function detectHighFreqBorrow() {
  return request.post('/v1/consumption/high-freq-borrow/detect')
}

/** 高频借贷预警历史 */
export function getHighFreqBorrowHistory(params) {
  return request.get('/v1/consumption/high-freq-borrow/history', { params })
}
```

**注意**：`request` 响应拦截器已自动解包 `res.data`，业务代码直接拿 data；`code !== 0` 已自动 `ElMessage.error` 并 reject，调用方只需 `.then` 处理成功分支或 `try/catch`。

### 2.2 View 页面结构（user-web）

每个页面 `src/views/{ModuleName}.vue`，使用 `<script setup>` + Element Plus：

```vue
<template>
  <div class="xxx-page">
    <el-card shadow="never">
      <!-- 业务内容 -->
    </el-card>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { detectHighFreqBorrow, getHighFreqBorrowHistory } from '@/api/consumption'

const loading = ref(false)
const list = ref([])

async function loadList() {
  loading.value = true
  try {
    list.value = await getHighFreqBorrowHistory()
  } finally {
    loading.value = false
  }
}

onMounted(loadList)
</script>

<style scoped>
.xxx-page { max-width: 1200px; margin: 0 auto; }
</style>
```

### 2.3 Router 配置（user-web）

在 `src/router/index.js` 的 `MainLayout` children 中添加：

```js
{
  path: 'consumption',
  name: 'Consumption',
  component: () => import('@/views/Consumption.vue'),
  meta: { title: '消费治理', requiresAuth: true }
}
```

### 2.4 菜单配置（user-web）

在 `src/layout/MainLayout.vue` 的 `<el-menu>` 中添加 `<el-menu-item index="/consumption">`，或放入用户下拉菜单。

### 2.5 admin-web 联调约定

- API 文件：`src/api/{模块名}.ts`（TypeScript），`import request from './request'`
- View：`src/views/{模块名}/index.vue`
- Router：`src/router/index.ts` 的 layout children 中添加
- 权限：管理端接口需 ADMIN/banker 角色，路由 `meta.requiresAuth: true`

---

## 三、user-web 各模块联调步骤

### 3.1 保函索赔闭环 G-6（P0）

**后端接口**（`/api/v1/guarantee/claims`）：

| Method | Path | 说明 |
|---|---|---|
| POST | / | 房东发起索赔 |
| PUT | /{id}/defense | 租客提交申辩 |
| PUT | /{id}/review | banker 人工复核（APPROVED/REJECTED） |
| GET | / | 分页查询索赔列表（房东/租客按身份过滤） |
| GET | /{id} | 索赔详情 |
| GET | /manual-review-queue | banker 人工复核队列 |
| GET | /status-flow | 索赔状态流转说明 |

**联调步骤**：

1. **API 文件**：在 `src/api/guarantee.js` 追加（复用现有文件，不新建）：
   ```js
   /** G-6 发起索赔 */
   export function submitClaim(data) {
     return request.post('/v1/guarantee/claims', data)
   }
   /** 租客申辩 */
   export function submitClaimDefense(id, data) {
     return request.put(`/v1/guarantee/claims/${id}/defense`, data)
   }
   /** banker 人工复核 */
   export function reviewClaim(id, data) {
     return request.put(`/v1/guarantee/claims/${id}/review`, data)
   }
   /** 索赔列表 */
   export function getClaimPage(params) {
     return request.get('/v1/guarantee/claims', { params })
   }
   /** 索赔详情 */
   export function getClaimDetail(id) {
     return request.get(`/v1/guarantee/claims/${id}`)
   }
   /** 人工复核队列 */
   export function getClaimReviewQueue(params) {
     return request.get('/v1/guarantee/claims/manual-review-queue', { params })
   }
   /** 索赔状态流转 */
   export function getClaimStatusFlow() {
     return request.get('/v1/guarantee/claims/status-flow')
   }
   ```

2. **View 改造**：改造 `src/views/Guarantee.vue`，在保函详情区增加"索赔"入口 Tab：
   - 房东视角：显示"发起索赔"按钮 → 弹窗填写 claimAmount / claimReason / evidenceFiles
   - 租客视角：DEFENSE_PERIOD 状态显示"提交申辩"按钮
   - 状态流转展示：调用 `/status-flow` 渲染 7 态时间线

3. **验证**：
   ```bash
   # 房东发起索赔
   curl -X POST http://localhost:8080/api/v1/guarantee/claims \
     -H "Authorization: Bearer <landlordJWT>" -H "Content-Type: application/json" \
     -d '{"guaranteeId":1,"claimAmount":5000,"claimReason":"墙面损坏","evidenceFiles":["url1"]}'
   ```

### 3.2 保函电子签约（G-2 增强）（P0）

**后端接口**：`PUT /api/v1/guarantee/{id}/landlord-confirm`（已有 guarantee.js 的 `landlordConfirm`）

**联调步骤**：改造 `Guarantee.vue` 的房东确认流程：
1. 确认前展示《保函申请协议》文本
2. 提供两种签署方式：Canvas 手写签名 / 点击确认按钮
3. 调用 `landlordConfirm(id)` 时携带 `signContent`（base64 或 "CLICK_CONFIRM"）
4. 展示签署时间

**注意**：需确认后端 `LandlordSignDTO` 是否支持 signContent 字段（步骤1已实现）。

### 3.3 贷款增强（P0）

**后端接口**（`/api/v1/loan`，追加到现有 `loan.js`）：

| Method | Path | 说明 |
|---|---|---|
| POST | /withdraw | A类循环贷提款 |
| POST | /repay | 还款 |
| GET | /credit-txns | 提还款流水 |
| GET | /observation | B转A观察期状态 |
| POST | /observation/advance | 加速观察（模拟月份推进） |
| GET | /merchants/by-status | 按状态查商户（banker） |
| PUT | /merchants/{id}/audit | 商户白名单审核（banker） |

**联调步骤**：
1. 在 `src/api/loan.js` 追加上述 7 个函数
2. 改造 `src/views/Loan.vue`，新增三个区域：
   - **A类循环贷**：提款/还款按钮 + 流水列表（`/credit-txns`）
   - **观察期**：展示 observationStatus / score / months，"加速观察"按钮
   - **商户管理**（仅 banker）：商户列表 + 审核弹窗

### 3.4 政策库匹配与推送（P1）

**后端接口**（`/api/v1/policy`，新建 `src/api/policy.js`）：

| Method | Path | 说明 |
|---|---|---|
| GET | /match | 按当前用户资质匹配政策 |
| GET | /matched | 已推送的政策列表 |
| GET | /{id} | 政策详情 |
| POST | /push | 推送政策到消息中心 |

**联调步骤**：
1. 新建 `src/api/policy.js`
2. 新建 `src/views/Policy.vue`：政策卡片列表（人才安居 4 + 创业贴息 4），按人群标签匹配高亮，点击"一键申报"调用 `/push`
3. Router + Menu 添加 `/policy`

### 3.5 保险代销匹配（P1）

**后端接口**（`/api/v1/insurance`，新建 `src/api/insurance.js`）：

| Method | Path | 说明 |
|---|---|---|
| GET | /match | 按经营场景匹配产品 |
| GET | /matched | 已匹配产品列表 |
| GET | /{id} | 产品详情 |
| POST | /{id}/apply-demo | 投保演示（返回模拟链接，不承保） |

**联调步骤**：
1. 新建 `src/api/insurance.js`
2. 新建 `src/views/Insurance.vue`：产品卡片（履约保证/知识产权/财产综合），展示 productElements + "工行仅代销、不承保" 提示，"跳转投保"调用 `/apply-demo`
3. Router + Menu 添加 `/insurance`

### 3.6 现金流风险预警（P1）

**后端接口**（`/api/v1/operation/cashflow-warning`，新建 `src/api/operation.js`）：

| Method | Path | 说明 |
|---|---|---|
| POST | /detect | 触发现金流预警检测 |
| GET | /history | 预警历史 |

**联调步骤**：
1. 新建 `src/api/operation.js`（统一放 cashflow-warning + finance-content）
2. 在 `Bookkeeping.vue` 增加"现金流预警"卡片：调用 `/detect` 按钮 + 历史列表
3. 预警命中时同步站内信（后端已写 sys_message）

### 3.7 财税科普与个转企引导（P2）

**后端接口**（`/api/v1/operation/finance`，追加到 `operation.js`）：

| Method | Path | 说明 |
|---|---|---|
| GET | /content | 财税科普内容列表 |
| GET | /content/{id} | 内容详情 |
| GET | /individual-to-company/checklist | 个转企自查条件 |
| POST | /individual-to-company/self-check | 个转企自查提交 |

**联调步骤**：
1. 追加 4 个函数到 `operation.js`
2. 在 `Bookkeeping.vue` 增加"财税科普"Tab + "个转企自查"Tab

### 3.8 高频借贷预警（P0）

**后端接口**（`/api/v1/consumption/high-freq-borrow`）：

| Method | Path | 说明 |
|---|---|---|
| POST | /detect | 触发检测，命中落 HIGH_FREQ_BORROW + 站内信 |
| GET | /history | 预警历史 |

**联调步骤**：
1. 新建 `src/api/consumption.js`（统一放 consumption 模块所有接口）
2. 新建 `src/views/Consumption.vue` 或在 Safety 页新增"负债预警"卡片
3. 展示：近30天申请数 / 未结清贷款数 / 月度负债率 / 触发原因 / 建议

### 3.9 征信监测（P0）

**后端接口**（`/api/v1/consumption/credit-monitor`）：

| Method | Path | 说明 |
|---|---|---|
| POST | /soft-query | 软查询模拟（source=SIMULATED），异常转 CREDIT_ABNORMAL |
| GET | /latest | 最近一次报告 |
| GET | /reports | 历史报告列表 |
| GET | /warnings | CREDIT_ABNORMAL 预警列表 |

**联调步骤**：
1. 追加 4 个函数到 `consumption.js`
2. 在 `Consumption.vue` 增加"征信监测"卡片：授权按钮 → 软查询 → 展示信用分/等级/贷款笔数/逾期笔数
3. 标注"模拟软查询，不产生硬查询"

### 3.10 三层消费引导（P1）

**后端接口**（`/api/v1/consumption/guide`）：

| Method | Path | 说明 |
|---|---|---|
| POST | /after-txn/{transactionId} | 第一层：交易后即时推送 |
| GET | /monthly-bill | 第二层：月度账单分析 |
| GET | /pay-before-reminder | 第三层：支付前提醒演示 |

**联调步骤**：
1. 追加 3 个函数到 `consumption.js`
2. 在 `Consumption.vue` 增加三层引导区域：
   - 第一层：交易列表中大额交易标记"已推送提醒"
   - 第二层：月度账单卡片（收入/支出/净现金流/储蓄率/负债预警）
   - 第三层：支付前提醒演示表单（金额 + 商户 → 提示），标注"仅工行自有支付场景"

### 3.11 风险测评（P0）

**后端接口**（`/api/v1/consumption/risk-assessment`）：

| Method | Path | 说明 |
|---|---|---|
| GET | /questionnaire | 获取 10 题问卷 |
| POST | /submit | 提交测评（10题答案 → 总分 → 风险等级） |
| GET | /latest | 最新测评结果（含过期标记） |
| GET | /history | 历史测评列表 |

**联调步骤**：
1. 追加 4 个函数到 `consumption.js`
2. 新建 `src/views/RiskAssessment.vue`：
   - 未测评：展示 10 题问卷（单选 A/B/C/D），提交后展示等级结果
   - 已测评：展示当前等级 + 有效期 + 重新测评入口
3. 评分映射：≤18 保守型 / 19-30 稳健型 / 31-40 平衡型

### 3.12 理财匹配（P0）

**后端接口**（`/api/v1/consumption/finance-product`）：

| Method | Path | 说明 |
|---|---|---|
| GET | / | 全部产品列表（不强制测评） |
| GET | /recommend | 按用户等级推荐（未测评返回 1001） |
| GET | /recommend/by-level | 按指定等级推荐 |
| GET | /{id} | 产品详情 |
| POST | /{id}/apply-demo | 购买演示（强制测评 + 适配校验，不扣款） |

**联调步骤**：
1. 追加 5 个函数到 `consumption.js`
2. 新建 `src/views/FinanceProduct.vue`：
   - 入口先调 `/latest` 检查测评，未测评跳转 RiskAssessment 页
   - 产品卡片列表（心愿储蓄/现金管理/短债/基金定投/积存金），仅 R1/R2
   - 产品详情弹窗：展示 riskDisclosure + "理财非存款、产品有风险、工行仅代销"
   - "购买演示"按钮：调用 `/apply-demo`，返回模拟链接 + 风险提示，**无扣款**

### 3.13 青年成长信用画像（P1）

**后端接口**（`/api/v1/profile`，新建 `src/api/profile.js`）：

| Method | Path | 说明 |
|---|---|---|
| GET | / | 查询画像（三维评分 + 成长轨迹） |
| POST | /linkage/apply | 联动提额演示 |

**联调步骤**：
1. 新建 `src/api/profile.js`
2. 改造 `src/views/Profile.vue`：增加"信用画像"Tab，展示稳定性/经营力/资金健康度三维雷达图 + "联动提额"按钮

### 3.14 AI 对话引擎（P2）

**后端接口**（`/api/v1/chat`，新建 `src/api/chat.js`）：

| Method | Path | 说明 |
|---|---|---|
| GET | /engine-status | 引擎状态（本地规则/LLM） |
| POST | /messages | 发送消息 |
| GET | /history | 历史消息 |

**联调步骤**：
1. 新建 `src/api/chat.js`
2. 新建 `src/views/Chat.vue`：对话界面，左侧消息流 + 底部输入框

### 3.15 金融安全增强（P1）

**后端接口**（`/api/v1/safety/*`，追加到现有 `safety.js`）：

| 控制器 | Method | Path | 说明 |
|---|---|---|---|
| CreditReportInterpret | GET | /safety/credit-report/reports | 模拟征信报告列表 |
| CreditReportInterpret | GET | /safety/credit-report/{reportId}/interpret | 报告智能解读 |
| CreditReportInterpret | POST | /safety/credit-report/load-demo | 加载演示报告 |
| OverdueRisk | GET | /safety/overdue-risk/calendar | 还款日历 |
| OverdueRisk | POST | /safety/overdue-risk/predict | 逾期风险预判 |
| OverdueRisk | GET | /safety/overdue-risk/warnings | 逾期预警列表 |
| CreditHealth | GET | /safety/credit-health | 征信健康分 + 改善清单 |
| CreditHealth | POST | /safety/credit-health/simulate-fix | 模拟修复路径 |
| ScenarioTeaching | GET | /safety/scenario/list | 情景教学列表 |
| ScenarioTeaching | GET | /safety/scenario/{id} | 情景详情 |
| ScenarioTeaching | POST | /safety/scenario/{id}/submit | 提交情景作答 |

**联调步骤**：
1. 在 `safety.js` 追加上述 11 个函数
2. 改造 `src/views/Safety.vue`，增加 Tab：
   - 征信解读：选择报告 → 展示结构化解读
   - 逾期风险：还款日历 + 预判按钮 + 预警列表
   - 征信健康：健康分 + 改善清单 + "模拟修复"按钮（标注不产生真实影响）
   - 情景教学：互动问答（刷单/冒充公检法/征信洗白）

### 3.16 多渠道流水聚合 + 防刷单（P2）

**后端接口**（`/api/v1/cashflow/*`，新建 `src/api/cashflow.js`）：

| 控制器 | Method | Path | 说明 |
|---|---|---|---|
| CashflowAggregate | GET | /cashflow/aggregate/auth | 已授权渠道 |
| CashflowAggregate | POST | /cashflow/aggregate/auth | 授权渠道 |
| CashflowAggregate | DELETE | /cashflow/aggregate/auth/{channelCode} | 解绑渠道 |
| CashflowAggregate | GET | /cashflow/aggregate/report | 聚合流水报表 |
| AntiBrush | POST | /cashflow/anti-brush/detect | 防刷单检测 |
| AntiBrush | GET | /cashflow/anti-brush/overview | 防刷单概览 |

**联调步骤**：
1. 新建 `src/api/cashflow.js`
2. 在 `Bookkeeping.vue` 增加"流水聚合"Tab：渠道授权勾选 → 聚合报表；"防刷单"Tab：检测按钮 + 风险分

---

## 四、admin-web 各模块联调步骤

### 4.1 数据看板动态化（P1）

**现状**：`dashboard/index.vue` 为静态占位。

**联调步骤**：
1. 新建 `src/api/dashboard.ts`，调用管理端聚合接口（如 `/admin/v1/user/page` 统计用户数、`/v1/guarantee/page` 统计保函数等）
2. 改造 `dashboard/index.vue`：用户总数 / 保函数 / 贷款数 / 预警数 四个统计卡片 + 趋势图

### 4.2 保函管理 + 人工复核（P0）

**后端接口**：
- `GET /v1/guarantee/page` — 保函列表
- `GET /v1/guarantee/{id}` — 保函详情
- `PUT /v1/guarantee/{id}/manual-review` — AI 复审存疑的保函人工审核
- `GET /v1/guarantee/claims/manual-review-queue` — 索赔人工复核队列
- `PUT /v1/guarantee/claims/{id}/review` — 索赔人工复核

**联调步骤**：
1. 新建 `src/api/guarantee.ts`
2. 新建 `src/views/guarantee/index.vue`：保函列表表格（状态筛选）
3. 新建 `src/views/guarantee/review.vue`：AI 复审存疑队列 → 通过/拒绝
4. 新建 `src/views/guarantee/claim.vue`：索赔复核队列 → 赔付/拒绝
5. Router 添加 `/guarantee`、`/guarantee/review`、`/guarantee/claim`

### 4.3 贷款管理 + 商户审核（P1）

**后端接口**：
- `GET /v1/loan/applications` — 贷款申请列表
- `GET /v1/loan/merchants/by-status` — 商户按状态查询
- `PUT /v1/loan/merchants/{id}/audit` — 商户白名单审核

**联调步骤**：
1. 新建 `src/api/loan.ts`
2. 新建 `src/views/loan/index.vue`：贷款申请列表
3. 新建 `src/views/loan/merchant.vue`：商户列表 + 审核弹窗（VERIFIED/REJECTED）

### 4.4 风险预警管理（P1）

**后端接口**：复用 `GET /v1/operation/cashflow-warning/history`、`GET /v1/consumption/high-freq-borrow/history`、`GET /v1/consumption/credit-monitor/warnings`（管理端需扩展为全量查询）

**联调步骤**：
1. 新建 `src/api/risk.ts`
2. 新建 `src/views/risk/index.vue`：预警聚合表格（类型/等级/用户/时间/状态），支持按 warningType 筛选

### 4.5 保险/政策/理财产品管理（P2）

**后端接口**：复用 `/v1/insurance`、`/v1/policy`、`/v1/consumption/finance-product` 的 GET 列表接口（管理端扩展增删改）

**联调步骤**：
1. 新建 `src/api/product.ts`
2. 新建 `src/views/product/insurance.vue`、`policy.vue`、`finance.vue`：产品列表 + 上下架

---

## 五、统一验证方式

### 5.1 环境准备

```bash
# 1. 初始化数据库
cd backend/sql
mysql -u root -p < init-database.sql

# 2. 启动后端（端口 8080）
cd backend
./mvnw spring-boot:run

# 3. 启动 user-web（端口 5173）
cd user-web
npm install
npm run dev

# 4. 启动 admin-web（端口 5174）
cd admin-web
npm install
npm run dev
```

### 5.2 演示账号

| 账号 | 密码 | 角色 | 用途 |
|---|---|---|---|
| testuser | 123456 | 青年用户 | user-web 测试 |
| entrepreneur | 123456 | 创业者 | 贷款/经营测试 |
| landlord01 | 123456 | 房东 | 保函/索赔测试 |
| banker01 | 123456 | 银行运营 | admin-web 测试 |
| admin | 123456 | 系统管理员 | admin-web 测试 |

### 5.3 联调检查清单

每个模块联调完成后，按以下清单验证：

- [ ] API 文件函数名与后端路径一一对应
- [ ] View 页面调用 API 并正确渲染数据
- [ ] 空数据有兜底（`el-empty` 或提示文案）
- [ ] 错误状态有提示（`ElMessage.error`，由 request 拦截器自动处理）
- [ ] 演示数据标注"模拟"（金融能力相关）
- [ ] 合规文案展示（理财非存款 / 工行仅代销 / 不产生硬查询 等）
- [ ] Router 与 Menu 配置正确，页面可跳转
- [ ] `npm run build` 无报错
- [ ] 浏览器控制台无报错

### 5.4 关键 curl 验证

```bash
# 获取 token
TOKEN=$(curl -s -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"testuser","password":"123456"}' | jq -r '.data.token')

# 1. 风险测评问卷
curl -s http://localhost:8080/api/v1/consumption/risk-assessment/questionnaire \
  -H "Authorization: Bearer $TOKEN" | jq '.data | length'  # 预期 10

# 2. 提交测评（保守型）
curl -s -X POST http://localhost:8080/api/v1/consumption/risk-assessment/submit \
  -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json" \
  -d '{"answers":{"1":"A","2":"A","3":"A","4":"A","5":"A","6":"A","7":"A","8":"A","9":"A","10":"A"}}' \
  | jq '.data.riskLevel'  # 预期 CONSERVATIVE

# 3. 理财推荐（测评通过后）
curl -s http://localhost:8080/api/v1/consumption/finance-product/recommend \
  -H "Authorization: Bearer $TOKEN" | jq '.data | length'

# 4. 高频借贷检测
curl -s -X POST http://localhost:8080/api/v1/consumption/high-freq-borrow/detect \
  -H "Authorization: Bearer $TOKEN" | jq '.data.warningTriggered'

# 5. 征信软查询
curl -s -X POST http://localhost:8080/api/v1/consumption/credit-monitor/soft-query \
  -H "Authorization: Bearer $TOKEN" | jq '.data.source'  # 预期 SIMULATED

# 6. 政策匹配
curl -s http://localhost:8080/api/v1/policy/match \
  -H "Authorization: Bearer $TOKEN" | jq '.data | length'

# 7. 保险匹配
curl -s http://localhost:8080/api/v1/insurance/match \
  -H "Authorization: Bearer $TOKEN" | jq '.data | length'

# 8. 现金流预警检测
curl -s -X POST http://localhost:8080/api/v1/operation/cashflow-warning/detect \
  -H "Authorization: Bearer $TOKEN" | jq '.data.warningTriggered'

# 9. 月度账单分析
curl -s "http://localhost:8080/api/v1/consumption/guide/monthly-bill?period=2026-09" \
  -H "Authorization: Bearer $TOKEN" | jq '.data.totalExpense'

# 10. 青年画像
curl -s http://localhost:8080/api/v1/profile \
  -H "Authorization: Bearer $TOKEN" | jq '.data.stabilityScore'
```

---

## 六、联调优先级与建议顺序

按演示主链路依赖关系，建议联调顺序：

1. **P0 核心链路**（答辩必走）：
   - 保函索赔 G-6 + 电子签约 → 完整安居闭环
   - 贷款增强（提款/还款/观察期）→ 完整创业闭环
   - 高频借贷预警 + 征信监测 → 消费治理核心
   - 风险测评 + 理财匹配 → 理财合规闭环

2. **P1 增强展示**（提升答辩丰富度）：
   - 政策库 + 保险代销 → 政策与金融服务
   - 现金流预警 + 财税科普 → 经营赋能
   - 三层消费引导 → 消费治理完整度
   - 金融安全增强（征信解读/逾期/健康/情景）→ 安全模块完整度
   - admin-web 保函管理 + 人工复核 → 管理端闭环

3. **P2 锦上添花**（时间充裕时）：
   - 青年画像 + AI 对话 → 智能中台
   - 流水聚合 + 防刷单 → 经营赋能进阶
   - admin-web 贷款/风险/产品管理 → 管理端完整度

---

## 七、注意事项

1. **不改动已联调模块**：guarantee.js / loan.js 等已有文件采用"追加函数"方式，不重写已有函数
2. **合规文案必须展示**：理财非存款、工行仅代销、模拟软查询、不扣款等
3. **无自动扣款逻辑**：所有"购买/投保/申请"接口均为演示，返回模拟链接
4. **7 天去重**：预警类接口 7 天内同用户不重复触发，前端展示历史即可
5. **测评前置**：理财推荐前必须先完成风险测评，未测评返回 1001 由前端拦截跳转
6. **管理端权限**：admin-web 接口需 ADMIN/banker 角色，banker01 账号测试
