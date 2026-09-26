package com.icbc.qingqi.module.safety.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.icbc.qingqi.module.safety.dto.FraudDetectDTO;
import com.icbc.qingqi.module.safety.entity.BizAntiFraudContent;
import com.icbc.qingqi.module.safety.entity.BizFraudDetectionLog;
import com.icbc.qingqi.module.safety.mapper.BizAntiFraudContentMapper;
import com.icbc.qingqi.module.safety.mapper.BizFraudDetectionLogMapper;
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
    private final BizFraudDetectionLogMapper detectionLogMapper;

    // 高危话术关键词 → DANGEROUS
    private static final List<String> DANGEROUS_KEYWORDS = List.of(
            "征信修复", "征信洗白", "征信铲单", "消除不良记录", "消除征信",
            "安全账户", "资金核查", "涉嫌洗钱",
            "刷单", "刷信誉", "刷销量",
            "裸条", "裸贷",
            "内部渠道", "内部人脉", "特殊关系",
            "洗白"
    );

    // 可疑话术关键词 → SUSPICIOUS
    private static final List<String> SUSPICIOUS_KEYWORDS = List.of(
            "验证码", "短信验证码",
            "保证金", "解冻费", "押金入职", "先交押金",
            "中奖", "缴纳税费", "领取奖品",
            "翻倍", "稳赚不赔", "高额回报",
            "零门槛", "秒到账", "黑户可贷"
    );

    public SafetyService(BizAntiFraudContentMapper contentMapper,
                         BizFraudDetectionLogMapper detectionLogMapper) {
        this.contentMapper = contentMapper;
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

        String result;
        int riskLevel;
        String warning;
        List<String> allHits = new ArrayList<>();

        if (!hitDangerous.isEmpty()) {
            result = "DANGEROUS";
            riskLevel = 5;
            allHits.addAll(hitDangerous);
            allHits.addAll(hitSuspicious);
            warning = buildDangerousWarning(hitDangerous, text);
        } else if (!hitSuspicious.isEmpty()) {
            result = "SUSPICIOUS";
            riskLevel = 3;
            allHits.addAll(hitSuspicious);
            warning = "可疑话术！检测到关键词：" + String.join("、", hitSuspicious)
                    + "。此类话术常被用于诈骗，请提高警惕，切勿提供个人信息或转账。【模拟识别】";
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
        if (hits.contains("安全账户") || hits.contains("资金核查") || hits.contains("涉嫌洗钱")) {
            return "高度疑似「冒充公检法」诈骗！公检法机关绝无\"安全账户\"概念，也不会要求转账核查。请立即挂断并拨打110。【模拟识别】";
        }
        return "高度疑似诈骗话术！命中关键词：" + String.join("、", hits) + "，请切勿相信，更不要转账或提供个人信息。【模拟识别】";
    }
}
