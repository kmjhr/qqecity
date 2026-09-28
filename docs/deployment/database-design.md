# 青启e城 · 数据库 ER 关系说明

> 共 32 张表，分 7 大模块 + 7 张补充计划新增表。本文档用文字描述各表之间的实体关系与核心外键关联。
> 表命名：`sys_*` 系统公共表，`biz_*` 业务模块表。
>
> **初始化方式**：执行 `backend/sql/init-database.sql`（内部按序 SOURCE schema.sql + data.sql）
> 或分步执行：`mysql -u root -p < schema.sql && mysql -u root -p < data.sql`

---

## 一、模块总览

| 模块 | 表数量 | 表名 |
| --- | --- | --- |
| 用户与权限 | 4 | sys_user, sys_role, sys_user_role, sys_login_log |
| 消息中心 | 1 | sys_message |
| 安居金融风控 | 6 | biz_landlord, biz_house, biz_rental_contract, biz_guarantee_application, biz_guarantee, biz_guarantee_claim |
| 青创e贷 | 4 | biz_merchant, biz_loan_application, biz_credit_limit, biz_entrust_payment |
| 经营赋能 | 2 | biz_bookkeeping_record, biz_cashflow_report |
| 预算消费 | 4 | biz_budget_category, biz_budget_setting, biz_transaction, biz_saving_goal |
| 金融安全 | 4 | biz_anti_fraud_content, biz_fraud_detection_log, biz_credit_report, biz_risk_warning |
| 补充计划新增表 | 7 | biz_policy, biz_credit_txn, biz_insurance_product, biz_finance_product, biz_risk_assessment, biz_scenario_practice, biz_scenario_round |

---

## 补充计划新增表说明

### biz_policy 政策库

- **用途**：人才安居政策 + 创业贴息政策智能匹配
- **核心字段**：`policy_type`（HOUSING/ENTREPRENEUR）、`target_crowd`（逗号分隔人群标签）、`max_amount`、`subsidy_rate`、`conditions`、`apply_url`
- **匹配规则**：按 `sys_user.user_type` 匹配 `biz_policy.target_crowd`
- **演示数据**：8 条（人才安居 4 + 创业贴息 4）
- **关联接口**：`GET /api/v1/policy/match`、`POST /api/v1/policy/push`

### biz_credit_txn 循环贷交易流水

- **用途**：A 类 5 万循环贷的提款/还款流水记录
- **核心字段**：`txn_type`（WITHDRAW/REPAY）、`principal_amount`、`interest_amount`、`borrow_days`、`balance_after`、`target_loan_no`（仅"指定结清某笔"的 REPAY 流水记录目标借款编号；FIFO 普通还款为空）
- **关联**：`credit_limit_id` → `biz_credit_limit.id`
- **计息规则**：按笔计息（利随本清）：每笔提款独立起息，`interest = 剩余本金 × 3.85% × days / 365`；还款按先进先出冲抵本金（指定结清的 REPAY 通过 `target_loan_no` 精确冲抵目标借款，不参与 FIFO、不漂移）
- **关联接口**：`POST /api/v1/loan/withdraw`、`POST /api/v1/loan/repay-order`、`GET /api/v1/loan/repay-preview`、`GET /api/v1/loan/credit-txns`

### biz_insurance_product 保险代销产品表（模拟）

- **用途**：履约保证保险 / 知识产权保险 / 财产综合险 按经营场景推荐
- **核心字段**：`insurance_type`（PERFORMANCE_BOND/IP_PATENT/IP_INFRINGEMENT/PROPERTY）、`scene`（CONTRACT/IP/PROPERTY/EMPLOYER）、`target_crowd`、`premium_rate`、`coverage_amount`、`insurer`、`product_elements`、`apply_url`
- **匹配规则**：按 `sys_user.user_type` 匹配 `biz_insurance_product.target_crowd`
- **演示数据**：4 条（履约保证 1 + 知识产权 2 + 财产综合 1）
- **合规口径**：工行仅代销、不承保；演示用，不构成真实投保邀约
- **关联接口**：`GET /api/v1/insurance/match`、`POST /api/v1/insurance/{id}/apply-demo`

### biz_finance_product 理财产品表（模拟）

