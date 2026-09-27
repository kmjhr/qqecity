package com.icbc.qingqi.module.cashflow.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.icbc.qingqi.module.bookkeeping.entity.BizBookkeepingRecord;
import com.icbc.qingqi.module.bookkeeping.mapper.BizBookkeepingRecordMapper;
import com.icbc.qingqi.module.cashflow.dto.AggregateTxnVO;
import com.icbc.qingqi.module.cashflow.dto.AntiBrushResultVO;
import com.icbc.qingqi.module.loan.entity.BizEntrustPayment;
import com.icbc.qingqi.module.loan.mapper.BizEntrustPaymentMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 防刷单三道防线规则引擎（演示级，模拟）
 * <p>
 * 防线一·异常特征：① 交易集中少数对手；② 凌晨高频（00:00-05:00）；③ 金额雷同（同金额出现≥5次）；④ 退款率异常（>15%）
 * 防线二·跨维比对：物料采购（受托支付 MATERIAL 商户）+ 摊位活动记录（记账 EXPENSE）vs 流水收入，偏差>30% 视为存疑
 * 防线三·平台直取：演示入口标注"平台直取禁截图"，仅返回标识不返回原始数据
 * <p>
 * 命中输出风险分（0-100）与处置建议，全程"模拟"口径
 */
@Slf4j
@Service
public class AntiBrushService {

    private static final double REFUND_RATE_THRESHOLD = 0.15;
    private static final int LATE_NIGHT_HOURS_THRESHOLD = 5;
    private static final int SAME_AMOUNT_TIMES_THRESHOLD = 5;
    private static final int COUNTERPARTY_CONCENTRATION_THRESHOLD = 3;
    private static final double CROSS_DEVIATION_THRESHOLD = 0.30;

    private final CashflowAggregateService aggregateService;
    private final BizEntrustPaymentMapper entrustMapper;
    private final BizBookkeepingRecordMapper bookkeepingMapper;

    public AntiBrushService(CashflowAggregateService aggregateService,
                            BizEntrustPaymentMapper entrustMapper,
                            BizBookkeepingRecordMapper bookkeepingMapper) {
        this.aggregateService = aggregateService;
        this.entrustMapper = entrustMapper;
        this.bookkeepingMapper = bookkeepingMapper;
    }

