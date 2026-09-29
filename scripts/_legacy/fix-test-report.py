# -*- coding: utf-8 -*-
"""增量更新全链路测试报告：补 BUG-5、43 表、脚本路径、部署复刻要点。"""
import io, os

P = r"D:\codex\codex-data\qingqi-ecity\docs\test\测试用例表-全链路检测报告.md"
with io.open(P, "r", encoding="utf-8-sig") as f:
    t = f.read()

# 1. 头部范围行
t = t.replace("41 张表", "43 张表")

# 2. 问题总览表：在 BUG-4 行后补 BUG-5
bug4 = "| BUG-4 | 🔴 高·安全 | 保函索赔「人工复核队列/人工复核」普通用户可访问（越权） | `/v1/guarantee/claims/manual-review-queue`、`/{id}/review` 无 `/admin/` 前缀，Filter 不校验角色，Service 也未校验 | `GuaranteeClaimService` 新增 `checkBackOffice()`：仅 ADMIN/BANK_OPERATOR，否则 4001 | ✅ testuser/entrepreneur 均 4001，banker 正常 |"
bug5 = bug4 + "\n" + "| BUG-5 | 🔴 高·数据 | 反诈定时任务误删人工种子预警 | `AntiFraudAlertSourceService.trimToMax()` 按 `publish_time < 最旧保留时间` 全表清理，把人工维护的种子预警（publish_time 固定较旧）当垃圾删除 | 新增 `AUTO_SOURCES` 白名单，`trimToMax()` 仅统计/保留/清理自动来源（平台实时轮换模拟 / 公开反诈源爬取），人工预警永不参与清理 | ✅ 重建部署后 `source,COUNT(*)` 种子 10 + 自动 1 共存；日志输出「人工预警不受影响」 |"
assert bug4 in t
t = t.replace(bug4, bug5)

# 3. 修复总结句补充
old_note = "> 已修复并全部复测通过。以下用例表中所有 FAIL 项均已在修复后复测转 PASS；部分 FAIL 为测试脚本初始参数写错（接口字段名/枚举），修正后通过，已在结果列注明\"脚本修正\"。"
new_note = old_note + "\n\n> **补充轮（2026-09-29）**：新增发现并修复 BUG-5（反诈定时任务误删人工种子数据，见上表）；本轮另完成 43 表清单与 schema.sql 一致性核对、注册白名单/AI审核全链路实测、A/B 用户与贷款对应约束实测（B 类用户提 A 类循环贷被 AI 审查拦截，code=3001）、管理端 dashboard 数据实测（用户 11/保函 13/预警 7/审核 32）。"
assert old_note in t
t = t.replace(old_note, new_note)

# 4. 测试脚本节：路径 scripts/ -> scripts/_legacy/
t = t.replace("存放于 `scripts/`（Python 标准库实现，无第三方依赖）：", "存放于 `scripts/_legacy/`（历史开发/测试脚本归档，Python 标准库实现，无第三方依赖；不参与部署）：")
t = t.replace("`docker compose up -d --build` 后依次运行 `python scripts/xxx.py`。", "`docker compose up -d --build` 后依次运行 `python scripts/_legacy/xxx.py`。")

# 5. 遗留说明：补部署复刻要点
old_sec = "## 五、遗留说明"
new_block = """## 五、部署复刻注意点（2026-09-29 实测补充）

1. **管理端登录态 localStorage key**：管理端（admin-web）使用 `admin_token` / `admin_refreshToken` / `admin_userInfo`；用户端（user-web）才是 `accessToken` + `userInfo`。浏览器调试注入时勿混用，否则 dashboard 全 0 且提示「登录状态已过期」（非代码 bug）。
2. **PowerShell 调接口中文编码**：`Invoke-RestMethod -Body` 直接传中文 JSON 会变成 `????`（如注册学校名），必须 `[System.Text.Encoding]::UTF8.GetBytes($json)` 显式编码后再传。
3. **`.env` 配置项**：除 `JWT_SECRET` / `CHAT_LLM_*` 外，反诈双模式开关 `ANTI_FRAUD_SOURCE=mock|real`、`ANTI_FRAUD_POLL_MINUTES`、`ANTI_FRAUD_SOURCE_URL` 均在 `.env.example` 有说明（见 docs/deployment/07-anti-fraud-source.md）。
4. **表数量**：schema.sql 当前 43 张表（含 biz_ai_review_log、biz_moveout_record），核对文档时以 `SELECT COUNT(*) FROM information_schema.TABLES WHERE TABLE_SCHEMA='qingqi'` 为准。
5. **SQL 进容器**：PowerShell 管道直传长 SQL 会参数解析失败，须 `Set-Content -Encoding UTF8 $env:TEMP\\q.sql` 后 `docker exec -i qqecity-mysql mysql -uroot -p123456 --default-character-set=utf8mb4 qingqi < q.sql`。

---

## 六、遗留说明
"""
assert old_sec in t
t = t.replace(old_sec, new_block)

with io.open(P, "w", encoding="utf-8", newline="") as f:
    f.write(t)
print("done")
