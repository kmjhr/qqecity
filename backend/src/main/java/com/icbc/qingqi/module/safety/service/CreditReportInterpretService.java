package com.icbc.qingqi.module.safety.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.icbc.qingqi.common.BizException;
import com.icbc.qingqi.common.ErrorCode;
import com.icbc.qingqi.module.consumption.entity.BizCreditReport;
import com.icbc.qingqi.module.consumption.mapper.BizCreditReportMapper;
import com.icbc.qingqi.module.safety.dto.CreditReportInterpretVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.*;

/**
 * 征信报告智能解读服务（模拟）
 * <p>
 * 缺口 #2 征信报告智能解读
 * - 用户上传/选择 biz_credit_report → 结构化解读
 * - 逐项生成"好/差"评价与改进建议
 * - 预置 2 份演示报告（GOOD / FLAWED）通过 loadDemo 装载
 * - 全程标注"模拟"，不接入真实征信系统
 */
@Slf4j
@Service
public class CreditReportInterpretService {

    private final BizCreditReportMapper reportMapper;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public CreditReportInterpretService(BizCreditReportMapper reportMapper) {
        this.reportMapper = reportMapper;
    }

    /**
     * 列出本人全部模拟征信报告
     */
    public List<BizCreditReport> listReports(Long userId) {
        return reportMapper.selectList(
                new LambdaQueryWrapper<BizCreditReport>()
                        .eq(BizCreditReport::getUserId, userId)
                        .orderByDesc(BizCreditReport::getQueryTime));
    }

    /**
     * 装载演示报告（GOOD / FLAWED）
     * <p>
     * 注：演示数据来自 data.sql，也可由本接口按模板即时生成一份
     */
    @Transactional(rollbackFor = Exception.class)
    public BizCreditReport loadDemo(Long userId, String type) {
        // 已存在则直接返回最近一份同类型演示报告，避免重复生成
        BizCreditReport existing = findDemoByType(userId, type);
        if (existing != null) {
            return existing;
        }
        BizCreditReport report = new BizCreditReport();
        report.setUserId(userId);
        report.setReportNo("CR" + System.currentTimeMillis());
        report.setReportType("DETAIL");
        report.setQueryTime(LocalDateTime.now());
        report.setSource("SIMULATED");

        if ("GOOD".equalsIgnoreCase(type)) {
            buildGoodDemo(report);
        } else if ("FLAWED".equalsIgnoreCase(type)) {
            buildFlawedDemo(report);
        } else {
            throw new BizException(ErrorCode.PARAM_ERROR, "type 仅支持 GOOD / FLAWED");
        }
        report.setReportDetail(buildDetailJson(report));
        reportMapper.insert(report);
        log.info("[征信解读] 装载演示报告：userId={}, type={}, reportNo={}", userId, type, report.getReportNo());
        return report;
    }

    /**
     * 结构化解读一份征信报告
     */
    public CreditReportInterpretVO interpret(Long userId, Long reportId) {
        BizCreditReport report = reportMapper.selectById(reportId);
        if (report == null || !userId.equals(report.getUserId())) {
            throw new BizException(ErrorCode.FORBIDDEN, "征信报告不存在或无权访问");
        }
        return buildInterpretation(report);
    }

    // ============================================================
    //  逐项评估
    // ============================================================

