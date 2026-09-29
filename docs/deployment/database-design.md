# 青启e城 · 数据库 ER 关系说明

> 共 43 张表（系统公共 6 + 业务模块 33 + 模拟支付中台 4），分 7 大模块 + 补充计划新增表 + 官方政策入口 + 支付中台。本文档用文字描述各表之间的实体关系与核心外键关联。
> 表命名：`sys_*` 系统公共表，`biz_*` 业务模块表，`pay_*` 模拟支付中台表。
>
> **初始化方式**：执行 `backend/sql/init-database.sql`（内部按序 SOURCE schema.sql + data.sql）
> 或分步执行：`mysql -u root -p < schema.sql && mysql -u root -p < data.sql`

---

## 一、模块总览

| 模块 | 表数量 | 表名 |
| --- | --- | --- |
| 用户与权限 | 6 | sys_user, sys_role, sys_user_role, sys_login_log, biz_school, biz_registration_review |
| 消息中心 | 1 | sys_message |
| 安居金融风控 | 7 | biz_landlord, biz_house, biz_rental_contract, biz_guarantee_application, biz_guarantee, biz_guarantee_claim, biz_moveout_record |
| 青创e贷 | 5 | biz_merchant, biz_loan_application, biz_credit_limit, biz_entrust_payment, biz_entrust_review |
| 经营赋能 | 2 | biz_bookkeeping_record, biz_cashflow_report |
| 预算消费 | 4 | biz_budget_category, biz_budget_setting, biz_transaction, biz_saving_goal |
| 金融安全 | 5 | biz_anti_fraud_content, biz_fraud_detection_log, biz_credit_report, biz_risk_warning, biz_anti_fraud_alert |
| 官方政策入口导航 | 1 | biz_policy_portal |
| 补充计划新增表 | 8 | biz_policy, biz_credit_txn, biz_insurance_product, biz_finance_product, biz_risk_assessment, biz_scenario_practice, biz_scenario_round, biz_ai_review_log |
| 模拟支付中台 | 4 | pay_wallet, pay_merchant_account, pay_order, pay_transaction |

> 合计 43 张表（6+1+7+5+2+4+5+1+8+4），与 `backend/sql/schema.sql` 中 43 个 CREATE TABLE 一一对应。

---

## 补充计划新增表说明

### biz_ai_review_log 借款前AI审查日志（模拟AI）

- **用途**：记录每次借款前 AI 审查（A 类提款 / B 类打款）的结果、评分与逐条原因，供管理端「贷款审批 → AI审核记录」展示
- **核心字段**：`credit_type`（A_TYPE/B_TYPE）、`biz_type`（WITHDRAW提款/ENTRUST_PAY打款）、`result`（PASS/REJECT）、`ai_score`（100分制）、`passed_items`/`rejected_items`（JSON数组）、`user_name`（冗余展示）、`request_no`（审查单号）
- **写入时机**：`LoanAiGuardService.guard()` 每次审查后落库（提款/打款均触发）
- **关联接口**：`GET /api/v1/admin/loan/ai-review-logs`（管理端分页，支持按 userId/result/creditType 筛选）
- **约束**：演示系统为模拟 AI，结果不构成真实授信依据


### biz_moveout_record 退租留档审核表（房屋照片留档 + AI 审核）

- **用途**：租房结束后租客上传房屋照片留档供审核（照片是否合格），并记录房东"确认无需索赔"；房屋状态分区与索赔前置条件
- **核心字段**：`guarantee_id`（关联保函）、`photos_json`（房屋照片文件列表 JSON）、`check_result`（PASS合格留档 / REVIEW需补拍或人工复核）、`landlord_confirm`（PENDING待确认 / CONFIRMED已确认无需索赔）、`landlord_confirm_time`、`landlord_confirm_remark`
- **状态流转**：租期结束 → 租客提交留档（照片 AI 审核模拟）→ 审核通过归入"租后"分区 → 房东确认（管理端代房东操作）→ CONFIRMED 完美结束；保函到期前房东未发起索赔则自动过期，不能再发起
- **关联接口**：保函页「房屋状态」分区、`/api/v1/guarantee/**` 下留档提交与查询（管理端代房东确认）
- **约束**：留档仅系统方防止纠纷，不写入保函状态机；租期前/租中状态由保函与索赔记录推导

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

### biz_school 高校库（模拟，注册学历核验白名单）

- **用途**：注册环节「AI 学历审查」的学校白名单（对应注册流程图「学校∈biz_school高校库（启用）」）
- **核心字段**：`school_name`（唯一）、`school_code`、`status`（0停用 1启用）
- **审核规则**：注册填写的学校必须命中本表且 `status=1`（启用），否则学历核验不通过
- **演示数据**：10 所高校（清华大学/北京大学/复旦大学/浙江大学/中山大学/华南理工大学/暨南大学/广州大学/深圳大学/广东工业大学）
- **关联接口**：`GET /api/v1/auth/schools`（注册页下拉，仅返回启用学校）

