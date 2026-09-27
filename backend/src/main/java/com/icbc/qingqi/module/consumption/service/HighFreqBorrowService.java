package com.icbc.qingqi.module.consumption.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.icbc.qingqi.module.bookkeeping.entity.BizBookkeepingRecord;
import com.icbc.qingqi.module.bookkeeping.mapper.BizBookkeepingRecordMapper;
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
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 高频借贷/非理性负债预警服务
 * <p>
 * 缺口 #13 高频借贷/非理性负债预警
 * 规则（演示口径）：
 *  - 30 天内贷款申请 ≥3 笔 → 短周期多次借款（HIGH）
 *  - 当前 APPROVED 贷款 ≥2 笔 + 申请新贷款 → 以贷养贷特征（HIGH）
 *  - 月还款额占月收入 >50% → 非理性负债（CRITICAL）
 * 命中落 biz_risk_warning（warning_type=HIGH_FREQ_BORROW）+ 站内信
 * 7 天内同用户同等级不重复触发
 */
@Slf4j
@Service
public class HighFreqBorrowService {

    private static final int RECENT_DAYS = 30;
    private static final int FREQ_THRESHOLD = 3;
    private static final int MULTI_LOAN_THRESHOLD = 2;
    private static final BigDecimal DEBT_RATIO_THRESHOLD = new BigDecimal("0.50");
    private static final int DEDUP_WINDOW_DAYS = 7;

    private final BizLoanApplicationMapper loanAppMapper;
    private final BizBookkeepingRecordMapper bookkeepingMapper;
    private final BizRiskWarningMapper riskWarningMapper;
    private final SysMessageMapper messageMapper;

    public HighFreqBorrowService(BizLoanApplicationMapper loanAppMapper,
                                 BizBookkeepingRecordMapper bookkeepingMapper,
                                 BizRiskWarningMapper riskWarningMapper,
                                 SysMessageMapper messageMapper) {
        this.loanAppMapper = loanAppMapper;
        this.bookkeepingMapper = bookkeepingMapper;
        this.riskWarningMapper = riskWarningMapper;
        this.messageMapper = messageMapper;
    }

