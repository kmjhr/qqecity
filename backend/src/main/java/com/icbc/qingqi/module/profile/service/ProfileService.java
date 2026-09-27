package com.icbc.qingqi.module.profile.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.icbc.qingqi.module.budget.entity.BizBudgetSetting;
import com.icbc.qingqi.module.budget.entity.BizSavingGoal;
import com.icbc.qingqi.module.budget.entity.BizTransaction;
import com.icbc.qingqi.module.budget.mapper.BizBudgetSettingMapper;
import com.icbc.qingqi.module.budget.mapper.BizSavingGoalMapper;
import com.icbc.qingqi.module.budget.mapper.BizTransactionMapper;
import com.icbc.qingqi.module.bookkeeping.entity.BizBookkeepingRecord;
import com.icbc.qingqi.module.bookkeeping.mapper.BizBookkeepingRecordMapper;
import com.icbc.qingqi.module.consumption.entity.BizCreditReport;
import com.icbc.qingqi.module.consumption.mapper.BizCreditReportMapper;
import com.icbc.qingqi.module.guarantee.entity.BizGuarantee;
import com.icbc.qingqi.module.guarantee.mapper.BizGuaranteeMapper;
import com.icbc.qingqi.module.loan.entity.BizCreditLimit;
import com.icbc.qingqi.module.loan.entity.BizEntrustPayment;
import com.icbc.qingqi.module.loan.mapper.BizCreditLimitMapper;
import com.icbc.qingqi.module.loan.mapper.BizEntrustPaymentMapper;
import com.icbc.qingqi.module.profile.dto.LinkageApplyVO;
import com.icbc.qingqi.module.profile.dto.ProfileVO;
import com.icbc.qingqi.module.risk.entity.BizRiskWarning;
import com.icbc.qingqi.module.risk.mapper.BizRiskWarningMapper;
import com.icbc.qingqi.module.user.entity.SysUser;
import com.icbc.qingqi.module.user.mapper.SysUserMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 青年成长信用画像服务（演示级，模拟）
 * <p>
 * 三场景数据聚合（实时从各模块表读取，不写死）：
 * - 安居（稳定性）：BizGuarantee（保函履约）+ BizTransaction（租金记录）
 * - 创业（经营力）：聚合流水 + BizEntrustPayment（受托支付）+ BizBookkeepingRecord（记账）
 * - 消费（资金健康度）：BizBudgetSetting（预算执行）+ BizSavingGoal（结余储蓄）+ BizCreditLimit（借贷行为）
 * <p>
 * 三维评分 → 联动演示"安居稳+经营好→授信提额、利率优惠"
 */
@Slf4j
@Service
public class ProfileService {

    private final SysUserMapper userMapper;
    private final BizGuaranteeMapper guaranteeMapper;
    private final BizTransactionMapper transactionMapper;
    private final BizEntrustPaymentMapper entrustMapper;
    private final BizBookkeepingRecordMapper bookkeepingMapper;
    private final BizBudgetSettingMapper budgetMapper;
    private final BizSavingGoalMapper savingGoalMapper;
    private final BizCreditLimitMapper creditLimitMapper;
    private final BizRiskWarningMapper riskWarningMapper;
    private final BizCreditReportMapper creditReportMapper;

    public ProfileService(SysUserMapper userMapper,
                          BizGuaranteeMapper guaranteeMapper,
                          BizTransactionMapper transactionMapper,
                          BizEntrustPaymentMapper entrustMapper,
                          BizBookkeepingRecordMapper bookkeepingMapper,
                          BizBudgetSettingMapper budgetMapper,
                          BizSavingGoalMapper savingGoalMapper,
                          BizCreditLimitMapper creditLimitMapper,
                          BizRiskWarningMapper riskWarningMapper,
                          BizCreditReportMapper creditReportMapper) {
        this.userMapper = userMapper;
        this.guaranteeMapper = guaranteeMapper;
        this.transactionMapper = transactionMapper;
        this.entrustMapper = entrustMapper;
        this.bookkeepingMapper = bookkeepingMapper;
        this.budgetMapper = budgetMapper;
        this.savingGoalMapper = savingGoalMapper;
        this.creditLimitMapper = creditLimitMapper;
        this.riskWarningMapper = riskWarningMapper;
        this.creditReportMapper = creditReportMapper;
    }

