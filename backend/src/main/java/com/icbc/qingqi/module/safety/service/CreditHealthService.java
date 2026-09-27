package com.icbc.qingqi.module.safety.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.icbc.qingqi.module.consumption.entity.BizCreditReport;
import com.icbc.qingqi.module.consumption.mapper.BizCreditReportMapper;
import com.icbc.qingqi.module.safety.dto.CreditHealthVO;
import com.icbc.qingqi.module.safety.dto.SimulateFixVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

/**
 * 征信健康管理服务（模拟）
 * <p>
 * 缺口 #26 征信健康管理（简化版）
 * - 健康分：复用 biz_credit_report.credit_score（取最新一份）
 * - 维度评分：还款 / 负债率 / 查询 / 逾期
 * - 改善清单：还清逾期 / 减少查询 / 规范还款 / 降低负债
 * - 模拟修复路径：展示 M1/M3/M6 分阶段提升，标注不产生真实征信影响
 */
@Slf4j
@Service
public class CreditHealthService {

    private final BizCreditReportMapper reportMapper;

    public CreditHealthService(BizCreditReportMapper reportMapper) {
        this.reportMapper = reportMapper;
    }

    /**
     * 获取用户征信健康分与改善清单
     */
    public CreditHealthVO getHealth(Long userId) {
        BizCreditReport latest = reportMapper.selectOne(
                new LambdaQueryWrapper<BizCreditReport>()
                        .eq(BizCreditReport::getUserId, userId)
                        .orderByDesc(BizCreditReport::getQueryTime)
                        .last("LIMIT 1"));

        CreditHealthVO vo = new CreditHealthVO();
        vo.setUserId(userId);
        vo.setSimulationNotice("征信健康分为模拟值，基于 biz_credit_report 最新报告（source=SIMULATED），不接入真实征信系统，不产生硬查询；本结果仅供演示。");

        if (latest == null) {
            vo.setHealthScore(null);
            vo.setHealthLevel("UNKNOWN");
            vo.setHealthLevelName("无报告");
            vo.setDimensions(List.of());
            vo.setImprovements(List.of());
            return vo;
        }

        Integer score = latest.getCreditScore();
        String level = score == null ? "UNKNOWN" :
                score >= 750 ? "EXCELLENT" : score >= 650 ? "GOOD" : score >= 550 ? "FAIR" : "POOR";
        vo.setHealthScore(score);
        vo.setHealthLevel(level);
        vo.setHealthLevelName(levelName(level));

        BigDecimal utilization = BigDecimal.ZERO;
        if (latest.getTotalCreditLimit() != null
                && latest.getTotalCreditLimit().compareTo(BigDecimal.ZERO) > 0
                && latest.getUsedCreditLimit() != null) {
            utilization = latest.getUsedCreditLimit()
                    .multiply(new BigDecimal("100"))
                    .divide(latest.getTotalCreditLimit(), 2, RoundingMode.HALF_UP);
        }

        // 维度评分
        List<CreditHealthVO.DimensionScore> dims = new ArrayList<>();
        dims.add(assessRepayment(latest.getOverdueCount()));
        dims.add(assessUtilization(utilization));
        dims.add(assessQuery(latest.getQueryCount()));
        dims.add(assessOverdue(latest.getOverdueCount()));
        vo.setDimensions(dims);

        // 改善清单
        vo.setImprovements(buildImprovements(latest, utilization));

        return vo;
    }