    private CreditReportInterpretVO buildInterpretation(BizCreditReport r) {
        CreditReportInterpretVO vo = new CreditReportInterpretVO();
        vo.setReportId(r.getId());
        vo.setReportNo(r.getReportNo());
        vo.setReportType(r.getReportType());
        vo.setCreditScore(r.getCreditScore());
        vo.setCreditLevel(r.getCreditLevel());
        vo.setTotalLoanCount(r.getTotalLoanCount());
        vo.setOverdueCount(r.getOverdueCount());
        vo.setTotalCreditLimit(r.getTotalCreditLimit());
        vo.setUsedCreditLimit(r.getUsedCreditLimit());
        vo.setQueryCount(r.getQueryCount());
        vo.setQueryTime(r.getQueryTime());
        vo.setSource(r.getSource());
        vo.setSimulationNotice("本解读基于模拟征信报告（source=SIMULATED），不接入真实征信系统，不产生硬查询；指标仅用于演示。");

        BigDecimal utilization = BigDecimal.ZERO;
        if (r.getTotalCreditLimit() != null && r.getTotalCreditLimit().compareTo(BigDecimal.ZERO) > 0
                && r.getUsedCreditLimit() != null) {
            utilization = r.getUsedCreditLimit()
                    .multiply(new BigDecimal("100"))
                    .divide(r.getTotalCreditLimit(), 2, RoundingMode.HALF_UP);
        }
        vo.setUtilizationRate(utilization);

        List<CreditReportInterpretVO.ItemAssessment> items = new ArrayList<>();

        // 1. 信用分
        items.add(assessScore(r.getCreditScore()));
        // 2. 逾期笔数
        items.add(assessOverdue(r.getOverdueCount()));
        // 3. 查询次数
        items.add(assessQuery(r.getQueryCount()));
        // 4. 额度使用率（负债率）
        items.add(assessUtilization(utilization));
        // 5. 贷款笔数
        items.add(assessLoanCount(r.getTotalLoanCount()));

        vo.setAssessments(items);

        // 总体评价
        String overallLevel = computeOverallLevel(r.getCreditScore(), r.getOverdueCount(),
                utilization, r.getQueryCount(), r.getTotalLoanCount());
        vo.setOverallLevel(overallLevel);
        vo.setOverallSummary(buildOverallSummary(overallLevel, r));

        // 改进建议清单（汇总）
        List<String> suggestions = new ArrayList<>();
        for (CreditReportInterpretVO.ItemAssessment it : items) {
            if (it.getSuggestion() != null && !it.getSuggestion().isEmpty()) {
                suggestions.add(it.getSuggestion());
            }
        }
        if (r.getOverdueCount() != null && r.getOverdueCount() > 0) {
            suggestions.add("优先结清现有 " + r.getOverdueCount() + " 笔逾期，逾期记录将在结清后 5 年从征信报告中滚动消除（模拟说明）。");
        }
        if (r.getQueryCount() != null && r.getQueryCount() >= 6) {
            suggestions.add("减少不必要的征信授权查询，未来 6 个月内避免新增贷款/信用卡申请。");
        }
        if (utilization.compareTo(new BigDecimal("70")) > 0) {
            suggestions.add("降低负债率至 70% 以下，可显著提升信用分（模拟建议）。");
        }
        suggestions.add("保持按时足额还款，避免最低还款或逾期；可通过青启e城「预算消费」模块建立月度预算。");
        suggestions.add("（以上为模拟建议，不构成投资/融资邀约；真实征信状况请以人民银行征信中心报告为准）");
        vo.setSuggestions(suggestions);

        return vo;
    }

    private CreditReportInterpretVO.ItemAssessment assessScore(Integer score) {
        CreditReportInterpretVO.ItemAssessment a = new CreditReportInterpretVO.ItemAssessment();
        a.setItemKey("CREDIT_SCORE");
        a.setItemLabel("信用分");
        a.setCurrentValue(score == null ? "无数据" : String.valueOf(score));
        if (score == null) {
            a.setLevel("FAIR");
            a.setEvaluation("报告未提供信用分，无法评级（模拟）。");
            a.setSuggestion("通过青启e城「征信监测」授权软查询生成模拟信用分。");
        } else if (score >= 750) {
            a.setLevel("GOOD");
            a.setEvaluation("信用分属于「优秀」区间（≥750），代表信用状况良好，融资可获得更优利率。");
            a.setSuggestion("继续保持按时还款与适度负债水平，维持信用优势。");
        } else if (score >= 650) {
            a.setLevel("FAIR");
            a.setEvaluation("信用分属于「良好」区间（650-749），信用尚可，但有提升空间。");
            a.setSuggestion("保持 6 个月以上无逾期记录，适度降低额度使用率，可稳步提升至 750+。");
        } else if (score >= 550) {
            a.setLevel("POOR");
            a.setEvaluation("信用分属于「一般」区间（550-649），可能影响部分贷款审批。");
            a.setSuggestion("核查近期逾期记录、查询次数、额度使用率，针对性改善（详见下方清单）。");
        } else {
            a.setLevel("POOR");
            a.setEvaluation("信用分低于 550，属于「较差」区间，建议立即采取修复措施。");
            a.setSuggestion("优先结清逾期，停止新增贷款申请，3-6 个月后再次评估。");
        }
        return a;
    }