### biz_registration_review 注册AI审核记录表（模拟）

- **用途**：记录每次注册申请的 AI 审核明细（白名单人群 / 同一材料同一人 / 重复注册），审核留痕可追溯
- **核心字段**：`review_no`（唯一）、`id_card`（SHA-256 哈希，不存明文）、`user_type`、`school`、`education_level`、`graduation_date`、`verify_type`（XUE_XIN_WANG学信网 / STUDENT_CARD学生证·仅在校生 / GRAD_CERT毕业证·仅毕业2年内）、`whitelist_pass`+`whitelist_detail`（白名单审核）、`material_pass`+`material_detail`（同一材料/同一人审核）、`result`（APPROVED/REJECTED）、`reject_reason`、`user_id`（注册成功后的用户ID）
- **审核规则（模拟 AI）**：
  - 白名单人群：人群类型 ∈ {STUDENT/GRADUATE/ENTREPRENEUR}；STUDENT/GRADUATE 需学历核验（学校∈biz_school启用 + 学历层次合法 + GRADUATE 毕业2年内 + 核验方式有效）；核验方式按人群限定：**在校生可选学信网或学生证照片识别（STUDENT_CARD）**，**毕业2年内可选学信网或毕业证照片识别（GRAD_CERT）**，学生证用于毕业人群、毕业证用于在校生均视为违规拒绝
  - 同一材料/同一人：身份证号已注册 → 同一人；手机号已注册 → 同一材料
  - 重复注册：用户名 / 证件号 / 手机号三重查重
- **关联接口**：`POST /api/v1/auth/register/ai-review`（AI 预审，不落库）、`POST /api/v1/auth/register`（正式注册，落库写记录）

### biz_anti_fraud_alert 实时反诈预警（模块5·安全教育平台）

- **用途**：安全教育平台「实时预警」横幅数据（人工维护、模拟实时发布），按平台场景维度展示给用户
- **核心字段**：`alert_level`（DANGER/WARNING/INFO）、`source`（来源，模拟）、`region`（涉及地区）、`summary`（预警内容）、`link_url`（官方链接：举报/提示入口）、`relate_scene`（GUARANTEE保函/LOAN创业贷/CREDIT征信/WEALTH理财/PLATFORM客服/OTHER）、`publish_time`（发布时间，模拟实时）
- **演示数据**：10 条（含"免押金保函代办""征信洗白""低息创业贷先交解冻费""高收益理财""AI 换脸冒充熟人"等典型骗局预警）
- **关联接口**：安全教育平台预警列表

### biz_policy_portal 官方政策入口导航（政策模块·独立专区）

- **用途**：政策专区「官方入口导航」——收录国家/省/市的人社、住建、政务、税务、教育等官方政策查询入口，跳转真实政府官网
- **核心字段**：`portal_name`（官网名称）、`portal_type`（GOV_HR人社/GOV_HOUSING住建房管/GOV_AFFAIR政务服务/GOV_TAX税务/GOV_EDU教育高校/GOV_OTHER其他）、`region`（全国/省/直辖市）、`url`（官网链接）、`description`（入口说明）、`sort_order`（排序）
- **演示数据**：29 条（覆盖全国 + 浙江/广东/江苏/上海/北京/四川/湖北/福建/山东/湖南/河南/安徽/重庆/广西/陕西等）
- **关联接口**：`GET /api/v1/policy/portal`（政策专区官方入口导航）

### pay_wallet 用户钱包表（模拟支付中台）

- **用途**：模拟支付中台的用户钱包账户，演示充值/支付/退款余额流转
- **核心字段**：`user_id`（唯一）、`balance`（可用余额，模拟）、`frozen`（冻结金额，支付中）、`pay_password`（支付密码，模拟默认123456）、`status`（ACTIVE/FROZEN/CLOSED）
- **关联**：`pay_wallet.user_id` → `sys_user.id`（一对一）

### pay_merchant_account 商户收款账户表（模拟支付中台）

- **用途**：商户（biz_merchant）在支付中台的收款账户，记录受托支付/消费收款入账
- **核心字段**：`merchant_id`（唯一，→ biz_merchant.id）、`balance`（收款余额）、`total_income`（累计收款）、`status`（ACTIVE/FROZEN）

### pay_order 支付订单表（模拟支付中台）