    @Transactional(rollbackFor = Exception.class)
    public DetectResult detect(Long userId) {
        LocalDate today = LocalDate.now();
        LocalDateTime since30 = LocalDateTime.now().minusDays(RECENT_DAYS);

        // 1. 近30天贷款申请
        List<BizLoanApplication> recentApps = loanAppMapper.selectList(
                new LambdaQueryWrapper<BizLoanApplication>()
                        .eq(BizLoanApplication::getUserId, userId)
                        .ge(BizLoanApplication::getSubmitTime, since30));
        int recentCount = recentApps.size();

        // 2. 当前 APPROVED 贷款（视为已放款未还清）
        List<BizLoanApplication> activeLoans = loanAppMapper.selectList(
                new LambdaQueryWrapper<BizLoanApplication>()
                        .eq(BizLoanApplication::getUserId, userId)
                        .eq(BizLoanApplication::getApplyStatus, "APPROVED"));
        int activeLoanCount = activeLoans.size();

        // 3. 月收入（近3月 INCOME 月均）
        LocalDate monthStart = today.withDayOfMonth(1).minusMonths(2);
        List<BizBookkeepingRecord> recentIncome = bookkeepingMapper.selectList(
                new LambdaQueryWrapper<BizBookkeepingRecord>()
                        .eq(BizBookkeepingRecord::getUserId, userId)
                        .eq(BizBookkeepingRecord::getRecordType, "INCOME")
                        .ge(BizBookkeepingRecord::getHappenDate, monthStart));
        BigDecimal monthlyIncome = recentIncome.stream()
                .map(BizBookkeepingRecord::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .divide(new BigDecimal("3"), 2, RoundingMode.HALF_UP);

        // 4. 月还款额估算（按 APPROVED 贷款总额 / 12 + 近3月 EXPENSE 中"还款"分类）
        BigDecimal monthlyRepay = BigDecimal.ZERO;
        if (!activeLoans.isEmpty()) {
            BigDecimal totalLoan = activeLoans.stream()
                    .map(BizLoanApplication::getApproveAmount)
                    .filter(java.util.Objects::nonNull)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            monthlyRepay = monthlyRepay.add(totalLoan.divide(new BigDecimal("12"), 2, RoundingMode.HALF_UP));
        }
        List<BizBookkeepingRecord> recentRepay = bookkeepingMapper.selectList(
                new LambdaQueryWrapper<BizBookkeepingRecord>()
                        .eq(BizBookkeepingRecord::getUserId, userId)
                        .eq(BizBookkeepingRecord::getRecordType, "EXPENSE")
                        .like(BizBookkeepingRecord::getCategory, "还款")
                        .ge(BizBookkeepingRecord::getHappenDate, monthStart));
        BigDecimal manualRepay = recentRepay.stream()
                .map(BizBookkeepingRecord::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .divide(new BigDecimal("3"), 2, RoundingMode.HALF_UP);
        monthlyRepay = monthlyRepay.add(manualRepay);

        BigDecimal debtRatio = monthlyIncome.compareTo(BigDecimal.ZERO) > 0
                ? monthlyRepay.divide(monthlyIncome, 4, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        // 5. 判定
        List<String> reasons = new ArrayList<>();
        String level = null;
        if (recentCount >= FREQ_THRESHOLD) {
            reasons.add("近 30 天内提交 " + recentCount + " 笔贷款申请，命中「短周期多次借款」特征");
            level = "HIGH";
        }
        if (activeLoanCount >= MULTI_LOAN_THRESHOLD && !recentApps.isEmpty()) {
            reasons.add("当前有 " + activeLoanCount + " 笔未还清贷款且仍在申请新贷款，命中「以贷养贷」特征");
            level = "HIGH";
        }
        if (debtRatio.compareTo(DEBT_RATIO_THRESHOLD) > 0) {
            reasons.add("月还款额占月收入 " + debtRatio.multiply(new BigDecimal("100")).setScale(2, RoundingMode.HALF_UP)
                    + "%，超过 50% 阈值，命中「非理性负债」特征");
            level = "CRITICAL";
        }

        DetectResult result = new DetectResult();
        result.setUserId(userId);
        result.setRecentAppCount(recentCount);
        result.setActiveLoanCount(activeLoanCount);
        result.setMonthlyIncome(monthlyIncome);
        result.setMonthlyRepay(monthlyRepay);
        result.setDebtRatio(debtRatio);

        if (level != null) {
            result.setWarningTriggered(true);
            result.setTriggerReasons(reasons);
            result.setWarningLevel(level);
            result.setAdvice(buildAdvice(level, recentCount, activeLoanCount, debtRatio));

            // 7 天内同等级去重
            LocalDateTime dedupSince = LocalDateTime.now().minusDays(DEDUP_WINDOW_DAYS);
            Long existing = riskWarningMapper.selectCount(
                    new LambdaQueryWrapper<BizRiskWarning>()
                            .eq(BizRiskWarning::getUserId, userId)
                            .eq(BizRiskWarning::getWarningType, "HIGH_FREQ_BORROW")
                            .eq(BizRiskWarning::getWarningLevel, level)
                            .ge(BizRiskWarning::getWarningTime, dedupSince));
            if (existing != null && existing > 0) {
                log.info("[高频借贷预警] 用户={} 近{}天已触发等级={}，跳过落库", userId, DEDUP_WINDOW_DAYS, level);
                return result;
            }

            BizRiskWarning warning = new BizRiskWarning();
            warning.setUserId(userId);
            warning.setWarningType("HIGH_FREQ_BORROW");
            warning.setWarningLevel(level);
            warning.setWarningTitle("高频借贷/非理性负债预警");
            warning.setWarningContent(buildContent(reasons, monthlyIncome, monthlyRepay, debtRatio));
            warning.setRelatedModule("LOAN");
            warning.setIsRead(0);
            warning.setIsHandled(0);
            warning.setWarningTime(LocalDateTime.now());
            riskWarningMapper.insert(warning);
            result.setWarningId(warning.getId());

            SysMessage msg = new SysMessage();
            msg.setUserId(userId);
            msg.setTitle("高频借贷风险预警（" + level + "）");
            msg.setContent(String.join("；", reasons) + "。" + result.getAdvice());
            msg.setType("SAFETY");
            msg.setBizType("HIGH_FREQ_BORROW");
            msg.setBizId(warning.getId());
            msg.setIsRead(0);
            messageMapper.insert(msg);

            log.info("[高频借贷预警] 用户={}, 等级={}, warningId={}", userId, level, warning.getId());
        } else {
            result.setWarningTriggered(false);
            result.setTriggerReasons(List.of());
            result.setAdvice("借贷行为健康，未触发预警。");
        }

        return result;
    }

    public List<BizRiskWarning> listWarnings(Long userId) {
        return riskWarningMapper.selectList(
                new LambdaQueryWrapper<BizRiskWarning>()
                        .eq(BizRiskWarning::getUserId, userId)
                        .eq(BizRiskWarning::getWarningType, "HIGH_FREQ_BORROW")
                        .orderByDesc(BizRiskWarning::getWarningTime));
    }

    private String buildContent(List<String> reasons, BigDecimal income, BigDecimal repay, BigDecimal ratio) {
        StringBuilder sb = new StringBuilder("【高频借贷/非理性负债预警】\n");
        sb.append("月收入：").append(income).append("元\n");
        sb.append("月还款额：").append(repay).append("元\n");
        sb.append("负债比：").append(ratio.multiply(new BigDecimal("100")).setScale(2, RoundingMode.HALF_UP))
                .append("%\n触发原因：\n");
        for (String r : reasons) {
            sb.append("- ").append(r).append("\n");
        }
        return sb.toString();
    }

    private String buildAdvice(String level, int recentCount, int activeCount, BigDecimal ratio) {
        StringBuilder sb = new StringBuilder("【建议】\n");
        sb.append("1. 暂停新增贷款申请，优先梳理现有债务；\n");
        sb.append("2. 制定还款计划，优先偿还高息贷款；\n");
        sb.append("3. 控制非必要消费，将月还款比降至 50% 以下；\n");
        sb.append("4. 可结合青启e城「预算消费」模块建立月度预算，量入为出；\n");
        sb.append("5. 如确实资金周转困难，可联系正规金融机构协商债务重组。\n");
        sb.append("（以上为模拟建议，不构成投资/融资邀约）");
        return sb.toString();
    }

    @Data
    public static class DetectResult {
        private Long userId;
        private Integer recentAppCount;
        private Integer activeLoanCount;
        private BigDecimal monthlyIncome;
        private BigDecimal monthlyRepay;
        private BigDecimal debtRatio;
        private Boolean warningTriggered;
        private List<String> triggerReasons;
        private String warningLevel;
        private String advice;
        private Long warningId;
    }
}