    private CreditReportInterpretVO.ItemAssessment assessOverdue(Integer overdue) {
        CreditReportInterpretVO.ItemAssessment a = new CreditReportInterpretVO.ItemAssessment();
        a.setItemKey("OVERDUE");
        a.setItemLabel("逾期笔数");
        int oc = overdue == null ? 0 : overdue;
        a.setCurrentValue(oc + " 笔");
        if (oc == 0) {
            a.setLevel("GOOD");
            a.setEvaluation("无逾期记录，信用历史良好。");
            a.setSuggestion("继续保持按时还款习惯。");
        } else if (oc == 1) {
            a.setLevel("FAIR");
            a.setEvaluation("存在 1 笔逾期记录，对信用分有中等影响。");
            a.setSuggestion("尽快结清逾期欠款，结清后保持 24 个月以上无新增逾期，可逐步修复。");
        } else {
            a.setLevel("POOR");
            a.setEvaluation("存在 " + oc + " 笔逾期记录，对信用分影响较大。");
            a.setSuggestion("立即结清所有逾期欠款，并与债权方协商出具结清证明。");
        }
        return a;
    }

    private CreditReportInterpretVO.ItemAssessment assessQuery(Integer query) {
        CreditReportInterpretVO.ItemAssessment a = new CreditReportInterpretVO.ItemAssessment();
        a.setItemKey("QUERY");
        a.setItemLabel("查询次数");
        int qc = query == null ? 0 : query;
        a.setCurrentValue(qc + " 次");
        if (qc <= 2) {
            a.setLevel("GOOD");
            a.setEvaluation("查询次数较少，未对信用分产生负面影响。");
            a.setSuggestion("保持审慎授权，避免短期内频繁申贷。");
        } else if (qc <= 5) {
            a.setLevel("FAIR");
            a.setEvaluation("查询次数偏多，可能触发部分机构风控规则。");
            a.setSuggestion("未来 6 个月尽量不再申请新的信贷产品。");
        } else {
            a.setLevel("POOR");
            a.setEvaluation("查询次数过多（" + qc + " 次），视为「资金渴求」信号，会拉低信用评分。");
            a.setSuggestion("暂停一切贷款/信用卡申请至少 6 个月，让查询记录自然老化。");
        }
        return a;
    }

    private CreditReportInterpretVO.ItemAssessment assessUtilization(BigDecimal utilization) {
        CreditReportInterpretVO.ItemAssessment a = new CreditReportInterpretVO.ItemAssessment();
        a.setItemKey("UTILIZATION");
        a.setItemLabel("额度使用率（负债率）");
        BigDecimal u = utilization == null ? BigDecimal.ZERO : utilization;
        a.setCurrentValue(u + "%");
        if (u.compareTo(new BigDecimal("30")) <= 0) {
            a.setLevel("GOOD");
            a.setEvaluation("额度使用率 ≤30%，负债结构健康。");
            a.setSuggestion("保持当前负债水平。");
        } else if (u.compareTo(new BigDecimal("70")) <= 0) {
            a.setLevel("FAIR");
            a.setEvaluation("额度使用率在 30%-70% 之间，负债偏高。");
            a.setSuggestion("优先偿还部分欠款，将使用率降至 30% 以下。");
        } else {
            a.setLevel("POOR");
            a.setEvaluation("额度使用率 >70%，被视为高负债，会显著影响信用评分。");
            a.setSuggestion("立即制定还款计划，尽快将使用率降至 50% 以下，目标 30%。");
        }
        return a;
    }

    private CreditReportInterpretVO.ItemAssessment assessLoanCount(Integer loanCount) {
        CreditReportInterpretVO.ItemAssessment a = new CreditReportInterpretVO.ItemAssessment();
        a.setItemKey("LOAN_COUNT");
        a.setItemLabel("贷款笔数");
        int lc = loanCount == null ? 0 : loanCount;
        a.setCurrentValue(lc + " 笔");
        if (lc <= 1) {
            a.setLevel("GOOD");
            a.setEvaluation("贷款笔数合理，未对信用产生明显影响。");
            a.setSuggestion("按计划还款即可。");
        } else if (lc <= 3) {
            a.setLevel("FAIR");
            a.setEvaluation("贷款笔数偏多，需关注月供占收入比例。");
            a.setSuggestion("关注月供占月收入比例，建议不超过 50%。");
        } else {
            a.setLevel("POOR");
            a.setEvaluation("贷款笔数过多（" + lc + " 笔），存在「以贷养贷」风险。");
            a.setSuggestion("梳理所有贷款明细，优先结清小额、高息贷款。");
        }
        return a;
    }

