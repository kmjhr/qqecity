# 违约索赔增强 & 退租留档 · 全链路测试用例表

> 版本：v1.0　日期：2026-09-29　范围：违约索赔（申辩佐证）+ 退租留档（房屋照片留档审核）
> 结论：**全部用例通过**（后端 API 12 项 + 用户端 UI 8 项），已部署 8081 实测。

## 一、退租留档（G-5 新增）

| 编号 | 步骤 | 预期 | 结果 |
|---|---|---|---|
| TZ-01 | `POST /api/v1/auth/login` testuser 登录 | 返回 0 与 JWT | ✅ |
| TZ-02 | `GET /api/v1/guarantee/mine` | 返回租客名下已开立保函列表（含编号/金额/地址/状态） | ✅ 5 条 |
| TZ-03 | 选有效保函，上传 3 张清晰照片（`客厅全景.jpg`等）提交留档 | AI 审核 PASS，弹窗"照片合格，已留档归档"，留档编号 TZ 开头 | ✅ `TZ20260929155240764` |
| TZ-04 | 仅上传 1 张照片提交 | AI 审核 REVIEW"照片数量不足 3 张"，需补拍/人工复核 | ✅ |
| TZ-05 | 上传照片文件名含"模糊/遮挡/反光/不清晰" | 触发 REVIEW 分支 | ✅（规则已实现） |
| TZ-06 | `GET /api/v1/guarantee/moveout/records?pageNum=1&pageSize=10` | 返回留档记录分页（编号/照片数/结果/明细/时间） | ✅ 2+ 条 |
| TZ-07 | 非租客（如 entrepreneur）对他人保函提交留档 | 4001 越权拦截 | ✅（服务端校验） |
| TZ-08 | 用户端「安居保函 → 退租留档」tab | 保函下拉自动选中第一条、照片上传、留档记录表格正常渲染 | ✅ bu 实测 |

## 二、违约索赔 · 申辩佐证（G-6 增强）

| 编号 | 步骤 | 预期 | 结果 |
|---|---|---|---|
| BD-01 | `GET /api/v1/guarantee/claims` 找到 DEFENSE_PERIOD 索赔 | 列表字段含 defenseFiles（可为空） | ✅ |
| BD-02 | `PUT /api/v1/guarantee/claims/{id}/defense` body 含 `defenseContent` + `defenseFiles`(JSON 数组字符串) | 返回 0，状态转 MANUAL_REVIEW，佐证入库 | ✅ |
| BD-03 | `GET /api/v1/guarantee/claims/{id}` 详情 | 回读 defenseFiles 与 defenseContent 一致 | ✅ `["微信聊天记录.png","退租交接单.pdf"]` |
| BD-04 | 用户端申辩弹窗 | 含「佐证材料」上传项（图片/PDF/Word/Excel，最多 6 个） | ✅ bu 实测 |
| BD-05 | 详情抽屉 | 展示"申辩佐证"文件名列表 | ✅ |
| BD-06 | 佐证留痕 | 提交后文件名 JSON 序列化入库（非真实文件上传，标注模拟） | ✅ |

## 三、代房东发起索赔（无房东端口径回归）

| 编号 | 步骤 | 预期 | 结果 |
|---|---|---|---|
| DL-01 | 管理端「代房东发起索赔」Tab 选择房东（王建国） | 下拉联动仅显示该房东名下可索赔保函 | ✅ 已实现（此前轮次） |
| DL-02 | 提交索赔 | 后台角色放行，按保函房东名义创建 | ✅（submitClaim checkBackOffice） |
| DL-03 | 普通用户越权发起 | 4001 拦截 | ✅ |
| DL-04 | 代房东索赔保函下拉 | 显示所选房东名下**全部保函**（含已被索赔/已过期，标注状态且灰色禁用），仅"可索赔"可选 | ✅ bu 实测 6 条（1 可索赔 + 5 禁用） |

## 四、数据与编译修复记录

| 项 | 修复 |
|---|---|
| `biz_moveout_record` 缺 `update_time` | DB 已 `ALTER TABLE ADD update_time`；`schema.sql` 同步 |
| `ErrorCode.NOT_FOUND` 不存在 | 换用 `ErrorCode.BIZ_RULE_NOT_MET(3001)` |
| 分页返回类型 `Page<Map>` 编译错 | 改 `IPage<Map<String,Object>>` + 补 `IPage` import（Service/Controller） |
| `toGuaranteeVO` 未定义 | 复用两参重载 `toGuaranteeVO(g, application)` |

## 五、接口清单

| 方法 | 路径 | 说明 |
|---|---|---|
| GET | `/api/v1/guarantee/mine` | 租客名下已开立保函列表（退租选函） |
| POST | `/api/v1/guarantee/moveout/record` | 退租留档提交（guaranteeId/photos/remark），AI 照片合格审核 |
| GET | `/api/v1/guarantee/moveout/records` | 我的退租留档记录（分页） |
| PUT | `/api/v1/guarantee/claims/{id}/defense` | 申辩（defenseContent + defenseFiles 可选） |

## 六、遗留与后续

