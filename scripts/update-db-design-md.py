# -*- coding: utf-8 -*-
# database-design.md 更新：总览 30→32 张表，补充计划新增表 5→7，新增 L3 两张表说明
import io

path = r'D:\codex\codex-data\qingqi-ecity\docs\deployment\database-design.md'
s = io.open(path, 'r', encoding='utf-8').read()

# 1) 首行总览
old1 = '> 共 30 张表，分 7 大模块 + 5 张补充计划新增表。'
new1 = '> 共 32 张表，分 7 大模块 + 7 张补充计划新增表。'
assert old1 in s
s = s.replace(old1, new1)

# 2) 模块总览表补充计划行
old2 = '| 补充计划新增表 | 5 | biz_policy, biz_credit_txn, biz_insurance_product, biz_finance_product, biz_risk_assessment |'
new2 = '| 补充计划新增表 | 7 | biz_policy, biz_credit_txn, biz_insurance_product, biz_finance_product, biz_risk_assessment, biz_scenario_practice, biz_scenario_round |'
assert old2 in s
s = s.replace(old2, new2)

# 3) 新增两节说明（插在 biz_risk_assessment 节之后、`---` 分隔线之前）
anchor = '### biz_risk_assessment 风险测评表'
assert anchor in s
# 找到该节末尾（下一行 "---"）
seg_start = s.index(anchor)
seg_end = s.index('---', seg_start)
add = '''
### biz_scenario_practice 反诈对话演练记录表（L3 对话式演练，模拟）

- **用途**：记录「对话式反诈演练」（AI 扮演诈骗分子，用户自由发言对抗）的完整演练记录；结果联动画像「反诈指数」与消息中心站内信
- **核心字段**：`practice_no`（演练编号，唯一）、`user_id`、`scenario_id`（→ biz_anti_fraud_content.id，content_type=SCENARIO_DIALOG）、`round_count`、`result`（SAFE识破/LURED被诱骗/TIMEOUT超时/FINISHED主动结束）、`risk_score`（0-100 越低越安全）、`result_desc`（AI 复盘摘要）
- **判定规则**：识破词库/危险词库双层打分；安全分≥70 识破、≤40 连续 2 回合被诱骗、≥10 回合超时；LLM 增强可配（缺 Key 自动降级本地剧本）
- **关联接口**：`POST /api/v1/safety/scenario/{id}/practice/start`、`POST /api/v1/safety/scenario/practice/{practiceNo}/turn`、`POST /api/v1/safety/scenario/practice/{practiceNo}/finish`、`GET /api/v1/safety/scenario/practice/history`、`GET /api/v1/safety/scenario/practice/{practiceNo}`

### biz_scenario_round 反诈对话演练回合明细表（L3 对话式演练，模拟）

- **用途**：演练每一回合的发言明细（诈骗方话术 / 用户应对 + 该回合安全分），用于回放复盘
- **核心字段**：`practice_id`（→ biz_scenario_practice.id）、`round_no`、`speaker`（FRAUD/USER）、`content`（发言内容）、`safe_score`（用户回合安全分 0-100）、`hit_words`（命中的词库标签，逗号分隔）
- **关联接口**：`GET /api/v1/safety/scenario/practice/{practiceNo}`（回放时返回主表+回合明细）
'''
s = s[:seg_end] + add + s[seg_end:]

io.open(path, 'w', encoding='utf-8', newline='').write(s)
print('database-design.md updated')
