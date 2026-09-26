# 青启e城 · 数据库 ER 关系说明

> 共 25 张表，分 7 大模块。本文档用文字描述各表之间的实体关系与核心外键关联。
> 表命名：`sys_*` 系统公共表，`biz_*` 业务模块表。

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
   └───1:N───  biz_cashflow_report
```

**关系说明：**

1. **biz_bookkeeping_record（记账记录）**：
   - `record.user_id` → `sys_user.id`
   - 类型：INCOME（收入）、EXPENSE（支出）
   - 来源：AUTO（自动识别）、MANUAL（手动录入）、IMPORT（导入）
   - `is_confirmed`：模糊交易待确认标记

2. **biz_cashflow_report（现金流报表）**：
   - `report.user_id` → `sys_user.id`
   - 按月生成：`(user_id, report_period)` 联合唯一
   - 字段：总收入/总支出/净现金流/利润/利润率
   - 预警等级：NORMAL / WARNING / CRITICAL

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
             └── biz_risk_warning
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
