package com.icbc.qingqi.module.safety.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.icbc.qingqi.module.safety.dto.FraudDetectDTO;
import com.icbc.qingqi.module.safety.entity.BizAntiFraudContent;
import com.icbc.qingqi.module.safety.entity.BizFraudDetectionLog;
import com.icbc.qingqi.module.safety.mapper.BizAntiFraudContentMapper;
import com.icbc.qingqi.module.safety.mapper.BizAntiFraudAlertMapper;
import com.icbc.qingqi.module.safety.mapper.BizFraudDetectionLogMapper;
import com.icbc.qingqi.module.safety.entity.BizAntiFraudAlert;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

/**
 * 青年金融安全服务
 * <p>
 * 覆盖 S-1 ~ S-2：
 * - S-1 反诈内容列表
 * - S-2 骗局甄别（关键词规则引擎，命中即拦截警示并落库）
 */
@Slf4j
@Service
public class SafetyService {

    private final BizAntiFraudContentMapper contentMapper;
    private final BizAntiFraudAlertMapper alertMapper;
    private final BizFraudDetectionLogMapper detectionLogMapper;

    // 高危话术关键词 → DANGEROUS
    private static final List<String> DANGEROUS_KEYWORDS = List.of(
            "征信修复", "征信洗白", "征信铲单", "消除不良记录", "消除征信",
            "安全账户", "资金核查", "涉嫌洗钱",
            "刷单", "刷信誉", "刷销量",
            "裸条", "裸贷",
            "内部渠道", "内部人脉", "特殊关系",
            "洗白",
            // 冒充运营商/开通诱导
            "免费开通", "业务开通提醒", "人工转", "二线客服",
            "退订请拨打", "下载客户端", "下载APP", "客服转接",
            "停机", "已停用", "注销",
            // 快递包裹/礼品诱导（伪基站群发，回复R标记活跃用户）
            "包裹派送", "派送中", "取件码", "拒收请回复",
            // 积分清零/紧迫威胁（诱导点击钓鱼链接）
            "积分清零", "积分过期", "即将失效", "即将清零", "过期清零",
            "逾期作废", "今日内", "最后期限", "作废"
    );

    // 可疑话术关键词 → SUSPICIOUS
    private static final List<String> SUSPICIOUS_KEYWORDS = List.of(
            "验证码", "短信验证码",
            "保证金", "解冻费", "押金入职", "先交押金",
            "中奖", "缴纳税费", "领取奖品",
            "翻倍", "稳赚不赔", "高额回报",
            "零门槛", "秒到账", "黑户可贷",
            // 快递/礼品诱导弱特征
            "领取", "已发", "到付", "抽奖", "赠品", "秘籍", "年终盛典", "扫码进群",
            // 积分/营销类（配合链接）
            "兑换", "兑换礼品", "回馈",
            // 出行/退款类
            "改签", "退票", "理赔", "退款", "全额退",
            // 贷款授信类
            "授信额度", "额度已批", "放款", "下款",
            // 冒充客服/联系方式
            "加微信", "添加微信", "点击链接", "回复TD"
    );

    public SafetyService(BizAntiFraudContentMapper contentMapper,
                         BizAntiFraudAlertMapper alertMapper,
                         BizFraudDetectionLogMapper detectionLogMapper) {
        this.contentMapper = contentMapper;
        this.alertMapper = alertMapper;
        this.detectionLogMapper = detectionLogMapper;
    }

    // ============================================================
    //  S-1 反诈内容
    // ============================================================

    public List<BizAntiFraudContent> listAntiFraud(String category, String contentType) {
        LambdaQueryWrapper<BizAntiFraudContent> wrapper = new LambdaQueryWrapper<BizAntiFraudContent>()
                .eq(BizAntiFraudContent::getStatus, 1)
                .orderByAsc(BizAntiFraudContent::getSortOrder);
        if (category != null && !category.isEmpty()) {
            wrapper.eq(BizAntiFraudContent::getCategory, category);
        }
        if (contentType != null && !contentType.isEmpty()) {
            wrapper.eq(BizAntiFraudContent::getContentType, contentType);
        }
        return contentMapper.selectList(wrapper);
    }