- **用途**：低风险理财产品池（仅 R1/R2），配合风险测评结果推荐
- **核心字段**：`product_type`（SAVING_GOAL/CASH_MANAGEMENT/SHORT_BOND/FUND_DCA/GOLD_ACCUM）、`risk_level`（R1/R2）、`expected_return`、`min_amount`、`period`、`target_risk_level`（CONSERVATIVE/STEADY/BALANCED 逗号分隔）、`risk_disclosure`、`apply_url`
- **匹配规则**：按 `biz_risk_assessment.risk_level` 匹配 `biz_finance_product.target_risk_level`
- **演示数据**：5 条（心愿储蓄/现金管理/短债/基金定投/积存金）
- **合规口径**：理财非存款、产品有风险、工行仅代销；演示用，不构成投资建议、不发起真实购买
- **关联接口**：`GET /api/v1/consumption/finance-product/recommend`、`POST /api/v1/consumption/finance-product/{id}/apply-demo`

### biz_risk_assessment 风险测评表

- **用途**：用户风险测评问卷作答记录（10 题 → 风险等级）
- **核心字段**：`assess_no`（测评编号）、`answers`（JSON：题号→选项）、`total_score`（10-40）、`risk_level`（CONSERVATIVE/STEADY/BALANCED）、`risk_level_name`、`valid_until`（1 年有效期）、`is_latest`（同一用户仅 1 条为 1）
- **评分规则**：10 题 × 1-4 分 = 10-40 分；≤18 CONSERVATIVE，19-30 STEADY，31-40 BALANCED
- **使用约束**：未完成测评不允许进入理财推荐；测评过期需重新测评
- **关联接口**：`GET /api/v1/consumption/risk-assessment/questionnaire`、`POST /api/v1/consumption/risk-assessment/submit`、`GET /api/v1/consumption/risk-assessment/latest`


### biz_scenario_practice 反诈对话演练记录表（L3 对话式演练，模拟）

- **用途**：记录「对话式反诈演练」（AI 扮演诈骗分子，用户自由发言对抗）的完整演练记录；结果联动画像「反诈指数」与消息中心站内信
- **核心字段**：`practice_no`（演练编号，唯一）、`user_id`、`scenario_id`（→ biz_anti_fraud_content.id，content_type=SCENARIO_DIALOG）、`round_count`、`result`（SAFE识破/LURED被诱骗/TIMEOUT超时/FINISHED主动结束）、`risk_score`（0-100 越低越安全）、`result_desc`（AI 复盘摘要）
- **判定规则**：识破词库/危险词库双层打分；安全分≥70 识破、≤40 连续 2 回合被诱骗、≥10 回合超时；LLM 增强可配（缺 Key 自动降级本地剧本）
- **关联接口**：`POST /api/v1/safety/scenario/{id}/practice/start`、`POST /api/v1/safety/scenario/practice/{practiceNo}/turn`、`POST /api/v1/safety/scenario/practice/{practiceNo}/finish`、`GET /api/v1/safety/scenario/practice/history`、`GET /api/v1/safety/scenario/practice/{practiceNo}`

### biz_scenario_round 反诈对话演练回合明细表（L3 对话式演练，模拟）

- **用途**：演练每一回合的发言明细（诈骗方话术 / 用户应对 + 该回合安全分），用于回放复盘
- **核心字段**：`practice_id`（→ biz_scenario_practice.id）、`round_no`、`speaker`（FRAUD/USER）、`content`（发言内容）、`safe_score`（用户回合安全分 0-100）、`hit_words`（命中的词库标签，逗号分隔）
- **关联接口**：`GET /api/v1/safety/scenario/practice/{practiceNo}`（回放时返回主表+回合明细）
---

## 二、核心 ER 关系详述

### 2.1 用户与权限模块

```
sys_user  ───1:N───  sys_user_role  ───N:1───  sys_role
   │
   └───1:N───  sys_login_log
```

- **sys_user ↔ sys_role**：多对多关系，通过 `sys_user_role` 中间表关联
  - 一个用户可以有多个角色（如既是青年用户又有房东身份）
  - 一个角色可分配给多个用户
  - 关联字段：`sys_user_role.user_id` → `sys_user.id`，`sys_user_role.role_id` → `sys_role.id`
