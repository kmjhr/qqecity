package com.icbc.qingqi.module.consumption.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.icbc.qingqi.module.consumption.entity.BizCreditReport;
import com.icbc.qingqi.module.consumption.mapper.BizCreditReportMapper;
import com.icbc.qingqi.module.loan.entity.BizLoanApplication;
import com.icbc.qingqi.module.loan.mapper.BizLoanApplicationMapper;
import com.icbc.qingqi.module.message.entity.SysMessage;
import com.icbc.qingqi.module.message.mapper.SysMessageMapper;
import com.icbc.qingqi.module.risk.entity.BizRiskWarning;
import com.icbc.qingqi.module.risk.mapper.BizRiskWarningMapper;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

/**
 * 常态化征信监测服务（模拟）
 * <p>
 * 缺口 #14 常态化征信监测
 * - 用户授权后「软查询」模拟，写入 biz_credit_report（source=SIMULATED，不产生硬查询）
 * - 检测异常：近6月新增贷款≥4笔 / 模拟逾期>0 → CREDIT_ABNORMAL 预警
 * - 7 天内同用户不重复触发预警
 */
@Slf4j
@Service
public class CreditMonitorService {

    private static final int RECENT_MONTHS = 6;
    private static final int LOAN_FREQ_ABNORMAL = 4;
    private static final int DEDUP_WINDOW_DAYS = 7;

    private final BizCreditReportMapper reportMapper;
    private final BizLoanApplicationMapper loanAppMapper;
    private final BizRiskWarningMapper riskWarningMapper;
    private final SysMessageMapper messageMapper;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public CreditMonitorService(BizCreditReportMapper reportMapper,
                                BizLoanApplicationMapper loanAppMapper,
                                BizRiskWarningMapper riskWarningMapper,
                                SysMessageMapper messageMapper) {
        this.reportMapper = reportMapper;
        this.loanAppMapper = loanAppMapper;
        this.riskWarningMapper = riskWarningMapper;
        this.messageMapper = messageMapper;
    }

    /**
     * 用户授权后软查询模拟征信报告
     */
    @Transactional(rollbackFor = Exception.class)
    public BizCreditReport softQuery(Long userId) {
        // 1. 聚合近6月贷款数据（模拟）
        LocalDateTime since6 = LocalDateTime.now().minusMonths(RECENT_MONTHS);
        List<BizLoanApplication> recentLoans = loanAppMapper.selectList(
                new LambdaQueryWrapper<BizLoanApplication>()
                        .eq(BizLoanApplication::getUserId, userId)
                        .ge(BizLoanApplication::getSubmitTime, since6));
        int loanCount = recentLoans.size();
        int approvedCount = (int) recentLoans.stream()
                .filter(l -> "APPROVED".equals(l.getApplyStatus()))
                .count();

        // 2. 模拟信用分（基于贷款数和逾期数）
        int overdueCount = simulateOverdueCount(userId);
        int score = 750 - loanCount * 10 - overdueCount * 30;
        score = Math.max(350, Math.min(850, score));
        String level = score >= 750 ? "EXCELLENT" : score >= 650 ? "GOOD" : score >= 550 ? "FAIR" : "POOR";

        BigDecimal totalLimit = recentLoans.stream()
                .map(BizLoanApplication::getApproveAmount)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // 3. 写入 biz_credit_report
        BizCreditReport report = new BizCreditReport();
        report.setUserId(userId);
        report.setReportNo("CR" + System.currentTimeMillis());
        report.setReportType("DETAIL");
        report.setCreditScore(score);
        report.setCreditLevel(level);
        report.setTotalLoanCount(approvedCount);
        report.setOverdueCount(overdueCount);
        report.setTotalCreditLimit(totalLimit);
        report.setUsedCreditLimit(totalLimit);
        report.setQueryCount(1);
        report.setReportSummary("模拟征信报告（软查询，不产生硬查询记录）。信用分 " + score
                + "，等级 " + level + "，近6月贷款 " + loanCount + " 笔，逾期 " + overdueCount + " 笔。");
        report.setReportDetail(buildDetailJson(loanCount, approvedCount, overdueCount, score, level));
        report.setQueryTime(LocalDateTime.now());
        report.setSource("SIMULATED");
        reportMapper.insert(report);

        // 4. 异常判定
        List<String> reasons = new ArrayList<>();
        if (loanCount >= LOAN_FREQ_ABNORMAL) {
            reasons.add("近6月新增贷款 " + loanCount + " 笔，超过 " + LOAN_FREQ_ABNORMAL + " 笔异常阈值");
        }
        if (overdueCount > 0) {
            reasons.add("模拟识别 " + overdueCount + " 笔逾期记录");
        }

        if (!reasons.isEmpty()) {
            // 7 天去重
            LocalDateTime dedupSince = LocalDateTime.now().minusDays(DEDUP_WINDOW_DAYS);
            Long existing = riskWarningMapper.selectCount(
                    new LambdaQueryWrapper<BizRiskWarning>()
                            .eq(BizRiskWarning::getUserId, userId)
                            .eq(BizRiskWarning::getWarningType, "CREDIT_ABNORMAL")
                            .ge(BizRiskWarning::getWarningTime, dedupSince));
            if (existing != null && existing > 0) {
                log.info("[征信监测] 用户={} 近{}天已触发 CREDIT_ABNORMAL，跳过落库", userId, DEDUP_WINDOW_DAYS);
                return report;
            }

            BizRiskWarning warning = new BizRiskWarning();
            warning.setUserId(userId);
            warning.setWarningType("CREDIT_ABNORMAL");
            warning.setWarningLevel("HIGH");
            warning.setWarningTitle("征信异常预警");
            warning.setWarningContent("【征信异常】\n" + String.join("\n", reasons)
                    + "\n\n信用分：" + score + "（" + level + "）");
            warning.setRelatedModule("CREDIT_REPORT");
            warning.setRelatedId(report.getId());
            warning.setIsRead(0);
            warning.setIsHandled(0);
            warning.setWarningTime(LocalDateTime.now());
            riskWarningMapper.insert(warning);

            SysMessage msg = new SysMessage();
            msg.setUserId(userId);
            msg.setTitle("征信监测异常预警");
            msg.setContent(String.join("；", reasons) + "。请关注您的信用状况，及时处理异常。");
            msg.setType("SAFETY");
            msg.setBizType("CREDIT_ABNORMAL");
            msg.setBizId(warning.getId());
            msg.setIsRead(0);
            messageMapper.insert(msg);

            log.info("[征信监测] 用户={}, warningId={}", userId, warning.getId());
        }

        return report;
    }