    public BizAntiFraudContent getAntiFraudDetail(Long id) {
        BizAntiFraudContent content = contentMapper.selectById(id);
        if (content == null) {
            throw new com.icbc.qingqi.common.BizException(com.icbc.qingqi.common.ErrorCode.BIZ_RULE_NOT_MET, "反诈内容不存在");
        }
        // 浏览量 +1
        content.setViewCount(content.getViewCount() + 1);
        contentMapper.updateById(content);
        return content;
    }

    // ============================================================
    //  S-2 骗局甄别
    // ============================================================

    @Transactional(rollbackFor = Exception.class)
    public BizFraudDetectionLog detectFraud(Long userId, FraudDetectDTO dto) {
        String text = dto.getInputText();
        List<String> hitDangerous = new ArrayList<>();
        List<String> hitSuspicious = new ArrayList<>();

        for (String kw : DANGEROUS_KEYWORDS) {
            if (text.contains(kw)) hitDangerous.add(kw);
        }
        for (String kw : SUSPICIOUS_KEYWORDS) {
            if (text.contains(kw)) hitSuspicious.add(kw);
        }

        // 复合模式识别：冒充运营商/威胁恐吓 + 索要敏感信息 → 高危（命中即 DANGEROUS）
        hitDangerous.addAll(detectSocialPatterns(text));

        // 短链接/钓鱼链接特征：http(s) 链接，或 短域名+路径（n5a.cn/KaEOyO、abcd.top/xxx 等）
        boolean hasUrl = containsSuspiciousUrl(text);

        String result;
        int riskLevel;
        String warning;
        List<String> allHits = new ArrayList<>();

        if (!hitDangerous.isEmpty()) {
            result = "DANGEROUS";
            riskLevel = 5;
            allHits.addAll(hitDangerous);
            allHits.addAll(hitSuspicious);
            if (hasUrl) allHits.add("含外链");
            warning = buildDangerousWarning(hitDangerous, text);
        } else if (!hitSuspicious.isEmpty() || hasUrl) {
            result = "SUSPICIOUS";
            riskLevel = 3;
            allHits.addAll(hitSuspicious);
            if (hasUrl) allHits.add("含外链");
            warning = "可疑话术！检测到关键词：" + String.join("、", allHits)
                    + "。此类话术常被用于诈骗，请提高警惕，切勿点击陌生链接、提供个人信息或转账。【模拟识别】";
        } else {
            result = "SAFE";
            riskLevel = 1;
            warning = "未检测到常见诈骗话术，该文本暂未发现明显风险。请注意：本结果为规则引擎模拟判断，仅供参考。";
        }

        BizFraudDetectionLog log = new BizFraudDetectionLog();
        log.setUserId(userId);
        log.setInputText(text);
        log.setDetectResult(result);
        log.setRiskLevel(riskLevel);
        log.setMatchedRules(allHits.toString());
        log.setWarningContent(warning);
        log.setDetectTime(LocalDateTime.now());
        detectionLogMapper.insert(log);

        return log;
    }