- **sys_user → sys_login_log**：一对多
  - 一个用户有多条登录日志
  - 关联字段：`sys_login_log.user_id` → `sys_user.id`

### 2.2 消息中心模块

```
sys_user  ───1:N───  sys_message
```

- **sys_user → sys_message**：一对多
  - 一个用户可接收多条消息
  - 关联字段：`sys_message.user_id` → `sys_user.id`
  - 消息类型：SYSTEM（系统通知）、BUDGET（预算提醒）、BUSINESS（业务通知）、SAFETY（安全预警）
  - 可选业务关联：`biz_type` + `biz_id` 可关联到具体业务表

### 2.3 安居金融风控模块（核心链路：申请 → 保函 → 索赔）

```
sys_user（租客） ─┐
                  ├─── biz_rental_contract ─── biz_house ─── biz_landlord ─── sys_user（房东）
sys_user（房东） ─┘        │
                           │
                           ├─── biz_guarantee_application ─── biz_guarantee ─── biz_guarantee_claim
```

**关系说明：**

1. **biz_landlord → sys_user**：一对一
   - 房东也是系统用户，`biz_landlord.user_id` → `sys_user.id`
   - 房东有独立的认证信息（身份证、银行卡等）

2. **biz_landlord → biz_house**：一对多
   - 一个房东可以有多处房屋
   - 关联字段：`biz_house.landlord_id` → `biz_landlord.id`

3. **biz_rental_contract 多方关联**：
   - `contract.house_id` → `biz_house.id`（租赁房屋）
   - `contract.landlord_id` → `biz_landlord.id`（出租方）
   - `contract.tenant_id` → `sys_user.id`（承租方，青年用户）

4. **biz_guarantee_application（保函申请）**：整条链路的入口
   - `application.tenant_id` → `sys_user.id`（申请人）
   - `application.house_id` → `biz_house.id`（对应房屋）
   - `application.contract_id` → `biz_rental_contract.id`（对应租赁合同）
   - `application.landlord_id` → `biz_landlord.id`（房东）
   - 状态流转：SUBMITTED → LANDLORD_CONFIRM → AI_REVIEW → MANUAL_REVIEW → PENDING_PAY → APPROVED / REJECTED
   - **电子签约字段**（补充计划）：`sign_content`（Canvas base64 或 CLICK_CONFIRM）、`sign_time`
   - **AI 复审增强**（补充计划）：置信度 < 60% 时停在 MANUAL_REVIEW，需 banker 通过 `PUT /v1/guarantee/{id}/manual-review` 放行

5. **biz_guarantee（保函主表）**：申请通过后生成正式保函
   - `guarantee.application_id` → `biz_guarantee_application.id`（一对一，申请通过后生成）
   - `guarantee.tenant_id` → `sys_user.id`
   - `guarantee.landlord_id` → `biz_landlord.id`
   - `guarantee.house_id` → `biz_house.id`
   - 状态：ACTIVE（有效）、EXPIRED（到期）、CLAIMED（已索赔）、TERMINATED（终止）

6. **biz_guarantee_claim（索赔表）**：保函有效期内房东可发起索赔
   - `claim.guarantee_id` → `biz_guarantee.id`（一对多，一张保函可多次索赔？实际一般一次）
   - `claim.claimant_id` → `biz_landlord.id`（索赔人=房东）
   - `claim.tenant_id` → `sys_user.id`（被索赔人=租客）
   - 状态流转：SUBMITTED → AI_REVIEW → MANUAL_REVIEW → DEFENSE_PERIOD → APPROVED / REJECTED → CLOSED

### 2.4 青创e贷模块（核心链路：申请 → 额度 → 受托支付）

```
sys_user  ───1:N───  biz_loan_application  ───1:1───  biz_credit_limit
    │                    │
    │                    └───1:N───  biz_entrust_payment  ───N:1───  biz_merchant
    └───1:N───────────────────────────────────────────────────┘
```

**关系说明：**

1. **biz_loan_application（贷款申请）**：
   - `application.user_id` → `sys_user.id`（申请人）
   - 贷款类型：A_TYPE（5万循环额度）、B_TYPE（小额定向）
   - 状态：PRE_CHECK → PENDING_APPROVAL → APPROVED / REJECTED