    /**
     * 拉取画像
     */
    public ProfileVO getProfile(Long userId) {
        SysUser user = userMapper.selectById(userId);
        ProfileVO vo = new ProfileVO();
        vo.setUserId(userId);
        vo.setSimulated(true);
        if (user != null) {
            vo.setUsername(user.getUsername());
            vo.setUserType(user.getUserType());
        }

        // === 安居·稳定性评分 ===
        vo.setStability(scoreStability(userId));

        // === 创业·经营力评分 ===
        vo.setOperation(scoreOperation(userId));

        // === 消费·资金健康度评分 ===
        vo.setFundHealth(scoreFundHealth(userId));

        // 综合评分（30/40/30 加权）
        int overall = vo.getStability().getScore() * 30
                + vo.getOperation().getScore() * 40
                + vo.getFundHealth().getScore() * 30;
        vo.setOverallScore(overall / 100);

        // 成长轨迹
        vo.setGrowthTrack(buildGrowthTrack(userId));

        // 联动演示
        vo.setLinkage(buildLinkage(userId, vo.getStability(), vo.getOperation()));

        return vo;
    }

    /**
     * 联动提额：仅演示，真实写入 BizCreditLimit（A 类）totalLimit/availableLimit/interestRate
     */
    public LinkageApplyVO applyLinkage(Long userId) {
        LinkageApplyVO vo = new LinkageApplyVO();
        vo.setSimulated(true);
        ProfileVO.LinkageDemo linkage = buildLinkage(userId,
                scoreStability(userId), scoreOperation(userId));
        if (!linkage.getEligible()) {
            vo.setApplied(false);
            vo.setNotice("未达联动条件：" + linkage.getExplanation() + "（模拟口径，无法应用提额）");
            return vo;
        }
        // 应用提额到 A 类授信
        BizCreditLimit limit = creditLimitMapper.selectOne(
                new LambdaQueryWrapper<BizCreditLimit>()
                        .eq(BizCreditLimit::getUserId, userId)
                        .eq(BizCreditLimit::getCreditType, "A_TYPE")
                        .last("LIMIT 1"));
        if (limit == null) {
            vo.setApplied(false);
            vo.setNotice("未找到 A 类授信额度，请先申请青创e贷 A 类循环贷（模拟口径）");
            return vo;
        }
        BigDecimal beforeLimit = limit.getTotalLimit();
        BigDecimal afterLimit = beforeLimit.add(linkage.getUpliftAmount());
        limit.setTotalLimit(afterLimit);
        limit.setAvailableLimit(afterLimit.subtract(limit.getUsedLimit()));
        limit.setInterestRate(linkage.getAfterRate());
        creditLimitMapper.updateById(limit);

        vo.setApplied(true);
        vo.setTotalLimit(afterLimit);
        vo.setAvailableLimit(limit.getAvailableLimit());
        vo.setInterestRate(limit.getInterestRate());
        vo.setNotice("联动提额已应用（模拟）：总额度 " + beforeLimit + " → " + afterLimit
                + "，利率 " + linkage.getBeforeRate() + " → " + linkage.getAfterRate());
        return vo;
    }

    // ============================================================
    // 评分实现
    // ============================================================

    private ProfileVO.ScoreDetail scoreStability(Long userId) {
        ProfileVO.ScoreDetail d = new ProfileVO.ScoreDetail();
        d.setScene("HOUSING");
        d.setDimensionName("稳定性（安居）");
        List<String> evidences = new ArrayList<>();
        List<ProfileVO.DataPoint> dataPoints = new ArrayList<>();

        // 保函履约
        List<BizGuarantee> guarantees = guaranteeMapper.selectList(
                new LambdaQueryWrapper<BizGuarantee>().eq(BizGuarantee::getTenantId, userId));
        long activeGuarantee = guarantees.stream().filter(g -> "ACTIVE".equals(g.getGuaranteeStatus())).count();
        long claimedGuarantee = guarantees.stream().filter(g -> "CLAIMED".equals(g.getGuaranteeStatus())).count();
        dataPoints.add(buildDataPoint("保函数量", String.valueOf(guarantees.size())));
        dataPoints.add(buildDataPoint("有效保函", String.valueOf(activeGuarantee)));
        dataPoints.add(buildDataPoint("保函索赔", String.valueOf(claimedGuarantee)));
        if (activeGuarantee > 0 && claimedGuarantee == 0) {
            evidences.add("保函有效且无索赔记录，履约表现良好");
        } else if (claimedGuarantee > 0) {
            evidences.add("存在 " + claimedGuarantee + " 次保函索赔，扣分");
        }

        // 租金支付（交易记录中含租金描述）
        List<BizTransaction> rentTxns = transactionMapper.selectList(
                new LambdaQueryWrapper<BizTransaction>()
                        .eq(BizTransaction::getUserId, userId)
                        .like(BizTransaction::getDescription, "租金"));
        dataPoints.add(buildDataPoint("租金支付记录", String.valueOf(rentTxns.size())));
        if (rentTxns.size() >= 3) {
            evidences.add("租金支付记录 " + rentTxns.size() + " 笔，按时支付");
        }

        int score = 40;
        if (activeGuarantee > 0) score += 30;
        if (claimedGuarantee == 0) score += 20; else score -= 20;
        if (rentTxns.size() >= 3) score += 10;
        score = clamp(score);

        d.setScore(score);
        d.setLevel(score >= 70 ? "HIGH" : score >= 40 ? "MEDIUM" : "LOW");
        d.setEvidences(evidences);
        d.setDataPoints(dataPoints);
        return d;
    }