    private String computeOverallLevel(Integer score, Integer overdue,
                                        BigDecimal utilization, Integer query, Integer loanCount) {
        int poorCnt = 0, fairCnt = 0;
        if (score == null || score < 550) poorCnt++;
        else if (score < 750) fairCnt++;
        if (overdue != null && overdue > 1) poorCnt++;
        else if (overdue != null && overdue == 1) fairCnt++;
        if (utilization != null && utilization.compareTo(new BigDecimal("70")) > 0) poorCnt++;
        else if (utilization != null && utilization.compareTo(new BigDecimal("30")) > 0) fairCnt++;
        if (query != null && query > 5) poorCnt++;
        else if (query != null && query > 2) fairCnt++;
        if (loanCount != null && loanCount > 3) poorCnt++;
        else if (loanCount != null && loanCount > 1) fairCnt++;

        if (poorCnt >= 2) return "POOR";
        if (poorCnt == 1 || fairCnt >= 3) return "FAIR";
        if (fairCnt >= 1) return "FAIR";
        return "GOOD";
    }

    private String buildOverallSummary(String level, BizCreditReport r) {
        switch (level) {
            case "GOOD":
                return "信用状况良好：无逾期、负债率健康、查询次数合理，融资可获更优利率。【模拟解读】";
            case "FAIR":
                return "信用状况一般：" + (r.getOverdueCount() != null && r.getOverdueCount() > 0
                        ? "存在逾期记录、" : "") + "负债率偏高或查询偏多，按建议改善后可逐步提升至「良好」。【模拟解读】";
            default:
                return "信用状况较差：多项指标需立即改善，请优先结清逾期、降低负债、减少查询。【模拟解读】";
        }
    }

    // ============================================================
    //  演示报告模板（GOOD / FLAWED）
    // ============================================================

    private void buildGoodDemo(BizCreditReport r) {
        r.setCreditScore(782);
        r.setCreditLevel("EXCELLENT");
        r.setTotalLoanCount(1);
        r.setOverdueCount(0);
        r.setTotalCreditLimit(new BigDecimal("50000.00"));
        r.setUsedCreditLimit(new BigDecimal("8000.00"));
        r.setQueryCount(2);
        r.setReportSummary("【良好示例·模拟】信用分782、无逾期、负债率16%、查询2次。"
                + "近6个月按时足额还款，信用状况良好，可获更优融资利率。");
    }

    private void buildFlawedDemo(BizCreditReport r) {
        r.setCreditScore(588);
        r.setCreditLevel("FAIR");
        r.setTotalLoanCount(3);
        r.setOverdueCount(1);
        r.setTotalCreditLimit(new BigDecimal("30000.00"));
        r.setUsedCreditLimit(new BigDecimal("24000.00"));
        r.setQueryCount(7);
        r.setReportSummary("【有瑕疵示例·模拟】信用分588、1笔逾期（已结清待滚动）、负债率80%、查询7次。"
                + "近期短周期多次申贷，被部分机构风控规则限制，建议暂停新申请。");
    }

    private String buildDetailJson(BizCreditReport r) {
        try {
            Map<String, Object> detail = new LinkedHashMap<>();
            detail.put("source", "SIMULATED");
            detail.put("notice", "本报告为模拟演示，不接入真实征信系统，不产生硬查询记录");
            detail.put("creditScore", r.getCreditScore());
            detail.put("creditLevel", r.getCreditLevel());
            detail.put("loanCount", r.getTotalLoanCount());
            detail.put("overdueCount", r.getOverdueCount());
            detail.put("totalCreditLimit", r.getTotalCreditLimit());
            detail.put("usedCreditLimit", r.getUsedCreditLimit());
            detail.put("queryCount", r.getQueryCount());
            detail.put("queryTime", r.getQueryTime() == null ? null : r.getQueryTime().toString());
            return objectMapper.writeValueAsString(detail);
        } catch (Exception e) {
            return "{}";
        }
    }

    private BizCreditReport findDemoByType(Long userId, String type) {
        // 演示报告以 report_summary 中的【良好示例·模拟】/【有瑕疵示例·模拟】为识别
        String keyword = "GOOD".equalsIgnoreCase(type) ? "良好示例·模拟" : "有瑕疵示例·模拟";
        List<BizCreditReport> list = reportMapper.selectList(
                new LambdaQueryWrapper<BizCreditReport>()
                        .eq(BizCreditReport::getUserId, userId)
                        .eq(BizCreditReport::getSource, "SIMULATED")
                        .like(BizCreditReport::getReportSummary, keyword)
                        .orderByDesc(BizCreditReport::getQueryTime));
        return list.isEmpty() ? null : list.get(0);
    }
}