    private String buildDangerousWarning(List<String> hits, String text) {
        if (hits.stream().anyMatch(h -> h.contains("征信") || h.equals("洗白"))) {
            return "高度疑似「征信修复/洗白」骗局！征信领域不存在\"修复\"\"洗白\"\"铲单\"概念，任何声称可以花钱消除不良记录的都是诈骗。正规渠道是向人民银行征信中心提出异议申请，且不收取任何费用。【模拟识别】";
        }
        if (hits.stream().anyMatch(h -> h.contains("刷单"))) {
            return "高度疑似「刷单诈骗」！所有要求先交钱的刷单兼职都是诈骗，刷单本身也是违法行为，切勿参与。【模拟识别】";
        }
        if (hits.stream().anyMatch(h -> h.contains("开通") || h.contains("人工转")
                || h.contains("二线客服") || h.contains("退订") || h.contains("下载") || h.contains("客服转接"))) {
            return "高度疑似「冒充运营商」诈骗！运营商不会主动免费开通业务并诱导下载客户端/转人工客服。凡要求下载不明APP、拨打客服退订的，请先通过官方渠道（10086/10010/10000）核实，切勿点击陌生链接。【模拟识别】";
        }
        if (hits.stream().anyMatch(h -> h.contains("包裹") || h.contains("派送") || h.contains("取件")
                || h.contains("拒收") || h.contains("领取"))) {
            return "高度疑似「快递包裹/礼品诱导」诈骗！伪基站冒充快递群发，短链接多为钓鱼引流，回复R会标记活跃号再接连环诈骗。请勿点击链接或回复，直接删除。【模拟识别】";
        }
        if (hits.stream().anyMatch(h -> h.contains("积分") || h.contains("清零") || h.contains("过期")
                || h.contains("失效") || h.contains("作废"))) {
            return "高度疑似「积分清零/兑换」诈骗！运营商积分不会无故清零，凡要求点击链接兑换、补差价的就是钓鱼。请通过官方APP核实，勿点短信链接。【模拟识别】";
        }
        if (hits.stream().anyMatch(h -> h.contains("ETC") || h.contains("医保") || h.contains("社保")
                || h.contains("停用") || h.contains("锁定") || h.contains("注销"))) {
            return "高度疑似「冒充ETC/医保」诈骗！ETC/医保卡不会因过期停用要求点击链接认证。请通过官方小程序/服务号核实，切勿点击短信链接填写信息。【模拟识别】";
        }
        if (hits.stream().anyMatch(h -> h.contains("航班") || h.contains("改签") || h.contains("退票"))) {
            return "高度疑似「机票退改签」诈骗！航空公司不会以短信要求拨打400电话改签理赔。请通过官方APP/客服核实航班状态，勿拨打短信内电话。【模拟识别】";
        }
        if (hits.stream().anyMatch(h -> h.contains("授信") || h.contains("额度") || h.contains("放款") || h.contains("下款"))) {
            return "高度疑似「虚假贷款」诈骗！正规贷款不会通过短信链接发放，凡要求先交保证金/解冻费的一律是诈骗。请通过银行官方渠道申请。【模拟识别】";
        }
        if (hits.contains("安全账户") || hits.contains("资金核查") || hits.contains("涉嫌洗钱")) {
            return "高度疑似「冒充公检法」诈骗！公检法机关绝无\"安全账户\"概念，也不会要求转账核查。请立即挂断并拨打110。【模拟识别】";
        }
        if (hits.contains("冒充运营商+威胁+索要个人信息") || hits.contains("威胁话术+索要身份证号码")) {
            return "高度疑似「冒充运营商/官方机构」诈骗！正规运营商不会通过短信索要您的身份证号码、姓名等个人信息，也不会以暂停服务相要挟。请勿回复，通过官方客服电话核实，必要时拨打96110/110报警。【模拟识别】";
        }
        if (hits.contains("索要身份证号码") || hits.contains("索要银行卡号") || hits.contains("索要支付/验证信息")) {
            return "高度警惕！该短信疑似诱导您提供身份证号码/银行卡号等敏感信息，正规机构不会通过短信索取。切勿回复，更不要提供个人信息或点击任何链接。【模拟识别】";
        }
        return "高度疑似诈骗话术！命中关键词：" + String.join("、", hits) + "，请切勿相信，更不要转账或提供个人信息。【模拟识别】";
    }