    /**
     * 查询用户最新一份模拟征信报告
     */
    public BizCreditReport latestReport(Long userId) {
        return reportMapper.selectOne(
                new LambdaQueryWrapper<BizCreditReport>()
                        .eq(BizCreditReport::getUserId, userId)
                        .eq(BizCreditReport::getSource, "SIMULATED")
                        .orderByDesc(BizCreditReport::getQueryTime)
                        .last("LIMIT 1"));
    }

    /**
     * 查询用户全部历史征信报告
     */
    public List<BizCreditReport> listReports(Long userId) {
        return reportMapper.selectList(
                new LambdaQueryWrapper<BizCreditReport>()
                        .eq(BizCreditReport::getUserId, userId)
                        .orderByDesc(BizCreditReport::getQueryTime));
    }

    public List<BizRiskWarning> listWarnings(Long userId) {
        return riskWarningMapper.selectList(
                new LambdaQueryWrapper<BizRiskWarning>()
                        .eq(BizRiskWarning::getUserId, userId)
                        .eq(BizRiskWarning::getWarningType, "CREDIT_ABNORMAL")
                        .orderByDesc(BizRiskWarning::getWarningTime));
    }

    /**
     * 模拟逾期笔数（基于用户已有 APPROVED 贷款数的伪随机）
     */
    private int simulateOverdueCount(Long userId) {
        // 演示用：用户 ID 为偶数 + APPROVED 贷款 ≥2 → 1 笔逾期；其他情况 0
        List<BizLoanApplication> approved = loanAppMapper.selectList(
                new LambdaQueryWrapper<BizLoanApplication>()
                        .eq(BizLoanApplication::getUserId, userId)
                        .eq(BizLoanApplication::getApplyStatus, "APPROVED"));
        if (approved.size() >= 2 && userId % 2 == 0) {
            return 1;
        }
        return 0;
    }

    private String buildDetailJson(int loanCount, int approvedCount, int overdue, int score, String level) {
        try {
            Map<String, Object> detail = new LinkedHashMap<>();
            detail.put("source", "SIMULATED");
            detail.put("notice", "本报告为模拟演示，不接入真实征信系统，不产生硬查询记录");
            detail.put("loanCount6m", loanCount);
            detail.put("approvedCount", approvedCount);
            detail.put("overdueCount", overdue);
            detail.put("creditScore", score);
            detail.put("creditLevel", level);
            detail.put("queryTime", LocalDateTime.now().toString());
            return objectMapper.writeValueAsString(detail);
        } catch (Exception e) {
            return "{}";
        }
    }
}
