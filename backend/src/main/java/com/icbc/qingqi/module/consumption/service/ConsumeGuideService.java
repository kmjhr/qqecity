package com.icbc.qingqi.module.consumption.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.icbc.qingqi.module.bookkeeping.entity.BizBookkeepingRecord;
import com.icbc.qingqi.module.bookkeeping.mapper.BizBookkeepingRecordMapper;
import com.icbc.qingqi.module.budget.entity.BizBudgetSetting;
import com.icbc.qingqi.module.budget.entity.BizTransaction;
import com.icbc.qingqi.module.budget.mapper.BizBudgetSettingMapper;
import com.icbc.qingqi.module.budget.mapper.BizTransactionMapper;
import com.icbc.qingqi.module.message.entity.SysMessage;
import com.icbc.qingqi.module.message.mapper.SysMessageMapper;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

/**
 * 三层渐进式消费引导服务
 * <p>
 * 缺口 #18 三层渐进式消费引导（简化）
 * ① 交易后即时推送：模拟交易触发（金额 > 预算月度 30%）
 * ② 月度账单分析：消费结构诊断 + 负债预警
 * ③ 支付前实时提醒：演示页（标注"仅工行自有支付场景"）
 */
@Slf4j
@Service
public class ConsumeGuideService {

    private static final BigDecimal LARGE_TXN_RATIO = new BigDecimal("0.30");
    private static final String COMPLIANCE_NOTICE =
            "本提示仅适用于工行自有支付场景（模拟演示），不涉及第三方支付。";

    private final BizTransactionMapper transactionMapper;
    private final BizBudgetSettingMapper budgetSettingMapper;
    private final BizBookkeepingRecordMapper bookkeepingMapper;
    private final SysMessageMapper messageMapper;

    public ConsumeGuideService(BizTransactionMapper transactionMapper,
                               BizBudgetSettingMapper budgetSettingMapper,
                               BizBookkeepingRecordMapper bookkeepingMapper,
                               SysMessageMapper messageMapper) {
        this.transactionMapper = transactionMapper;
        this.budgetSettingMapper = budgetSettingMapper;
        this.bookkeepingMapper = bookkeepingMapper;
        this.messageMapper = messageMapper;
    }

    // ============================================================
    //  第一层：交易后即时推送
    // ============================================================

    /**
     * 交易后即时推送（模拟交易触发）
     * <p>
     * 检测指定交易是否为大额（> 预算月度 30%），命中则发站内信
     *
     * @param userId        用户ID
     * @param transactionId 交易ID
     */
    @Transactional(rollbackFor = Exception.class)
    public AfterTxnResult afterTransactionPush(Long userId, Long transactionId) {
        BizTransaction txn = transactionMapper.selectById(transactionId);
        if (txn == null || !userId.equals(txn.getUserId())) {
            return null;
        }

        AfterTxnResult result = new AfterTxnResult();
        result.setTransactionId(transactionId);
        result.setAmount(txn.getAmount());
        result.setMerchantName(txn.getMerchantName());

        // 查找当月预算
        String currentMonth = LocalDate.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM"));
        List<BizBudgetSetting> settings = budgetSettingMapper.selectList(
                new LambdaQueryWrapper<BizBudgetSetting>()
                        .eq(BizBudgetSetting::getUserId, userId)
                        .likeRight(BizBudgetSetting::getBudgetPeriod, currentMonth));

        BigDecimal budgetTotal = settings.stream()
                .map(BizBudgetSetting::getBudgetAmount)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal threshold = budgetTotal.multiply(LARGE_TXN_RATIO);
        boolean isLarge = budgetTotal.compareTo(BigDecimal.ZERO) > 0
                && txn.getAmount().compareTo(threshold) > 0;

        result.setMonthlyBudgetTotal(budgetTotal);
        result.setLargeThreshold(threshold);
        result.setIsLarge(isLarge);

        if (isLarge) {
            String content = "您刚发生一笔交易 ¥" + txn.getAmount()
                    + "（" + (txn.getMerchantName() != null ? txn.getMerchantName() : "未知名商户") + "），"
                    + "占本月预算总额的 "
                    + txn.getAmount().multiply(new BigDecimal("100")).divide(budgetTotal, 2, RoundingMode.HALF_UP)
                    + "%，超过 30% 大额阈值，建议核查消费必要性。";

            SysMessage msg = new SysMessage();
            msg.setUserId(userId);
            msg.setTitle("交易后即时提醒：检测到大额消费");
            msg.setContent(content);
            msg.setType("BUDGET");
            msg.setBizType("LARGE_TXN");
            msg.setBizId(transactionId);
            msg.setIsRead(0);
            messageMapper.insert(msg);

            result.setMessageSent(true);
            result.setAdvice(content);
            log.info("[消费引导-交易后] 用户={}, 交易={}, 大额预警已发", userId, transactionId);
        } else {
            result.setMessageSent(false);
            result.setAdvice("交易金额正常，未触发大额预警。");
        }

        return result;
    }