- **用途**：模拟支付订单（待支付→已支付/已关闭/已退款），统一承载保函费、贷款还款、受托支付、模拟消费、充值、退款等支付场景
- **核心字段**：`order_no`（唯一）、`user_id`、`biz_type`（GUARANTEE_FEE保函费/LOAN_REPAY还款/ENTRUST_PAY受托支付/MERCHANT_CONSUME模拟消费/RECHARGE充值/REFUND退款）、`biz_id`（关联业务ID：保函ID/还款流水ID/受托支付ID等）、`merchant_id`（收款商户）、`subject`、`amount`、`pay_method`（WALLET/CARD/SIM_BANK）、`status`（PENDING_PAY/PAID/CLOSED/REFUNDED）、`pay_time`、`close_time`、`expire_time`（默认15分钟）

### pay_transaction 支付流水表（模拟支付中台）

- **用途**：每一笔支付/退款的资金流水明细（IN收入/OUT支出），记录交易后余额用于对账展示
- **核心字段**：`txn_no`（唯一）、`order_id`+`order_no`（→ pay_order）、`user_id`、`merchant_id`、`direction`（IN/OUT）、`amount`、`balance_after`（交易后余额）、`pay_method`、`status`（SUCCESS/FAILED）、`biz_type`、`related_no`（关联业务单号：保函编号/还款流水号/受托支付号）
- **关联**：`pay_transaction.order_id` → `pay_order.id`（一对多，一单可多笔流水，如退款）

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

5. **biz_entrust_review（受托支付复核单）**：
   - 用户自定义商户（USER_CUSTOM）每次受托支付前须银行复核，复核通过才执行放款（扣额度 + EP 流水 + 商户收款入账）；平台通用商户直接放款、不建复核单
   - 字段与状态流转详见文末「14.1 受托支付复核单」章节

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

4. **biz_saving_goal（心愿储蓄 / 还款保障金）**：
   - `goal.user_id` → `sys_user.id`
   - 预算结余可一键转入心愿储蓄（goal_type=WISH）；模块3/4×贷款联动新增 **还款保障金**（goal_type=REPAY_GUARD，用于贷款一键还本付息）
   - 字段：target_amount / current_amount / progress_percent / **goal_type**（WISH/REPAY_GUARD，默认 WISH）/ **linked_credit_limit_id**（预留：保障金绑定的授信额度，当前按用户聚合演示，未实际写入）

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

5. **biz_anti_fraud_alert（实时反诈预警）**：
   - 系统内容表（人工维护、模拟实时发布），所有用户共享，无用户关联
   - 级别：DANGER / WARNING / INFO；按平台场景 `relate_scene`（GUARANTEE/LOAN/CREDIT/WEALTH/PLATFORM/OTHER）维度展示
   - 演示数据 10 条，详情见「补充计划新增表说明」

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

## 14.1 biz_entrust_review 受托支付复核单（新增，自定义商户每单复核）

> **新增理由**：用户自定义商户（USER_CUSTOM）审核通过（VERIFIED）后仅归属本人可用，且**每次受托支付前须银行复核**（区别于平台通用商户直接放款）。复核单需独立记录（一商户可多次打款、多次复核），无法在 `biz_merchant` 单行内承载，故新增本表。

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| id | BIGINT PK | 主键 |
| review_no | VARCHAR(50) UK | 复核单号（ERR+时间戳） |
| user_id | BIGINT | 借款人ID |
| loan_application_id | BIGINT | 贷款申请ID |
| merchant_id | BIGINT | 收款商户ID（用户自定义商户） |
| merchant_name | VARCHAR(200) | 商户名称快照 |
| amount | DECIMAL(18,2) | 打款金额 |
| purpose | VARCHAR(200) | 用途说明 |
| trade_proof | VARCHAR(500) | 交易凭证说明 |
| status | VARCHAR(20) | PENDING待复核 / APPROVED已通过 / REJECTED已驳回 |
| reviewer_id | BIGINT | 复核人ID（banker） |
| review_remark | VARCHAR(500) | 复核意见/驳回原因 |
| review_time | DATETIME | 复核时间 |
| payment_id | BIGINT | 放款流水ID（复核通过后生成的受托支付流水） |
| create_time / update_time | DATETIME | 创建/更新时间 |

**业务规则**：
- 平台通用商户（merchant_source=SYSTEM，VERIFIED）：受托支付直接放款，不建复核单；
- 用户自定义商户（USER_CUSTOM，VERIFIED 且归属本人）：每次受托支付提交即生成复核单（PENDING，不扣额度不放款）→ 管理端复核通过（APPROVED）才执行放款（扣额度 + EP 流水 + 商户收款入账）；驳回（REJECTED）不放款、额度不动。
- 用户端「打款记录」= 受托支付成功流水（SUCCESS）+ 复核单（PENDING_REVIEW / REJECTED / APPROVED）。