    /**
     * 模拟修复路径（标注不产生真实征信影响）
     */
    public SimulateFixVO simulateFix(Long userId) {
        BizCreditReport latest = reportMapper.selectOne(
                new LambdaQueryWrapper<BizCreditReport>()
                        .eq(BizCreditReport::getUserId, userId)
                        .orderByDesc(BizCreditReport::getQueryTime)
                        .last("LIMIT 1"));

        SimulateFixVO vo = new SimulateFixVO();
        vo.setUserId(userId);
        vo.setSimulationNotice("【重要】本路径为模拟演示，不会向真实征信系统提交任何修复请求，不产生真实征信影响；"
                + "真实征信记录只能通过人民银行征信中心异议申请或正常还款滚动消除，且不收取任何费用。");

        int startScore = latest == null || latest.getCreditScore() == null
                ? 600 : latest.getCreditScore();
        vo.setStartScore(startScore);

        List<SimulateFixVO.FixStage> stages = new ArrayList<>();

        // M1：结清现有逾期
        int m1Score = Math.min(850, startScore + (latest != null && latest.getOverdueCount() != null && latest.getOverdueCount() > 0 ? 20 : 5));
        stages.add(buildStage("M1", "第 1 个月", "结清现有逾期欠款并取得债权方结清证明",
                m1Score, m1Score - startScore,
                "逾期记录虽不会立即消除，但「已结清」状态对信用评分有即时正向影响（模拟）。"));

        // M3：降低额度使用率至 30% 以下
        int m3Score = Math.min(850, m1Score + 25);
        stages.add(buildStage("M3", "第 3 个月", "偿还部分欠款，将额度使用率降至 30% 以下",
                m3Score, m3Score - startScore,
                "额度使用率是信用评分核心因子，30% 以下视为健康（模拟）。"));

        // M6：查询次数自然老化 + 持续按时还款
        int m6Score = Math.min(850, m3Score + 20);
        stages.add(buildStage("M6", "第 6 个月", "6 个月内不再申请新信贷，所有账单按时足额还款",
                m6Score, m6Score - startScore,
                "查询记录 6 个月后影响减弱，按时还款累计 6 个月可显著提升评分（模拟）。"));

        vo.setStages(stages);
        vo.setEndScore(m6Score);
        vo.setScoreGain(m6Score - startScore);

        log.info("[征信健康] 用户={} 模拟修复路径：{} → {}（+{}分）", userId, startScore, m6Score, m6Score - startScore);
        return vo;
    }

    // ============================================================
    //  内部方法
    // ============================================================

    private CreditHealthVO.DimensionScore assessRepayment(Integer overdueCount) {
        CreditHealthVO.DimensionScore d = new CreditHealthVO.DimensionScore();
        d.setKey("REPAYMENT");
        d.setLabel("还款行为");
        int oc = overdueCount == null ? 0 : overdueCount;
        if (oc == 0) {
            d.setScore(90);
            d.setEvaluation("无逾期，还款行为良好。");
        } else if (oc == 1) {
            d.setScore(65);
            d.setEvaluation("存在 1 笔逾期，需尽快结清。");
        } else {
            d.setScore(40);
            d.setEvaluation("存在多笔逾期，还款行为需要立即改善。");
        }
        return d;
    }

    private CreditHealthVO.DimensionScore assessUtilization(BigDecimal utilization) {
        CreditHealthVO.DimensionScore d = new CreditHealthVO.DimensionScore();
        d.setKey("UTILIZATION");
        d.setLabel("额度使用率");
        BigDecimal u = utilization == null ? BigDecimal.ZERO : utilization;
        if (u.compareTo(new BigDecimal("30")) <= 0) {
            d.setScore(95);
            d.setEvaluation("使用率 ≤30%，负债结构健康。");
        } else if (u.compareTo(new BigDecimal("70")) <= 0) {
            d.setScore(70);
            d.setEvaluation("使用率 30%-70%，负债偏高。");
        } else {
            d.setScore(45);
            d.setEvaluation("使用率 >70%，对信用评分影响较大。");
        }
        return d;
    }

    private CreditHealthVO.DimensionScore assessQuery(Integer queryCount) {
        CreditHealthVO.DimensionScore d = new CreditHealthVO.DimensionScore();
        d.setKey("QUERY");
        d.setLabel("查询次数");
        int qc = queryCount == null ? 0 : queryCount;
        if (qc <= 2) {
            d.setScore(95);
            d.setEvaluation("查询次数合理。");
        } else if (qc <= 5) {
            d.setScore(70);
            d.setEvaluation("查询次数偏多，建议未来 6 个月不新增申请。");
        } else {
            d.setScore(40);
            d.setEvaluation("查询次数过多，影响信用评分。");
        }
        return d;
    }