    /**
     * 复合模式识别：冒充运营商/威胁恐吓 + 索要敏感信息 → 高危
     * 单关键词太脆（正常短信也会出现"身份证""暂停"），组合命中才判高危
     */
    private List<String> detectSocialPatterns(String text) {
        List<String> hits = new ArrayList<>();
        boolean operator = text.contains("中国电信") || text.contains("中国移动") || text.contains("中国联通")
                || text.contains("10086") || text.contains("10000") || text.contains("10010")
                || text.contains("电信") || text.contains("移动") || text.contains("联通");
        boolean threat = text.contains("暂停") || text.contains("停用") || text.contains("封停")
                || text.contains("停机") || text.contains("冻结") || text.contains("注销")
                || text.contains("涉嫌") || text.contains("违法") || text.contains("报案");
        boolean askId = text.contains("身份证号码") || text.contains("身份证号");
        boolean askSensitive = askId || text.contains("银行卡号") || text.contains("支付密码")
                || text.contains("验证码");
        boolean replyAct = text.contains("回复") || text.contains("发送") || text.contains("提供")
                || text.contains("申请复核");

        if (operator && threat && askSensitive) {
            hits.add("冒充运营商+威胁+索要个人信息");
        } else if (threat && askId && replyAct) {
            hits.add("威胁话术+索要身份证号码");
        } else if (askId && replyAct) {
            hits.add("索要身份证号码");
        } else if (text.contains("银行卡号") && replyAct) {
            hits.add("索要银行卡号");
        } else if (text.contains("支付密码") || (text.contains("验证码") && replyAct)) {
            hits.add("索要支付/验证信息");
        }
        return hits;
    }

    /** 短链接/钓鱼链接特征：http(s) 链接，或 短域名+路径（n5a.cn/KaEOyO、abcd.top/xxx 等） */
    private boolean containsSuspiciousUrl(String text) {
        if (text == null || text.isBlank()) return false;
        String lower = text.toLowerCase();
        if (lower.contains("http://") || lower.contains("https://")) return true;
        return java.util.regex.Pattern
                .compile("[a-z0-9][a-z0-9-]{1,10}\\.(cn|com|top|xyz|cc|vip|net|io|icu|site)/[a-z0-9]{4,}")
                .matcher(lower)
                .find();
    }

    // ============================================================
    //  S-3 实时反诈预警（人工维护·模拟实时，安全教育平台风格）
    // ============================================================

    public List<BizAntiFraudAlert> listAlerts(String level, String scene) {
        LambdaQueryWrapper<BizAntiFraudAlert> wrapper = new LambdaQueryWrapper<BizAntiFraudAlert>()
                .eq(BizAntiFraudAlert::getStatus, 1)
                .orderByDesc(BizAntiFraudAlert::getPublishTime);
        if (level != null && !level.isEmpty()) {
            wrapper.eq(BizAntiFraudAlert::getAlertLevel, level);
        }
        if (scene != null && !scene.isEmpty()) {
            wrapper.eq(BizAntiFraudAlert::getRelateScene, scene);
        }
        return alertMapper.selectList(wrapper);
    }

    // ============================================================
    //  S-4 典型反诈案例（固定区，与青启e城业务强关联的前置）
    // ============================================================

    /** 与平台业务关联性强的分类置前：保函租房 → 征信 → 创业贷 → 理财 → 客服 → 通用高发 */
    private static final List<String> FEATURED_CATEGORY_ORDER = List.of(
            "租房诈骗", "征信修复", "创业贷款", "虚假投资", "冒充客服", "校园贷", "套路贷",
            "刷单诈骗", "冒充公检法", "AI换脸");

    public List<BizAntiFraudContent> featuredCases() {
        List<BizAntiFraudContent> all = contentMapper.selectList(new LambdaQueryWrapper<BizAntiFraudContent>()
                .eq(BizAntiFraudContent::getStatus, 1)
                .in(BizAntiFraudContent::getContentType, "ARTICLE", "CASE"));
        Map<String, Integer> rank = new HashMap<>();
        for (int i = 0; i < FEATURED_CATEGORY_ORDER.size(); i++) {
            rank.put(FEATURED_CATEGORY_ORDER.get(i), i);
        }
        all.sort(Comparator.comparingInt(
                (BizAntiFraudContent c) -> rank.getOrDefault(c.getCategory(), 100))
                .thenComparing(BizAntiFraudContent::getSortOrder));
        return all.stream().limit(8).collect(java.util.stream.Collectors.toList());
    }

}