    public AntiBrushResultVO detect(Long userId) {
        List<AggregateTxnVO> txns = aggregateService.loadRaw(userId);
        AntiBrushResultVO vo = new AntiBrushResultVO();
        vo.setUserId(userId);
        vo.setSimulated(true);
        if (txns == null || txns.isEmpty()) {
            vo.setHit(false);
            vo.setRiskScore(0);
            vo.setRiskLevel("LOW");
            vo.setRuleHits(new ArrayList<>());
            vo.setAdvices(List.of("未授权聚合流水，无法检测。请先在「多渠道流水聚合」页授权。"));
            return vo;
        }

        List<AntiBrushResultVO.RuleHit> hits = new ArrayList<>();

        // ========= 防线一·异常特征 =========
        // ① 交易集中少数对手
        Map<String, Long> counterpartyCount = txns.stream()
                .filter(t -> t.getCounterparty() != null)
                .collect(Collectors.groupingBy(AggregateTxnVO::getCounterparty, Collectors.counting()));
        int maxCounterpartyCount = counterpartyCount.values().stream().max(Long::compareTo).orElse(0L).intValue();
        int uniqueCounterparties = counterpartyCount.size();
        double concentration = txns.size() > 0 ? (double) maxCounterpartyCount / txns.size() : 0;
        if (uniqueCounterparties <= COUNTERPARTY_CONCENTRATION_THRESHOLD && concentration > 0.5) {
            AntiBrushResultVO.RuleHit h = new AntiBrushResultVO.RuleHit();
            h.setDefenseLine(1);
            h.setRuleName("交易集中少数对手");
            h.setHitDesc("对手方仅 " + uniqueCounterparties + " 个，最大占比 "
                    + (concentration * 100) + "%，疑似自刷");
            h.setDeductedScore(25);
            h.setSamples(counterpartyCount.entrySet().stream()
                    .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                    .limit(3)
                    .map(e -> e.getKey() + ":" + e.getValue() + "笔")
                    .collect(Collectors.toList()));
            hits.add(h);
        }

        // ② 凌晨高频（00:00-05:00）
        long lateNightCount = txns.stream()
                .filter(t -> t.getTxnTime() != null)
                .filter(t -> {
                    int h = t.getTxnTime().getHour();
                    return h >= 0 && h < LATE_NIGHT_HOURS_THRESHOLD;
                })
                .count();
        double lateNightRate = txns.size() > 0 ? (double) lateNightCount / txns.size() : 0;
        if (lateNightRate > 0.3) {
            AntiBrushResultVO.RuleHit h = new AntiBrushResultVO.RuleHit();
            h.setDefenseLine(1);
            h.setRuleName("凌晨高频交易");
            h.setHitDesc("凌晨(00:00-05:00)交易占比 "
                    + (lateNightRate * 100) + "%，疑似异常活跃");
            h.setDeductedScore(20);
            h.setSamples(List.of(lateNightCount + "笔凌晨交易 / 共" + txns.size() + "笔"));
            hits.add(h);
        }

        // ③ 金额雷同（同金额出现≥5次）
        Map<BigDecimal, Long> amountCount = txns.stream()
                .filter(t -> "INCOME".equals(t.getTxnType()))
                .collect(Collectors.groupingBy(AggregateTxnVO::getAmount, Collectors.counting()));
        List<Map.Entry<BigDecimal, Long>> duplicateAmounts = amountCount.entrySet().stream()
                .filter(e -> e.getValue() >= SAME_AMOUNT_TIMES_THRESHOLD)
                .collect(Collectors.toList());
        if (!duplicateAmounts.isEmpty()) {
            AntiBrushResultVO.RuleHit h = new AntiBrushResultVO.RuleHit();
            h.setDefenseLine(1);
            h.setRuleName("金额雷同");
            h.setHitDesc(duplicateAmounts.size() + " 个金额出现≥"
                    + SAME_AMOUNT_TIMES_THRESHOLD + "次，疑似脚本批量刷单");
            h.setDeductedScore(20);
            h.setSamples(duplicateAmounts.stream()
                    .limit(3)
                    .map(e -> e.getKey() + "元 × " + e.getValue() + "次")
                    .collect(Collectors.toList()));
            hits.add(h);
        }

        // ④ 退款率异常（>15%）
        long incomeCount = txns.stream().filter(t -> "INCOME".equals(t.getTxnType())).count();
        long refundCount = txns.stream().filter(t -> "REFUND".equals(t.getTxnType())).count();
        double refundRate = incomeCount > 0 ? (double) refundCount / incomeCount : 0;
        if (refundRate > REFUND_RATE_THRESHOLD) {
            AntiBrushResultVO.RuleHit h = new AntiBrushResultVO.RuleHit();
            h.setDefenseLine(1);
            h.setRuleName("退款率异常");
            h.setHitDesc("退款率 " + (refundRate * 100) + "% 超阈值 "
                    + (REFUND_RATE_THRESHOLD * 100) + "%，疑似刷单后退款");
            h.setDeductedScore(15);
            h.setSamples(List.of(refundCount + "笔退款 / " + incomeCount + "笔收入"));
            hits.add(h);
        }

        // ========= 防线二·跨维比对 =========
        // 物料采购总额（来自受托支付：SUCCESS 的支付总额）
        List<BizEntrustPayment> payments = entrustMapper.selectList(
                new LambdaQueryWrapper<BizEntrustPayment>()
                        .eq(BizEntrustPayment::getUserId, userId)
                        .eq(BizEntrustPayment::getPaymentStatus, "SUCCESS"));
        BigDecimal materialPurchase = payments.stream()
                .map(BizEntrustPayment::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        Long stallCountLong = bookkeepingMapper.selectCount(
                new LambdaQueryWrapper<BizBookkeepingRecord>()
                        .eq(BizBookkeepingRecord::getUserId, userId)
                        .eq(BizBookkeepingRecord::getRecordType, "EXPENSE"));
        int stallActivityCount = stallCountLong != null ? stallCountLong.intValue() : 0;

        BigDecimal aggregatedIncome = txns.stream()
                .filter(t -> "INCOME".equals(t.getTxnType()))
                .map(AggregateTxnVO::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        AntiBrushResultVO.CrossCheckResult cross = new AntiBrushResultVO.CrossCheckResult();
        cross.setMaterialPurchaseTotal(materialPurchase.toPlainString());
        cross.setStallActivityCount(stallActivityCount);
        cross.setAggregatedIncome(aggregatedIncome.toPlainString());

        BigDecimal expectedIncome = materialPurchase.multiply(new BigDecimal("1.2")); // 期望毛收入
        BigDecimal deviation = expectedIncome.compareTo(BigDecimal.ZERO) > 0
                ? aggregatedIncome.subtract(expectedIncome).multiply(new BigDecimal("100"))
                    .divide(expectedIncome, 0, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;
        int deviationPct = deviation.intValue();
        cross.setDeviationPercent(Math.abs(deviationPct));
        if (Math.abs(deviationPct) > CROSS_DEVIATION_THRESHOLD * 100) {
            cross.setConclusion("聚合流水收入与物料采购规模偏差 "
                    + Math.abs(deviationPct) + "%（超 30% 阈值），存在流水虚高或物料未实际投入嫌疑");
            AntiBrushResultVO.RuleHit h = new AntiBrushResultVO.RuleHit();
            h.setDefenseLine(2);
            h.setRuleName("跨维比对偏差");
            h.setHitDesc("聚合流水(" + aggregatedIncome + "元) vs 期望收入(" + expectedIncome + "元)，偏差"
                    + Math.abs(deviationPct) + "%");
            h.setDeductedScore(25);
            h.setSamples(List.of("物料采购=" + materialPurchase + "元 / " + payments.size() + "笔",
                    "记账支出=" + stallActivityCount + "条",
                    "聚合收入=" + aggregatedIncome + "元"));
            hits.add(h);
        } else {
            cross.setConclusion("聚合流水与物料采购规模匹配，偏差 " + Math.abs(deviationPct) + "% 在 30% 阈值内");
        }
        vo.setCrossCheck(cross);

        // ========= 防线三·平台直取 =========
        // 演示入口标注"平台直取禁截图"，仅返回标识不返回原始数据
        AntiBrushResultVO.RuleHit platformDirect = new AntiBrushResultVO.RuleHit();
        platformDirect.setDefenseLine(3);
        platformDirect.setRuleName("平台直取（演示入口标注）");
        platformDirect.setHitDesc("平台直取能力演示入口已标注'平台直取禁截图'，"
                + "本接口仅返回风险分与处置建议，不返回原始交易数据截图");
        platformDirect.setDeductedScore(0);
        platformDirect.setSamples(List.of("（演示入口标注：平台直取禁截图）"));
        hits.add(platformDirect);

        // ========= 风险分计算 =========
        int totalScore = hits.stream().mapToInt(AntiBrushResultVO.RuleHit::getDeductedScore).sum();
        int riskScore = Math.min(100, totalScore);
        String riskLevel;
        if (riskScore >= 70) riskLevel = "CRITICAL";
        else if (riskScore >= 40) riskLevel = "HIGH";
        else if (riskScore >= 20) riskLevel = "MEDIUM";
        else riskLevel = "LOW";

        vo.setHit(riskScore > 0);
        vo.setRiskScore(riskScore);
        vo.setRiskLevel(riskLevel);
        vo.setRuleHits(hits);
        vo.setAdvices(buildAdvices(riskLevel, hits));

        log.info("[防刷单] userId={} 风险分={} 级别={} 命中规则={}条",
                userId, riskScore, riskLevel, hits.size());
        return vo;
    }

    private List<String> buildAdvices(String riskLevel, List<AntiBrushResultVO.RuleHit> hits) {
        List<String> advices = new ArrayList<>();
        advices.add("（模拟口径，仅供参考）");
        if ("CRITICAL".equals(riskLevel)) {
            advices.add("风险等级 CRITICAL：建议立即冻结受托支付权限，转人工复核并保留证据。");
        } else if ("HIGH".equals(riskLevel)) {
            advices.add("风险等级 HIGH：建议暂停当日受托支付，限制提款额度并复核对手方。");
        } else if ("MEDIUM".equals(riskLevel)) {
            advices.add("风险等级 MEDIUM：建议复核对手方资质，加强交易真实性核验。");
        } else {
            advices.add("风险等级 LOW：交易特征正常，建议保持日常监测。");
        }
        for (AntiBrushResultVO.RuleHit h : hits) {
            if (h.getDeductedScore() > 0) {
                advices.add("- [" + h.getRuleName() + "] " + h.getHitDesc() + "（扣 " + h.getDeductedScore() + " 分）");
            }
        }
        return advices;
    }
}
