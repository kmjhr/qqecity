# -*- coding: utf-8 -*-
"""更新 database-design.md：43 表总览 + biz_moveout_record 说明段落。"""
import io, os

P = r"D:\codex\codex-data\qingqi-ecity\docs\deployment\database-design.md"
with io.open(P, "r", encoding="utf-8-sig") as f:
    text = f.read()

# 1. 顶部总览
text = text.replace(
    "> 共 41 张表（系统公共 5 + 业务模块 32 + 模拟支付中台 4）",
    "> 共 43 张表（系统公共 6 + 业务模块 33 + 模拟支付中台 4）",
)
text = text.replace(
    "| 安居金融风控 | 6 | biz_landlord, biz_house, biz_rental_contract, biz_guarantee_application, biz_guarantee, biz_guarantee_claim |",
    "| 安居金融风控 | 7 | biz_landlord, biz_house, biz_rental_contract, biz_guarantee_application, biz_guarantee, biz_guarantee_claim, biz_moveout_record |",
)
text = text.replace(
    "| 补充计划新增表 | 7 | biz_policy, biz_credit_txn, biz_insurance_product, biz_finance_product, biz_risk_assessment, biz_scenario_practice, biz_scenario_round |",
    "| 补充计划新增表 | 8 | biz_policy, biz_credit_txn, biz_insurance_product, biz_finance_product, biz_risk_assessment, biz_scenario_practice, biz_scenario_round, biz_ai_review_log |",
)
text = text.replace(
    "> 合计 41 张表（6+1+6+5+2+4+5+1+7+4），与 `backend/sql/schema.sql` 中 41 个 CREATE TABLE 一一对应。",
    "> 合计 43 张表（6+1+7+5+2+4+5+1+8+4），与 `backend/sql/schema.sql` 中 43 个 CREATE TABLE 一一对应。",
)

# 2. 在 biz_ai_review_log 说明段落之后补 biz_moveout_record 说明
anchor = "### biz_policy 政策库"
moveout_doc = """### biz_moveout_record 退租留档审核表（房屋照片留档 + AI 审核）

- **用途**：租房结束后租客上传房屋照片留档供审核（照片是否合格），并记录房东"确认无需索赔"；房屋状态分区与索赔前置条件
- **核心字段**：`guarantee_id`（关联保函）、`photos_json`（房屋照片文件列表 JSON）、`check_result`（PASS合格留档 / REVIEW需补拍或人工复核）、`landlord_confirm`（PENDING待确认 / CONFIRMED已确认无需索赔）、`landlord_confirm_time`、`landlord_confirm_remark`
- **状态流转**：租期结束 → 租客提交留档（照片 AI 审核模拟）→ 审核通过归入"租后"分区 → 房东确认（管理端代房东操作）→ CONFIRMED 完美结束；保函到期前房东未发起索赔则自动过期，不能再发起
- **关联接口**：保函页「房屋状态」分区、`/api/v1/guarantee/**` 下留档提交与查询（管理端代房东确认）
- **约束**：留档仅系统方防止纠纷，不写入保函状态机；租期前/租中状态由保函与索赔记录推导

"""

if "### biz_moveout_record" not in text and anchor in text:
    text = text.replace(anchor, moveout_doc + anchor)
    print("inserted biz_moveout_record doc")
else:
    print("anchor missing or already present:", "biz_moveout_record" in text)

with io.open(P, "w", encoding="utf-8", newline="") as f:
    f.write(text)
print("done")