2. **biz_credit_limit（授信额度）**：
   - `limit.user_id` → `sys_user.id`
   - 一个用户可有多类授信（A类+B类），通过 `credit_type` 区分
   - 唯一约束：`(user_id, credit_type)` 联合唯一
   - 额度字段：total_limit / used_limit / available_limit
   - **B 转 A 观察期字段**（补充计划）：`observation_status`（OBSERVING/PROMOTED/EXITED）、`observation_start`、`observation_months`、`observation_score`
   - **A 类循环贷**（补充计划）：年化 3.85%，随借随还，提还款流水记录在 `biz_credit_txn`

3. **biz_entrust_payment（受托支付）**：
   - `payment.user_id` → `sys_user.id`（借款人）
   - `payment.loan_application_id` → `biz_loan_application.id`（关联贷款申请）
   - `payment.merchant_id` → `biz_merchant.id`（收款商户）
   - 核心规则：资金 100% 定向打给商户，不经过借款人个人账户

4. **biz_merchant（商户表）**：
   - 受托支付的收款方，系统预置商户列表
   - 商户类型：MATERIAL（物料）、STALL（摊位）、PROMOTION（推广）、OTHER

### 2.5 经营赋能模块

```
sys_user  ───1:N───  biz_bookkeeping_record
   │
   ├───1:N───  biz_cashflow_report
   │
   ├───1:N───  biz_risk_warning (warning_type=CASHFLOW_WARNING，现金流风险预警)
   │
   └───1:N───  biz_insurance_product（保险代销产品，无强用户关联，按人群标签匹配）
```

**关系说明：**

1. **biz_bookkeeping_record（记账记录）**：
   - `record.user_id` → `sys_user.id`
   - 类型：INCOME（收入）、EXPENSE（支出）
   - 来源：AUTO（自动识别）、MANUAL（手动录入）、IMPORT（导入）
   - `is_confirmed`：模糊交易待确认标记（现金流预警中作为"应收未收"识别依据）

2. **biz_cashflow_report（现金流报表）**：
   - `report.user_id` → `sys_user.id`
   - 按月生成：`(user_id, report_period)` 联合唯一
   - 字段：总收入/总支出/净现金流/利润/利润率
   - 预警等级：NORMAL / WARNING / CRITICAL

3. **biz_risk_warning（现金流预警，补充计划步骤4）**：
   - `warning.user_id` → `sys_user.id`
   - 触发规则：近 3 月结余率 < 10% **且** 存在 > 7 天应收未收（INCOME + `isConfirmed=0`）
   - `warning_type=CASHFLOW_WARNING`，`warning_level=HIGH`
   - 7 天内同用户不重复触发（去重）
   - 同步发站内信 `sys_message`（type=SAFETY, bizType=CASHFLOW_WARNING）

4. **biz_insurance_product（保险代销产品，补充计划步骤4）**：
   - 与 `sys_user` 无强外键关联，通过 `target_crowd` 标签与 `sys_user.user_type` 软匹配
   - 险种：`PERFORMANCE_BOND`（履约保证）/ `IP_PATENT`（专利执行）/ `IP_INFRINGEMENT`（侵权责任）/ `PROPERTY`（财产综合）
   - 经营场景：`CONTRACT`（合同履约）/ `IP`（知识产权）/ `PROPERTY`（财产）/ `EMPLOYER`（雇主）
   - 合规口径：工行仅代销、不承保；`apply_url` 为模拟跳转链接
   - 关联接口：`GET /api/v1/insurance/match`、`POST /api/v1/insurance/{id}/apply-demo`

5. **财税科普与个转企引导（补充计划步骤4，无独立表）**：
   - 内容硬编码返回（轻量化，不新增表），符合 §1.3 简化原则
   - 关联接口：`GET /api/v1/operation/finance/content`、`POST /api/v1/operation/finance/individual-to-company/self-check`

### 2.6 预算消费模块（核心链路：预算设置 → 交易扣减 → 转储蓄）

```
biz_budget_category  ───1:N───  biz_budget_setting  ───1:N───  biz_transaction
                                                               │
sys_user  ───1:N──────────────────────────────────────────────┘
   │
   └───1:N───  biz_saving_goal
```

**关系说明：**

