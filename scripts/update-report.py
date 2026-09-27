# -*- coding: utf-8 -*-
import io

p = r'D:\codex\codex-data\qingqi-ecity\docs\common\补充测试报告.md'
s = io.open(p, 'r', encoding='utf-8').read()

# ---------- 1) 缺陷清单整段替换 ----------
old_defect = s[s.find('## 五、缺陷清单'):s.find('## 六、结论')]
new_defect = '''## 五、缺陷清单（2026-09-27 晚已修复并回归）

### 已修复（本轮会话完成，均已回归验证）
| ID | 严重度 | 问题 | 处理 | 回归证据 |
| -- | --- | --- | --- | --- |
| D1 | P1 | user-web/src/api/chat.js 发送体字段 `{content}`，后端要求 `{message}`，对话全部 400「消息内容不能为空」 | 已改 `{message: content}`，并重建 user-web 镜像 | POST /v1/chat/messages 返回带来源引用回答 |
| D2 | P1 | 管理端角色校验仅放行 ADMIN，banker01(BANK_OPERATOR) 进不了 /v1/admin/* 审核台，与计划书 §8 不符 | JwtAuthFilter 新增 isBackOffice 放行 ADMIN/BANK_OPERATOR；admin-web login 同步放宽 + UserInfo 类型补充 | banker01 访问 loan.apps / risk-warnings / admin user.page 均 code=0；普通用户仍 4001 |
| D3 | P1 | 反诈情景教学库无 SCENARIO_SIM 数据，list 空 / submit 5000 | data.sql 已含 id=5/6/7 三条（刷单/公检法/征信洗白），补灌运行库 | list 返回 3 条；submit 全对 accuracy=100、选错 66 且有纠偏 |
| D4 | P1 | 运行中 MySQL 数据卷缺 5 张新表 + 观察期列 + 电子签名列，约 10 个接口 5000 | 容器内幂等迁移（scripts/test-migrate.sql） | 42 个只读 GET 全部 code=0 |
| D5 | P2 | 防刷单 detect 永远「未授权聚合流水」：CashflowAggregateService 用 new ObjectMapper() 未注册 JSR310，序列化 LocalDateTime 抛异常被 try/catch 吞掉，Redis 写不进去 | 改为注入 Spring ObjectMapper（自带 JSR310） | 授权后 report totalCount=23、income 10395；detect hit=true、riskScore=25 MEDIUM、命中跨维比对 |
| D6 | P2 | schema.sql 中 4 条 CYTX 政策 INSERT 的 policy_type/target_crowd 列错位，全新 docker 初始化会报错 | 已修正 4 行列序（scripts/fix-schema.py） | 修正后 INSERT 在运行库执行通过，policy_type=ENTREPRENEUR、max_amount 正确 |
| D7 | P3 | 种子商户全部 VERIFIED，#6「非白名单商户受托支付被拒」无数据可演示 | data.sql 已含 id=5 PENDING 商户（速速达物流），补灌运行库 | /merchants/by-status?verifyStatus=PENDING 返回该商户 |

### 遗留（未处理）
| ID | 严重度 | 问题 | 建议 |
| -- | --- | --- | --- |
| D8 | P3 | #11 B转A advance 仅验证了 NONE 态拒绝；达标转 A / 数据不足退出两条路径未造数验证 | 造一条 OBSERVING 的 B 类额度后推进月份 |

### 后置
| ID | 说明 |
| -- | --- |
| D9 | JWT 真实过期未等待实测（逻辑已核对：过滤器校验 exp，篡改/缺失均 1002） |
| — | #23 小程序/公网部署（步骤10）不在本次测试范围 |

'''
s = s.replace(old_defect, new_defect)

# ---------- 2) 结论更新 ----------
old_concl = '''* 23 项中 **15 项通过、6 项部分通过（需补造数）、1 项未通过（#1 情景教学无数据）**。

* 安全面：未登录 / 伪造 token / 越权 / 数据隔离均正确拦截，错误码 1002/4001/3001/1001 行为符合约定。

* 性能面：4 个核心接口 100 并发 P95 ≤ 437ms，达标。

* **演示前必须处理 D1 前端重新构建 + D2 banker 角色放行 + D3 情景教学造数**，否则答辩时对话页空白、banker 账号进不了审核台、反诈教学点不开。'''
new_concl = '''* 23 项中 **19 项通过、3 项部分通过（#5 预算三档 / #6 非白名单拒付 / #11 B转A双路径，需补造数逐项触发）、1 项不属本次（#23 小程序/公网）**。
* 安全面：未登录 / 伪造 token / 越权 / 数据隔离均正确拦截，错误码 1002/4001/3001/1001 行为符合约定；管理端审核台已按计划书放行 admin/banker01，普通用户仍 4001。
* 性能面：4 个核心接口 100 并发 P95 ≤ 437ms，达标。
* 上一轮报告中的 P1 缺陷（D1 对话字段 / D2 banker 角色 / D3 情景教学 / D4 库表缺失）及 P2（D5 防刷单 / D6 schema 列错）、P3（D7 商户数据）均已修复并回归通过，可直接演示。'''
assert old_concl in s, 'conclusion block not found'
s = s.replace(old_concl, new_concl)

io.open(p, 'w', encoding='utf-8', newline='').write(s)
print('report updated')