    private CreditHealthVO.DimensionScore assessOverdue(Integer overdueCount) {
        CreditHealthVO.DimensionScore d = new CreditHealthVO.DimensionScore();
        d.setKey("OVERDUE");
        d.setLabel("逾期记录");
        int oc = overdueCount == null ? 0 : overdueCount;
        if (oc == 0) {
            d.setScore(95);
            d.setEvaluation("无逾期记录。");
        } else if (oc == 1) {
            d.setScore(60);
            d.setEvaluation("1 笔逾期，结清后 5 年滚动消除（模拟）。");
        } else {
            d.setScore(30);
            d.setEvaluation(oc + " 笔逾期，需立即结清。");
        }
        return d;
    }

    private List<CreditHealthVO.ImprovementItem> buildImprovements(BizCreditReport r, BigDecimal utilization) {
        List<CreditHealthVO.ImprovementItem> list = new ArrayList<>();

        if (r.getOverdueCount() != null && r.getOverdueCount() > 0) {
            CreditHealthVO.ImprovementItem it = new CreditHealthVO.ImprovementItem();
            it.setKey("CLEAR_OVERDUE");
            it.setLabel("结清逾期欠款");
            it.setCurrentValue(r.getOverdueCount() + " 笔未结清");
            it.setTargetValue("0 笔");
            it.setEstimatedScoreGain(20);
            it.setAction("联系债权方结清欠款并索取结清证明；逾期记录将在结清后 5 年从报告中滚动消除（模拟）。");
            it.setCompleted(false);
            list.add(it);
        }

        if (utilization.compareTo(new BigDecimal("30")) > 0) {
            CreditHealthVO.ImprovementItem it = new CreditHealthVO.ImprovementItem();
            it.setKey("LOWER_UTILIZATION");
            it.setLabel("降低额度使用率");
            it.setCurrentValue(utilization + "%");
            it.setTargetValue("≤30%");
            it.setEstimatedScoreGain(25);
            it.setAction("优先偿还部分欠款，将额度使用率降至 30% 以下；可通过青启e城「预算消费」模块建立还款计划。");
            it.setCompleted(false);
            list.add(it);
        }

        if (r.getQueryCount() != null && r.getQueryCount() > 2) {
            CreditHealthVO.ImprovementItem it = new CreditHealthVO.ImprovementItem();
            it.setKey("REDUCE_QUERY");
            it.setLabel("减少征信授权查询");
            it.setCurrentValue(r.getQueryCount() + " 次");
            it.setTargetValue("≤2 次/6 月");
            it.setEstimatedScoreGain(15);
            it.setAction("未来 6 个月避免申请任何新的信贷产品，让查询记录自然老化。");
            it.setCompleted(false);
            list.add(it);
        }

        CreditHealthVO.ImprovementItem it3 = new CreditHealthVO.ImprovementItem();
        it3.setKey("STANDARD_REPAY");
        it3.setLabel("规范还款");
        it3.setCurrentValue("—");
        it3.setTargetValue("连续 6 月无新增逾期");
        it3.setEstimatedScoreGain(15);
        it3.setAction("设置自动还款提醒，按时足额还款，避免最低还款或逾期；可通过青启e城「逾期预判」模块提前预警。");
        it3.setCompleted(false);
        list.add(it3);

        return list;
    }

    private String levelName(String level) {
        switch (level) {
            case "EXCELLENT": return "优秀";
            case "GOOD": return "良好";
            case "FAIR": return "一般";
            case "POOR": return "较差";
            default: return "无报告";
        }
    }

    private SimulateFixVO.FixStage buildStage(String stage, String stageName, String action,
                                              int expectedScore, int gain, String explanation) {
        SimulateFixVO.FixStage s = new SimulateFixVO.FixStage();
        s.setStage(stage);
        s.setStageName(stageName);
        s.setAction(action);
        s.setExpectedScore(expectedScore);
        s.setGainFromStart(gain);
        s.setExplanation(explanation);
        return s;
    }
}