1. **biz_budget_category（预算分类）**：
   - 系统预设 + 用户可自定义（`is_system` 标记）
   - 分类类型：FOOD / ENTERTAINMENT / SHOPPING / TRANSPORT / OTHER

2. **biz_budget_setting（预算设置）**：
   - `setting.user_id` → `sys_user.id`
   - `setting.category_id` → `biz_budget_category.id`
   - 按月设置：`(user_id, category_id, budget_period)` 联合唯一
   - 字段：budget_amount / used_amount / remaining_amount / usage_percent
   - 分级提醒标记：remind_50_sent / remind_20_sent / remind_over_sent

3. **biz_transaction（交易记录）**：
   - `transaction.user_id` → `sys_user.id`
   - `transaction.category_id` → `biz_budget_category.id`（MCC 映射后的分类）
   - `transaction.budget_id` → `biz_budget_setting.id`（关联对应月份的预算）
   - 来源：SIMULATED（模拟数据）、BANK_IMPORT（银行导入）

4. **biz_saving_goal（心愿储蓄）**：
   - `goal.user_id` → `sys_user.id`
   - 预算结余可一键转入心愿储蓄
   - 字段：target_amount / current_amount / progress_percent

### 2.7 金融安全模块

```
sys_user  ───1:N───  biz_fraud_detection_log
   │
   ├───1:N───  biz_credit_report
   │
   └───1:N───  biz_risk_warning

biz_anti_fraud_content（内容表，无用户关联）
```

**关系说明：**

1. **biz_anti_fraud_content（反诈教学内容）**：
   - 系统内容表，所有用户共享
   - 类型：ARTICLE（文章）、VIDEO（视频）、CASE（案例）
   - 分类：征信修复、套路贷、电信诈骗 等

2. **biz_fraud_detection_log（骗局甄别记录）**：
   - `log.user_id` → `sys_user.id`
   - 用户输入话术文本，系统判定风险
   - 结果：SAFE / SUSPICIOUS / DANGEROUS
   - 风险等级：1-5 级

3. **biz_credit_report（征信报告）**：
   - `report.user_id` → `sys_user.id`
   - 演示数据（SIMULATED），不接入真实征信系统
   - 字段：credit_score / credit_level / 贷款笔数 / 逾期笔数 / 授信额度

4. **biz_risk_warning（风险预警）**：
   - `warning.user_id` → `sys_user.id`
   - 预警类型：OVERDUE_RISK / HIGH_FREQ_BORROW / CREDIT_ABNORMAL / BUDGET_OVER / CASHFLOW_WARNING
   - 预警等级：LOW / MEDIUM / HIGH / CRITICAL
   - 可关联具体业务模块和业务ID：`related_module` + `related_id`

### 2.8 消费治理模块（补充计划步骤5）

```
sys_user  ───1:N───  biz_risk_warning (warning_type=HIGH_FREQ_BORROW，高频借贷预警)
   │
   ├───1:N───  biz_risk_warning (warning_type=CREDIT_ABNORMAL，征信异常预警)
   │
   ├───1:N───  biz_credit_report (source=SIMULATED，软查询模拟)
   │
   ├───1:N───  biz_risk_assessment（风险测评，仅 1 条 is_latest=1）
   │                └── 匹配 ── biz_finance_product（target_risk_level 软匹配）
   │
   └── 三层消费引导（无独立表，复用 biz_transaction / biz_budget_setting / sys_message）
```

**关系说明：**

1. **高频借贷/非理性负债预警（HIGH_FREQ_BORROW）**：
   - 复用 `biz_risk_warning` 表，`warning_type=HIGH_FREQ_BORROW`
   - 数据来源：`biz_loan_application`（近 30 天申请数 + APPROVED 未结清数）+ `biz_bookkeeping_record`（月度收入与还款支出）
   - 触发规则（任一命中即预警）：
     - 近 30 天申请数 ≥ 3 → HIGH
     - APPROVED 未结清 ≥ 2 且新增申请 → HIGH
     - 月度负债率 > 50% → CRITICAL
   - 7 天内同用户不重复触发（去重）
   - 同步发站内信 `sys_message`（type=SAFETY, bizType=HIGH_FREQ_BORROW）

