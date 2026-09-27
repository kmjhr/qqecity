package com.icbc.qingqi.module.chat.support;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * FAQ 50 条演示知识库（模拟）
 * <p>
 * 涵盖：保函/贷款/预算/记账/反诈/政策/征信/保险/理财 9 类
 * 关键词命中后用模板化回答（来源：FAQ#{id}）
 * <p>
 * 演示用，标注"模拟对话引擎/仅供参考"
 */
public final class FaqData {

    private FaqData() {}

    public static final List<FaqItem> FAQ_LIST = Collections.unmodifiableList(build());

    private static List<FaqItem> build() {
        List<FaqItem> list = new ArrayList<>(50);

        // === 保函类（G，6 条） ===
        list.add(new FaqItem(1L, "保函",
                asList("保函", "押金", "房东", "租金押金", "履约保函"),
                "保函是房东与租客之间的履约担保工具，由工行作为开函行（模拟）。"
                        + "青年租客可通过《青启e城》申请，费率按押金金额分档：≤2000→0.8%，≤4000→1.0%，≤6000→1.2%，>6000→1.5%。"
                        + "保函生效后，租客押金可转为储蓄或低风险理财。",
                "/pages/guarantee/apply", "保函申请"));
        list.add(new FaqItem(2L, "保函费率",
                asList("保函费", "费率", "保函多少钱"),
                "保函费按押金金额分档：押金 ≤2000 元费率 0.8%；≤4000 元 1.0%；≤6000 元 1.2%；>6000 元 1.5%（模拟口径，仅供参考）。",
                "/pages/guarantee/fee", "保函费率说明"));
        list.add(new FaqItem(3L, "保函索赔",
                asList("索赔", "房东索赔", "保函赔付"),
                "房东可在保函有效期内发起索赔：状态机 SUBMITTED→AI_REVIEW→(低风险小案 APPROVED / 存疑 DEFENSE_PERIOD→MANUAL_REVIEW)→APPROVED/REJECTED→CLOSED。"
                        + "租客收到申辩期通知后可提交 defense_content 反证。",
                "/pages/guarantee/claim", "保函索赔流程"));
        list.add(new FaqItem(4L, "电子签约",
                asList("电子签约", "签名", "canvas", " landlord sign"),
                "G-2 房东确认环节支持电子签约：Canvas 手写或点击确认，签署时间/签名入库 biz_guarantee_application.sign_content/sign_time。",
                "/pages/guarantee/sign", "房东电子签约"));
        list.add(new FaqItem(5L, "AI 预审",
                asList("AI 预审", "置信度", "人工复核", "MANUAL_REVIEW"),
                "G-3 复审 AI 预审：置信度 <0.6 命中高风险规则时转 MANUAL_REVIEW，不直接 PASS；管理端可查人工复核队列。",
                "/pages/guarantee/ai-review", "AI 预审与人工复核"));
        list.add(new FaqItem(6L, "保函状态",
                asList("保函状态", "申请中", "待确认", "待缴费", "已开立", "已失效"),
                "保函申请状态：申请中→待确认→待缴费→已开立→已失效（或索赔 CLOSED）。状态不允许跳变，每步动作写 sys_message。",
                "/pages/guarantee/status", "保函状态流转"));

        // === 贷款类（L，6 条） ===
        list.add(new FaqItem(7L, "青创e贷",
                asList("青创e贷", "贷款", "创业贷款"),
                "青创e贷分 A/B 双轨：A 类 5 万元循环额度（年化 3.85%，随借随还）；B 类小额定向（受托支付到白名单商户，不经过个人账户）。",
                "/pages/loan/apply", "青创e贷 A/B 双轨"));
        list.add(new FaqItem(8L, "A 类循环贷",
                asList("A 类", "循环贷", "随借随还", "提款", "还款"),
                "A 类循环贷：5 万额度内分次提款、按实际用款天数和 3.85% 年化计息（interest = principal × 3.85% × days / 365），"
                        + "归还后额度自动恢复；提还款流水记录在 biz_credit_txn。",
                "/pages/loan/withdraw", "A 类循环贷提还款"));
        list.add(new FaqItem(9L, "B 转 A 观察期",
                asList("B 转 A", "观察期", "B 类升级", "提额"),
                "B 类预审通过后进入 6 个月观察期（演示支持加速推进）。月度评分=受托支付活跃30+记账收入30+还款行为20+基础分20，"
                        + "达标自动转 A 类并提额；数据不足则维持小额或退出。",
                "/pages/loan/observation", "B 转 A 观察期"));
        list.add(new FaqItem(10L, "受托支付",
                asList("受托支付", "定向", "白名单商户"),
                "受托支付 100% 资金定向打给预置白名单商户，不经过借款人个人账户；非 VERIFIED 商户受托支付返回 2002 错误。",
                "/pages/loan/entrust", "受托支付"));
        list.add(new FaqItem(11L, "商户白名单",
                asList("商户白名单", "灰名单", "白名单审核"),
                "商户状态：PENDING（灰名单）→banker 审核→VERIFIED（白名单）/REJECTED。仅 VERIFIED 商户允许受托支付。",
                "/pages/loan/merchant", "商户白名单管理"));
        list.add(new FaqItem(12L, "贷款预审",
                asList("贷款预审", "B 类预审", "预审"),
                "B 类预审不查征信：10,000-20,000 元额度，按创业计划书 + 人群资质（在校生/应届/青年创业）判定。",
                "/pages/loan/precheck", "B 类预审"));

        // === 预算类（C，5 条） ===
        list.add(new FaqItem(13L, "预算设置",
                asList("预算", "分类预算", "预算设置"),
                "预算按 MCC 码归类扣减：用户为每个分类设置月度预算，交易发生时按 MCC 自动归类并扣减；超支触发三级提醒。",
                "/pages/budget/setting", "分类预算设置"));
        list.add(new FaqItem(14L, "MCC 归类",
                asList("MCC", "mcc", "归类", "扣减"),
                "MCC 码映射：餐饮 5812-5814→FOOD；娱乐 7832/7922→ENTERTAINMENT；购物 5310/5411→SHOPPING；交通 4111/4131→TRANSPORT。"
                        + "命中后扣减对应分类预算。",
                "/pages/budget/mcc", "MCC 归类扣减"));
        list.add(new FaqItem(15L, "预算三级提醒",
                asList("三级提醒", "50%", "20%", "超支", "结转"),
                "预算三级提醒：剩余 ≤50% 温和提醒；≤20% 紧张提醒；超支 100% 预警。每档仅首次触发（remind50Sent/remind20Sent/remindOverSent 去重），"
                        + "超支同步落 biz_risk_warning(BUDGET_OVER)。",
                "/pages/budget/remind", "预算三级提醒"));
        list.add(new FaqItem(16L, "结余转储蓄",
                asList("结余", "转储蓄", "心愿储蓄"),
                "月度结余可一键转入「心愿储蓄」（biz_saving_goal），完成储蓄目标进度。",
                "/pages/budget/transfer", "结余转储蓄"));
        list.add(new FaqItem(17L, "交易记录",
                asList("交易", "交易记录", "支出"),
                "交易记录落 biz_transaction：类型 INCOME/EXPENSE，来源 SIMULATED/BANK_IMPORT，按 MCC 自动归类扣减预算。",
                "/pages/budget/txn", "交易记录"));

        // === 记账类（B，3 条） ===
        list.add(new FaqItem(18L, "记账",
                asList("记账", "记账记录", "bookkeeping"),
                "记账记录落 biz_bookkeeping_record：类型 INCOME/EXPENSE，来源 AUTO（自动识别）/MANUAL（手动录入）/IMPORT（导入）。"
                        + "模糊交易待确认（is_confirmed=0）用于现金流预警的应收未收识别。",
                "/pages/bookkeeping", "记账与现金流"));
        list.add(new FaqItem(19L, "现金流报表",
                asList("现金流", "报表", "记账报表"),
                "按月生成 biz_cashflow_report：总收入/总支出/净现金流/利润/利润率，预警等级 NORMAL/WARNING/CRITICAL。",
                "/pages/bookkeeping/report", "现金流报表"));
        list.add(new FaqItem(20L, "应收未收",
                asList("应收未收", "待确认", "模糊交易"),
                "INCOME + is_confirmed=0 + happenDate 早于今日>7 天 → 现金流预警的应收未收识别依据。",
                "/pages/bookkeeping/pending", "应收未收"));

        // === 反诈类（S，5 条） ===
        list.add(new FaqItem(21L, "刷单诈骗",
                asList("刷单", "刷单诈骗", "兼职刷单"),
                "刷单诈骗特征：先小额返利建立信任→诱导大额垫付→卷款拉黑。任何要求垫付的刷单都是诈骗。",
                "/pages/safety/fraud/1", "刷单诈骗识别"));
        list.add(new FaqItem(22L, "冒充公检法",
                asList("公检法", "冒充公检法", "假冒警察"),
                "公检法不会通过电话办案，更不存在'安全账户'。要求转账到'安全账户'的一律是诈骗。",
                "/pages/safety/fraud/2", "冒充公检法"));
        list.add(new FaqItem(23L, "征信洗白",
                asList("征信洗白", "征信修复", "洗白"),
                "征信由央行统一管理，任何机构/个人无权'洗白'。收费修复征信都是诈骗。",
                "/pages/safety/fraud/3", "征信洗白骗局"));
        list.add(new FaqItem(24L, "情景教学",
                asList("情景教学", "情景模拟", "互动问答"),
                "反诈情景教学：互动问答式拆解（刷单/公检法/征信洗白），选错有纠偏文案，可累积积分进度。",
                "/pages/safety/scenario", "反诈情景教学"));
        list.add(new FaqItem(25L, "骗局甄别",
                asList("骗局甄别", "诈骗识别", "防骗"),
                "骗局甄别规则：要求垫付/安全账户/客服加好友/陌生链接/验证码索要 → 命中即拦截 + 推送反诈内容。",
                "/pages/safety/detect", "骗局甄别拦截"));

        // === 政策类（3 条） ===
        list.add(new FaqItem(26L, "人才安居",
                asList("人才安居", "人才公寓", "安居政策"),
                "人才安居政策：人才公寓、落户补贴、就业扶持，按人群资质（在校生/应届/城市青年）匹配。"
                        + "申报入口见「政策匹配」页（biz_policy，8 条演示数据）。",
                "/pages/policy/match", "人才安居政策匹配"));
        list.add(new FaqItem(27L, "创业贴息",
                asList("创业贴息", "创业担保贷款", "贴息"),
                "创业贴息政策：创业担保贷款个人最高 30 万、部分地区 50 万、贴息；个体工商户税费优惠。"
                        + "按经营画像 + 人群资质匹配推送。",
                "/pages/policy/match", "创业贴息政策推送"));
        list.add(new FaqItem(28L, "个转企",
                asList("个转企", "个体工商户", "转企业"),
                "个转企条件自查：6 项自查（年营收/员工数/行业资质/纳税/商标/经营场所），达标引导流程。",
                "/pages/operation/finance/individual-to-company", "个转企引导"));

        // === 征信类（4 条） ===
        list.add(new FaqItem(29L, "征信报告解读",
                asList("征信", "征信报告", "信用报告"),
                "征信报告智能解读：信用分/等级、逾期笔数与原因、查询次数、负债率逐项评价与改进建议。"
                        + "演示报告 2 份（良好/有瑕疵），biz_credit_report.source=SIMULATED。",
                "/pages/safety/credit-report", "征信报告解读"));
        list.add(new FaqItem(30L, "逾期风险预判",
                asList("逾期", "逾期风险", "预判"),
                "逾期风险预判：还款日历（贷款/受托支付/保函费）+ 收支规则评分。命中落 biz_risk_warning(OVERDUE_RISK)，提前 N 天预警。",
                "/pages/safety/overdue", "逾期风险预判"));
        list.add(new FaqItem(31L, "征信健康",
                asList("征信健康", "健康分", "改善"),
                "征信健康分（复用 credit_score）+ 改善清单：还清逾期、减少查询、规范还款。"
                        + "可一键模拟修复路径（标注不产生真实征信影响）。",
                "/pages/safety/credit-health", "征信健康管理"));
        list.add(new FaqItem(32L, "常态化监测",
                asList("征信监测", "软查询", "常态化"),
                "常态化征信监测：用户授权后软查询（SIMULATED，不产生硬查询），异常借贷/逾期落 biz_risk_warning(CREDIT_ABNORMAL)。",
                "/pages/consumption/credit-monitor", "常态化征信监测"));

        // === 保险类（3 条） ===
        list.add(new FaqItem(33L, "履约保证保险",
                asList("履约保险", "保证保险", "履约保证"),
                "履约保证保险：覆盖合同履约风险，工行仅代销、不承保。演示产品 4 条（履约保证 1 + 知识产权 2 + 财产综合 1）。",
                "/pages/operation/insurance", "保险代销"));
        list.add(new FaqItem(34L, "知识产权保险",
                asList("知识产权保险", "专利执行", "侵权责任"),
                "知识产权保险：专利执行险 / 侵权责任险，按 IP 经营场景推荐。",
                "/pages/operation/insurance", "知识产权保险"));
        list.add(new FaqItem(35L, "保险代销合规",
                asList("保险代销", "代销", "工行代销"),
                "保险代销合规口径：工行仅代销、不承保；apply_url 为模拟跳转链接；演示用，不构成真实投保邀约。",
                "/pages/operation/insurance", "保险代销合规"));

        // === 理财类（4 条） ===
        list.add(new FaqItem(36L, "风险测评",
                asList("风险测评", "测评问卷", "测评"),
                "风险测评 10 题 → 总分 10-40：≤18 CONSERVATIVE（保守），19-30 STEADY（稳健），31-40 BALANCED（平衡）。"
                        + "未完成测评不允许进理财推荐；测评 1 年有效。",
                "/pages/consumption/risk-assessment", "风险测评"));
        list.add(new FaqItem(37L, "低风险理财",
                asList("理财", "低风险", "稳健理财"),
                "理财仅推荐低风险（R1/R2）产品：心愿储蓄/现金管理/短债/基金定投/积存金，按风险等级匹配。"
                        + "理财非存款、产品有风险。",
                "/pages/consumption/finance-product", "低风险理财推荐"));
        list.add(new FaqItem(38L, "高频借贷预警",
                asList("高频借贷", "以贷养贷", "多平台"),
                "高频借贷识别：多平台借贷/短周期多次借款/以贷养贷特征（演示数据）。命中落 biz_risk_warning(HIGH_FREQ_BORROW)。",
                "/pages/consumption/high-freq", "高频借贷预警"));
        list.add(new FaqItem(39L, "三层消费引导",
                asList("消费引导", "三层引导", "支付前提醒"),
                "三层渐进式消费引导：① 交易后即时推送；② 月度账单分析；③ 支付前实时提醒（仅工行自有支付场景）。",
                "/pages/consumption/guide", "三层消费引导"));

        // === 信用画像（4 条） ===
        list.add(new FaqItem(40L, "青年成长信用画像",
                asList("信用画像", "成长画像", "三维评分"),
                "青年成长信用画像：三场景聚合（安居/创业/消费）→ 稳定性/经营力/资金健康度三维评分 + 成长轨迹。"
                        + "联通逻辑：安居稳 + 经营好 → 授信提额、利率优惠。",
                "/pages/profile", "青年成长信用画像"));
        list.add(new FaqItem(41L, "稳定性评分",
                asList("稳定性", "安居评分", "保函履约"),
                "稳定性评分（安居场景）：保函履约记录 + 租金支付记录。保函 ACTIVE 且无索赔 = 满分基础。",
                "/pages/profile", "稳定性维度"));
        list.add(new FaqItem(42L, "经营力评分",
                asList("经营力", "创业评分", "经营流水"),
                "经营力评分（创业场景）：经营流水 + 受托支付 + 记账数据。受托支付活跃度 + 收入规模 + 记账规范性综合评分。",
                "/pages/profile", "经营力维度"));
        list.add(new FaqItem(43L, "资金健康度",
                asList("资金健康度", "消费评分", "预算执行"),
                "资金健康度评分（消费场景）：预算执行率 + 结余储蓄率 + 借贷行为（无逾期/无高频借贷）。",
                "/pages/profile", "资金健康度维度"));

        // === 智能中台（4 条） ===
        list.add(new FaqItem(44L, "对话引擎",
                asList("对话引擎", "AI 客服", "智能客服"),
                "对话引擎双模式：local（默认，离线可用，FAQ 50 条 + 政策库 + 反诈库关键词召回）"
                        + "+ agent（可选，需 API Key，本地 RAG 先召回拼入上下文，LLM 仅组织语言，回答仍附本地来源）。",
                "/pages/chat", "对话引擎双模式"));
        list.add(new FaqItem(45L, "流水聚合",
                asList("流水聚合", "多渠道", "授权"),
                "多渠道流水聚合：用户授权勾选（工行收款码/微信/支付宝/淘宝店）→ 聚合经营流水，来源标注'已授权聚合'，支持解绑。",
                "/pages/cashflow/aggregate", "多渠道流水聚合"));
        list.add(new FaqItem(46L, "防刷单",
                asList("防刷单", "刷单识别", "三道防线"),
                "防刷单三道防线：① 异常特征（集中对手/凌晨高频/金额雷同/退款率异常）；② 跨维比对（物料采购/摊位活动 vs 流水）；"
                        + "③ 平台直取禁截图。命中输出风险分与处置建议。",
                "/pages/cashflow/anti-brush", "防刷单三道防线"));
        list.add(new FaqItem(47L, "消息中心",
                asList("消息中心", "未读", "站内信"),
                "消息中心：申请/确认/出函/索赔/预算提醒/政策推送 全部自动写 sys_message，未读数量实时统计。",
                "/pages/message", "消息中心"));

        // === 平台通用（3 条） ===
        list.add(new FaqItem(48L, "登录与权限",
                asList("登录", "权限", "ADMIN", "banker"),
                "用户角色：USER（青年用户）/ ADMIN（管理员）/ BANKER（银行业务员）。admin-web 接口校验 ADMIN/banker 角色。"
                        + "Token 鉴权 Authorization: Bearer <JWT>，2 小时有效。",
                "/login", "登录与权限"));
        list.add(new FaqItem(49L, "演示账号",
                asList("演示账号", "测试账号", "密码"),
                "演示账号：admin / testuser / entrepreneur / landlord01 / banker01，密码统一 123456。"
                        + "所有银行能力用模拟桩，不查真实征信。",
                "/login", "演示账号"));
        list.add(new FaqItem(50L, "模拟口径",
                asList("模拟", "演示", "工行杯"),
                "《青启e城》为工行杯参赛演示系统，所有银行能力用模拟桩并标注'模拟'：不查真实征信、放款/保函开立/赔付全部模拟；"
                        + "保险代销仅展示与跳转；理财推荐仅限低风险产品且强制风险测评。",
                "/", "模拟口径声明"));

        return list;
    }

    private static List<String> asList(String... arr) {
        List<String> list = new ArrayList<>(arr.length);
        for (String s : arr) {
            list.add(s.toLowerCase());
        }
        return list;
    }

    /**
     * FAQ 条目
     */
    public static class FaqItem {
        public final Long id;
        public final String title;
        public final List<String> keywords;
        public final String answer;
        public final String url;
        public final String sourceLabel;

        public FaqItem(Long id, String title, List<String> keywords,
                       String answer, String url, String sourceLabel) {
            this.id = id;
            this.title = title;
            this.keywords = keywords;
            this.answer = answer;
            this.url = url;
            this.sourceLabel = sourceLabel;
        }
    }
}