    // ============================================================
    //  第二层：月度账单分析
    // ============================================================

    public MonthlyBillAnalysis analyzeMonthlyBill(Long userId, String period) {
        String p = period != null ? period
                : LocalDate.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM"));
        LocalDate start = LocalDate.parse(p + "-01");
        LocalDate end = start.plusMonths(1).minusDays(1);

        List<BizTransaction> txns = transactionMapper.selectList(
                new LambdaQueryWrapper<BizTransaction>()
                        .eq(BizTransaction::getUserId, userId)
                        .between(BizTransaction::getTransactionTime, start.atStartOfDay(), end.atTime(23, 59, 59)));

        BigDecimal totalIncome = txns.stream()
                .filter(t -> "INCOME".equals(t.getTransactionType()))
                .map(BizTransaction::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalExpense = txns.stream()
                .filter(t -> "EXPENSE".equals(t.getTransactionType()))
                .map(BizTransaction::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal netCashFlow = totalIncome.subtract(totalExpense);

        // 按分类汇总支出
        Map<String, BigDecimal> expenseByCategory = new LinkedHashMap<>();
        for (BizTransaction t : txns) {
            if (!"EXPENSE".equals(t.getTransactionType())) continue;
            String cat = t.getCategoryCode() != null ? t.getCategoryCode() : "OTHER";
            expenseByCategory.merge(cat, t.getAmount(), BigDecimal::add);
        }

        // 找最大支出分类
        Map.Entry<String, BigDecimal> topCategory = expenseByCategory.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .orElse(null);

        // 负债预警：月支出 > 月收入
        boolean debtWarning = totalExpense.compareTo(totalIncome) > 0;
        BigDecimal savingsRate = totalIncome.compareTo(BigDecimal.ZERO) > 0
                ? netCashFlow.multiply(new BigDecimal("100")).divide(totalIncome, 2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        List<String> advices = new ArrayList<>();
        if (debtWarning) {
            advices.add("本月支出 ¥" + totalExpense + " 超过收入 ¥" + totalIncome + "，出现赤字 ¥"
                    + netCashFlow.abs() + "，建议优先削减非必要消费。");
        }
        if (topCategory != null && topCategory.getValue().compareTo(totalExpense.multiply(new BigDecimal("0.4"))) > 0) {
            advices.add("「" + topCategory.getKey() + "」分类支出占比过高（"
                    + topCategory.getValue().multiply(new BigDecimal("100")).divide(totalExpense, 2, RoundingMode.HALF_UP)
                    + "%），建议关注该分类消费。");
        }
        if (savingsRate.compareTo(new BigDecimal("20")) < 0) {
            advices.add("本月储蓄率仅 " + savingsRate + "%，低于健康水平 20%，建议提升结余能力。");
        }
        if (advices.isEmpty()) {
            advices.add("本月消费结构健康，无风险预警。继续保持。");
        }

        MonthlyBillAnalysis result = new MonthlyBillAnalysis();
        result.setUserId(userId);
        result.setPeriod(p);
        result.setTotalIncome(totalIncome);
        result.setTotalExpense(totalExpense);
        result.setNetCashFlow(netCashFlow);
        result.setSavingsRate(savingsRate);
        result.setExpenseByCategory(expenseByCategory);
        result.setTopCategory(topCategory != null ? topCategory.getKey() : null);
        result.setTopCategoryAmount(topCategory != null ? topCategory.getValue() : BigDecimal.ZERO);
        result.setDebtWarning(debtWarning);
        result.setAdvices(advices);
        return result;
    }

    // ============================================================
    //  第三层：支付前实时提醒演示页
    // ============================================================

    public PayBeforeReminder payBeforeReminder(Long userId, BigDecimal amount, String merchantName) {
        PayBeforeReminder reminder = new PayBeforeReminder();
        reminder.setUserId(userId);
        reminder.setAmount(amount);
        reminder.setMerchantName(merchantName);
        reminder.setComplianceNotice(COMPLIANCE_NOTICE);

        List<String> tips = new ArrayList<>();
        // 1. 预算检查
        String currentMonth = LocalDate.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM"));
        List<BizBudgetSetting> settings = budgetSettingMapper.selectList(
                new LambdaQueryWrapper<BizBudgetSetting>()
                        .eq(BizBudgetSetting::getUserId, userId)
                        .likeRight(BizBudgetSetting::getBudgetPeriod, currentMonth));
        BigDecimal totalBudget = settings.stream()
                .map(BizBudgetSetting::getBudgetAmount)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalUsed = settings.stream()
                .map(BizBudgetSetting::getUsedAmount)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal remaining = totalBudget.subtract(totalUsed);

        if (totalBudget.compareTo(BigDecimal.ZERO) > 0) {
            tips.add("本月预算剩余 ¥" + remaining + "（已用 "
                    + totalUsed.multiply(new BigDecimal("100")).divide(totalBudget, 2, RoundingMode.HALF_UP) + "%）。");
            if (amount.compareTo(remaining) > 0) {
                tips.add("⚠️ 本笔支付 ¥" + amount + " 将超出本月剩余预算 ¥" + remaining.abs() + "，请审慎确认。");
            } else if (amount.compareTo(remaining.multiply(LARGE_TXN_RATIO)) > 0) {
                tips.add("本笔支付金额较大，占剩余预算 "
                        + amount.multiply(new BigDecimal("100")).divide(remaining, 2, RoundingMode.HALF_UP)
                        + "%，建议核查必要性。");
            }
        }

        // 2. 大额支付建议
        if (amount.compareTo(new BigDecimal("5000")) > 0) {
            tips.add("支付金额 ¥" + amount + " 属于大额支付，建议二次确认商户信息「"
                    + (merchantName != null ? merchantName : "未知名") + "」是否正确。");
        }

        // 3. 反诈提示
        tips.add("如该交易对方要求加急、保密、转账到个人账户，请立即停止并拨打 95588 工行客服核实。");

        reminder.setTips(tips);
        reminder.setMonthlyBudgetTotal(totalBudget);
        reminder.setMonthlyBudgetUsed(totalUsed);
        reminder.setMonthlyBudgetRemaining(remaining);
        return reminder;
    }

    // ============================================================
    //  VO 类
    // ============================================================

    @Data
    public static class AfterTxnResult {
        private Long transactionId;
        private BigDecimal amount;
        private String merchantName;
        private BigDecimal monthlyBudgetTotal;
        private BigDecimal largeThreshold;
        private Boolean isLarge;
        private Boolean messageSent;
        private String advice;
    }

    @Data
    public static class MonthlyBillAnalysis {
        private Long userId;
        private String period;
        private BigDecimal totalIncome;
        private BigDecimal totalExpense;
        private BigDecimal netCashFlow;
        private BigDecimal savingsRate;
        private Map<String, BigDecimal> expenseByCategory;
        private String topCategory;
        private BigDecimal topCategoryAmount;
        private Boolean debtWarning;
        private List<String> advices;
    }

    @Data
    public static class PayBeforeReminder {
        private Long userId;
        private BigDecimal amount;
        private String merchantName;
        private BigDecimal monthlyBudgetTotal;
        private BigDecimal monthlyBudgetUsed;
        private BigDecimal monthlyBudgetRemaining;
        private List<String> tips;
        private String complianceNotice;
    }
}