2. **常态化征信监测（CREDIT_ABNORMAL）**：
   - 复用 `biz_credit_report` 表，`source=SIMULATED`（不产生硬查询）
   - 评分公式：`credit_score = 750 - loanCount*10 - overdue*30`（clamp [350, 850]）
   - 异常判定：6 个月贷款笔数 ≥ 4 或 逾期 > 0
   - 异常转 `biz_risk_warning`（`warning_type=CREDIT_ABNORMAL`，`warning_level=HIGH`），7 天去重
   - 同步发站内信

3. **三层消费引导（无独立表，复用现有表）**：
   - 第一层 交易后即时推送：复用 `biz_transaction` + `biz_budget_setting`，单笔金额 > 月度预算总额 30% 时触发 `sys_message`
   - 第二层 月度账单分析：复用 `biz_transaction` 聚合当月收入/支出/净现金流/储蓄率/负债预警
   - 第三层 支付前提醒：演示页，复用 `biz_budget_setting` 检查剩余预算，标注"仅工行自有支付场景"

4. **理财匹配与风险测评**：
   - `biz_risk_assessment.user_id` → `sys_user.id`
   - 同一用户仅 1 条 `is_latest=1`，新测评提交时旧记录置 0
   - 评分映射：≤18 CONSERVATIVE（R1）/ 19-30 STEADY（R1+R2）/ 31-40 BALANCED（R1+R2）
   - `biz_finance_product` 与 `sys_user` 无强外键，通过 `target_risk_level` 与用户 `risk_level` 软匹配
   - 合规口径：理财非存款、产品有风险、工行仅代销；未测评不可推荐；演示不发起真实购买、不扣款

---

## 三、跨模块关系汇总

### 3.1 用户为中心的全局关系

```
                         ┌── sys_user_role ── sys_role
                         │
sys_user ────┬───────────┼── sys_login_log
             │           │
             │           └── sys_message
             │
             ├── biz_landlord（房东身份）
             │
             ├── biz_guarantee_application（租客）
             │       └── biz_guarantee ── biz_guarantee_claim
             │
             ├── biz_loan_application
             │       ├── biz_credit_limit
             │       └── biz_entrust_payment ── biz_merchant
             │
             ├── biz_bookkeeping_record
             ├── biz_cashflow_report
             │
             ├── biz_budget_setting ── biz_transaction
             ├── biz_saving_goal
             │
             ├── biz_fraud_detection_log
             ├── biz_credit_report
             ├── biz_risk_warning
             │
             ├── biz_risk_assessment ──（匹配）── biz_finance_product
             │
```

### 3.2 主链路串联关系

**演示主链路涉及的表流转：**

```
注册(sys_user)
   ↓
设置预算(biz_budget_category → biz_budget_setting)
   ↓
模拟交易(biz_transaction) → 触发分级提醒(sys_message) → 结余转储蓄(biz_saving_goal)
   ↓
申请保函(biz_guarantee_application) → 房东确认 → AI复审 → 缴费 → 开函(biz_guarantee)
   ↓
贷款预审(biz_loan_application) → 授信(biz_credit_limit) → 受托支付(biz_entrust_payment → biz_merchant)
   ↓
经营记账(biz_bookkeeping_record) → 现金流报表(biz_cashflow_report)
   ↓
反诈甄别(biz_fraud_detection_log) / 征信报告(biz_credit_report) / 风险预警(biz_risk_warning)
```

---

## 四、设计说明

1. **逻辑删除**：所有业务表均使用 `deleted` 字段做逻辑删除，保留数据完整性。
2. **金额类型**：所有金额字段统一使用 `DECIMAL(18,2)`，不使用 FLOAT/DOUBLE，避免精度丢失。
3. **索引策略**：
   - 所有关联外键字段建索引（`idx_*` 前缀）
   - 常用组合查询建联合索引（如 `idx_user_read`、`idx_user_date`）
   - 唯一约束用 `uk_` 前缀
4. **冗余字段**：部分冗余设计（如保函表冗余 tenant_id、landlord_id），优化查询性能，避免多表 JOIN。
5. **状态机**：申请类表均有完整的状态流转字段，便于业务流程追踪。
6. **审计字段**：所有表均有 `create_time` / `update_time`，支持数据变更追踪。