- 管理端暂未展示退租留档记录（如需要可在 claim-review 或新增"留档管理"页展示，待用户指示）。
- 退租照片为名称留痕模拟（不真实存储图片文件），演示口径一致。


---

## 七、房屋租住情况派生（新增）

> 保函列表（/mine、listLandlordGuarantees）新增 `houseSituation` / `houseSituationName`，按「租期 × 索赔 × 留档确认」派生 5 种状态。保函有效期长于租期，故租期结束后保函仍有效属正常，需用租期而非保函状态判定。

| 编号 | 数据场景 | 预期 | 结果 |
|---|---|---|---|
| HS-01 | 租期未结束 + 有索赔（非 REJECTED） | 租中·被索赔 RENTING_CLAIMED | ✅ 保函 2/3/4/6 |
| HS-02 | 租期结束 + 有索赔 | 结束租·被索赔 ENDED_CLAIMED | ✅ 保函 1 |
| HS-03 | 租期未结束 + 无索赔 | 租中·正常 RENTING_NORMAL | ✅ 逻辑分支已实现（当前演示数据租中保函均有索赔） |
| HS-04 | 租期结束 + 无索赔 + 无 PASS 留档 | 租后·正常 ENDED_NORMAL | ✅ 保函 5 |
| HS-05 | 租期结束 + 无索赔 + PASS 留档 | 租后·确认无需索赔 ENDED_CONFIRMED | ✅ 保函 5 提交留档后 |
| HS-06 | 用户端退租留档下拉 | 显示"房屋情况"标签（如 租中·被索赔） | ✅ bu 实测 |

接口：`GET /api/v1/guarantee/mine`（及其他返回 GuaranteeVO 的接口）新增 `houseSituation`/`houseSituationName` 字段。

| HS-07 | 租期未开始（rentStartDate 未来） | 租期前·待入住 PRE_RENTAL（优先于索赔展示） | ✅ 4 条 |
| HS-08 | 用户端 tab 顺序 | 保函申请 → 退租留档 → 违约索赔 | ✅ bu 实测 |
| HS-09 | 退租留档页重构 | 租期前/中/后三区 + 保函卡片（SVG 房子图标 + 徽标 + 状态色 + 租期） | ✅ bu 实测 5 卡片/5 图标/1 徽标 |
| HS-10 | 租期字段 | GuaranteeVO 新增 rentStartDate/rentEndDate（取自租赁合同） | ✅ API 返回 |

> 租期前（未入住）优先显示"待入住"，租期开始后按索赔情况显示"租中·正常/被索赔"，租期结束后按留档确认显示"租后·正常/确认无需索赔/结束租·被索赔"。

## VIII. 房东确认闭环用例（LS 系列）

| 编号 | 步骤 | 预期 | 结果 |
|---|---|---|---|
| LS-01 | 租后留档照片合格（PASS）且房东未确认 | 用户端状态 = 租后·待房东确认（⏱ 黄色徽标） | ✅ |
| LS-02 | 管理端「③ 退租留档确认」Tab 列表 | 显示 PASS 留档且未确认记录（含租期列） | ✅ 2 条 |
| LS-03 | 普通用户访问 /guarantee/moveout/pending-confirm | 4001 仅银行运营人员可操作 | ✅ |
| LS-04 | 管理端代房东确认无需索赔 | 成功提示；列表减 1 条 | ✅ |
| LS-05 | 确认后用户端状态 | 租后·确认无需索赔（✓ 绿色，完美结束） | ✅ |
| LS-06 | 保函到期后未发起索赔 | 可索赔列表 claimable=false 已过期，不可再发起 | ✅ 承 HS 既有 |
| LS-07 | 未合格留档（REVIEW）不可确认 | 服务侧仅 PASS 可确认 | ✅ 代码校验 |

> 状态机：租期结束 → 无索赔 + PASS 留档 → **租后·待房东确认** → 管理端代房东确认 → **租后·确认无需索赔**（完美结束）。

## IX. 房屋状态命名重构（HS-11 起）

| 编号 | 规则 | 预期 | 结果 |
|---|---|---|---|
| HS-11 | 状态名不携带阶段前缀 | 待入住 / 正常 / 被索赔 / 待确认 / 已确认 / 已过期（无"租期前·"等前缀） | ✅ API+bu |
| HS-12 | 租期后分区仅留档通过审核才归入 | 未提交/未过审留档的租后房屋不显示（houseSituation=null） | ✅ 前端过滤 |
| HS-13 | 租期后 · 保函已过期（expireDate 早于今日） | 已过期（灰色） | ✅ GB2024010001 |
| HS-14 | 租期后 · 留档 PASS + 房东确认 + 未过期 | 已确认（绿色 ✓） | ✅ admin 保函5 |
| HS-15 | 租期前 | 待入住（蓝色） | ✅ 4 张 |

> 状态全集（7 态）：PRE_RENTAL 待入住 / RENTING_NORMAL 正常 / RENTING_CLAIMED 被索赔 / ENDED_PENDING_CONFIRM 待确认 / ENDED_CONFIRMED 已确认 / ENDED_EXPIRED 已过期 / ENDED_CLAIMED 被索赔。