    private ProfileVO.ScoreDetail scoreOperation(Long userId) {
        ProfileVO.ScoreDetail d = new ProfileVO.ScoreDetail();
        d.setScene("ENTREPRENEUR");
        d.setDimensionName("经营力（创业）");
        List<String> evidences = new ArrayList<>();
        List<ProfileVO.DataPoint> dataPoints = new ArrayList<>();

        // 受托支付
        List<BizEntrustPayment> payments = entrustMapper.selectList(
                new LambdaQueryWrapper<BizEntrustPayment>().eq(BizEntrustPayment::getUserId, userId));
        long successPay = payments.stream().filter(p -> "SUCCESS".equals(p.getPaymentStatus())).count();
        BigDecimal payAmount = payments.stream()
                .filter(p -> "SUCCESS".equals(p.getPaymentStatus()))
                .map(BizEntrustPayment::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        dataPoints.add(buildDataPoint("受托支付笔数", String.valueOf(successPay)));
        dataPoints.add(buildDataPoint("受托支付总额", payAmount.toPlainString()));

        // 记账数据
        List<BizBookkeepingRecord> records = bookkeepingMapper.selectList(
                new LambdaQueryWrapper<BizBookkeepingRecord>().eq(BizBookkeepingRecord::getUserId, userId));
        long incomeRecords = records.stream().filter(r -> "INCOME".equals(r.getRecordType())).count();
        BigDecimal incomeAmount = records.stream()
                .filter(r -> "INCOME".equals(r.getRecordType()))
                .map(BizBookkeepingRecord::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        dataPoints.add(buildDataPoint("记账收入笔数", String.valueOf(incomeRecords)));
        dataPoints.add(buildDataPoint("记账收入总额", incomeAmount.toPlainString()));

        if (successPay >= 2) evidences.add("受托支付活跃，" + successPay + " 笔成功支付");
        if (incomeRecords >= 3) evidences.add("记账收入 " + incomeRecords + " 笔，经营记录规范");

        int score = 30;
        if (successPay >= 2) score += 25;
        if (successPay >= 5) score += 15;
        if (incomeRecords >= 3) score += 15;
        if (incomeAmount.compareTo(new BigDecimal("10000")) > 0) score += 15;
        score = clamp(score);

        d.setScore(score);
        d.setLevel(score >= 70 ? "HIGH" : score >= 40 ? "MEDIUM" : "LOW");
        d.setEvidences(evidences);
        d.setDataPoints(dataPoints);
        return d;
    }

    private ProfileVO.ScoreDetail scoreFundHealth(Long userId) {
        ProfileVO.ScoreDetail d = new ProfileVO.ScoreDetail();
        d.setScene("CONSUMPTION");
        d.setDimensionName("资金健康度（消费）");
        List<String> evidences = new ArrayList<>();
        List<ProfileVO.DataPoint> dataPoints = new ArrayList<>();

        // 预算执行
        List<BizBudgetSetting> budgets = budgetMapper.selectList(
                new LambdaQueryWrapper<BizBudgetSetting>().eq(BizBudgetSetting::getUserId, userId));
        int budgetsUnder = (int) budgets.stream()
                .filter(b -> b.getUsagePercent() != null && b.getUsagePercent().compareTo(new BigDecimal("100")) < 0)
                .count();
        int budgetsOver = (int) budgets.stream()
                .filter(b -> b.getUsagePercent() != null && b.getUsagePercent().compareTo(new BigDecimal("100")) >= 0)
                .count();
        dataPoints.add(buildDataPoint("预算分类数", String.valueOf(budgets.size())));
        dataPoints.add(buildDataPoint("未超支预算", String.valueOf(budgetsUnder)));
        dataPoints.add(buildDataPoint("超支预算", String.valueOf(budgetsOver)));

        // 储蓄目标
        List<BizSavingGoal> savings = savingGoalMapper.selectList(
                new LambdaQueryWrapper<BizSavingGoal>().eq(BizSavingGoal::getUserId, userId));
        long activeGoals = savings.stream().filter(s -> "ACTIVE".equals(s.getStatus())).count();
        BigDecimal savedAmount = savings.stream()
                .map(BizSavingGoal::getCurrentAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        dataPoints.add(buildDataPoint("储蓄目标数", String.valueOf(activeGoals)));
        dataPoints.add(buildDataPoint("已储蓄金额", savedAmount.toPlainString()));

        // 借贷行为（风险预警）
        long overdueRiskCount = riskWarningMapper.selectCount(
                new LambdaQueryWrapper<BizRiskWarning>()
                        .eq(BizRiskWarning::getUserId, userId)
                        .eq(BizRiskWarning::getWarningType, "OVERDUE_RISK"));
        long highFreqCount = riskWarningMapper.selectCount(
                new LambdaQueryWrapper<BizRiskWarning>()
                        .eq(BizRiskWarning::getUserId, userId)
                        .eq(BizRiskWarning::getWarningType, "HIGH_FREQ_BORROW"));
        dataPoints.add(buildDataPoint("逾期风险预警", String.valueOf(overdueRiskCount)));
        dataPoints.add(buildDataPoint("高频借贷预警", String.valueOf(highFreqCount)));

        if (budgets.size() > 0 && budgetsOver == 0) evidences.add("所有预算均未超支，消费自律");
        if (activeGoals > 0) evidences.add("进行中的储蓄目标 " + activeGoals + " 个，已储蓄 " + savedAmount + "元");
        if (overdueRiskCount == 0 && highFreqCount == 0) evidences.add("无逾期/高频借贷预警");

        int score = 40;
        if (budgets.size() > 0 && budgetsOver == 0) score += 25;
        if (activeGoals > 0) score += 15;
        if (overdueRiskCount == 0) score += 10; else score -= 20;
        if (highFreqCount == 0) score += 10; else score -= 15;
        score = clamp(score);

        d.setScore(score);
        d.setLevel(score >= 70 ? "HIGH" : score >= 40 ? "MEDIUM" : "LOW");
        d.setEvidences(evidences);
        d.setDataPoints(dataPoints);
        return d;
    }

    private List<ProfileVO.GrowthTrack> buildGrowthTrack(Long userId) {
        List<ProfileVO.GrowthTrack> list = new ArrayList<>();
        LocalDate now = LocalDate.now();
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM");

        // 保函成长轨迹
        List<BizGuarantee> guarantees = guaranteeMapper.selectList(
                new LambdaQueryWrapper<BizGuarantee>().eq(BizGuarantee::getTenantId, userId));
        for (BizGuarantee g : guarantees) {
            if (g.getCreateTime() != null) {
                ProfileVO.GrowthTrack t = new ProfileVO.GrowthTrack();
                t.setPeriod(g.getCreateTime().toLocalDate().format(fmt));
                t.setEvent("保函开立（" + g.getGuaranteeNo() + "），金额 " + g.getGuaranteeAmount());
                t.setScene("HOUSING");
                t.setScoreDelta(5);
                list.add(t);
            }
        }

        // 受托支付轨迹
        List<BizEntrustPayment> payments = entrustMapper.selectList(
                new LambdaQueryWrapper<BizEntrustPayment>().eq(BizEntrustPayment::getUserId, userId));
        for (BizEntrustPayment p : payments) {
            if (p.getPaymentStatus() == "SUCCESS" && p.getPaymentTime() != null) {
                ProfileVO.GrowthTrack t = new ProfileVO.GrowthTrack();
                t.setPeriod(p.getPaymentTime().toLocalDate().format(fmt));
                t.setEvent("受托支付成功（" + p.getPaymentNo() + "），" + p.getAmount() + "元");
                t.setScene("ENTREPRENEUR");
                t.setScoreDelta(3);
                list.add(t);
            }
        }

        // 记账记录轨迹
        List<BizBookkeepingRecord> records = bookkeepingMapper.selectList(
                new LambdaQueryWrapper<BizBookkeepingRecord>().eq(BizBookkeepingRecord::getUserId, userId));
        for (BizBookkeepingRecord r : records) {
            if (r.getHappenDate() != null && "INCOME".equals(r.getRecordType())) {
                ProfileVO.GrowthTrack t = new ProfileVO.GrowthTrack();
                t.setPeriod(r.getHappenDate().format(fmt));
                t.setEvent("记账收入（" + r.getCategory() + "）" + r.getAmount() + "元");
                t.setScene("ENTREPRENEUR");
                t.setScoreDelta(1);
                list.add(t);
            }
        }

        // 储蓄目标轨迹
        List<BizSavingGoal> savings = savingGoalMapper.selectList(
                new LambdaQueryWrapper<BizSavingGoal>().eq(BizSavingGoal::getUserId, userId));
        for (BizSavingGoal s : savings) {
            if (s.getCreateTime() != null) {
                ProfileVO.GrowthTrack t = new ProfileVO.GrowthTrack();
                t.setPeriod(s.getCreateTime().toLocalDate().format(fmt));
                t.setEvent("储蓄目标：" + s.getGoalName() + "，已存 " + s.getCurrentAmount());
                t.setScene("CONSUMPTION");
                t.setScoreDelta(2);
                list.add(t);
            }
        }

        list.sort(Comparator.comparing(ProfileVO.GrowthTrack::getPeriod).reversed());
        return list.stream().limit(20).collect(Collectors.toList());
    }

    private ProfileVO.LinkageDemo buildLinkage(Long userId,
                                              ProfileVO.ScoreDetail stability,
                                              ProfileVO.ScoreDetail operation) {
        ProfileVO.LinkageDemo demo = new ProfileVO.LinkageDemo();
        demo.setSimulated(true);
        BizCreditLimit limit = creditLimitMapper.selectOne(
                new LambdaQueryWrapper<BizCreditLimit>()
                        .eq(BizCreditLimit::getUserId, userId)
                        .eq(BizCreditLimit::getCreditType, "A_TYPE")
                        .last("LIMIT 1"));
        BigDecimal currentLimit = limit != null ? limit.getTotalLimit() : new BigDecimal("50000");
        BigDecimal currentRate = limit != null && limit.getInterestRate() != null
                ? limit.getInterestRate() : new BigDecimal("3.85");

        boolean stableHousing = stability.getScore() >= 60;
        boolean goodOperation = operation.getScore() >= 60;
        boolean eligible = stableHousing && goodOperation;

        demo.setEligible(eligible);
        demo.setBeforeLimit(currentLimit);
        demo.setBeforeRate(currentRate);

        if (eligible) {
            BigDecimal uplift = currentLimit.multiply(new BigDecimal("0.20")).setScale(2, RoundingMode.HALF_UP);
            BigDecimal afterLimit = currentLimit.add(uplift).min(new BigDecimal("80000"));
            BigDecimal afterRate = currentRate.subtract(new BigDecimal("0.30"))
                    .max(new BigDecimal("3.00"));
            demo.setUpliftAmount(uplift);
            demo.setAfterLimit(afterLimit);
            demo.setAfterRate(afterRate);
            demo.setExplanation("安居稳（稳定性≥60）+ 经营好（经营力≥60）→ 授信提额 20%（上限 8 万）"
                    + "，利率优惠 0.30pct（下限 3.00%）");
        } else {
            demo.setUpliftAmount(BigDecimal.ZERO);
            demo.setAfterLimit(currentLimit);
            demo.setAfterRate(currentRate);
            demo.setExplanation("未达联动条件（需稳定性≥60 且 经营力≥60，当前 稳定性="
                    + stability.getScore() + " 经营力=" + operation.getScore() + "）");
        }
        return demo;
    }

    private ProfileVO.DataPoint buildDataPoint(String label, String value) {
        ProfileVO.DataPoint p = new ProfileVO.DataPoint();
        p.setLabel(label);
        p.setValue(value);
        return p;
    }

    private int clamp(int score) {
        return Math.max(0, Math.min(100, score));
    }
}
