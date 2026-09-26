package com.icbc.qingqi.module.budget;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.icbc.qingqi.common.BizException;
import com.icbc.qingqi.module.message.MessageService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 模块4 碎片消费治理：分类预算、交易归类、分级提醒、结余转储蓄
 * 提醒阈值（设计说明书）：剩余50%温和提醒 / 20%紧张提醒 / 超支强提醒
 */
@Service
public class BudgetService {

    private static final BigDecimal RATIO_50 = new BigDecimal("50");
    private static final BigDecimal RATIO_20 = new BigDecimal("20");

    private final BudgetMapper budgetMapper;
    private final TransactionMapper transactionMapper;
    private final GoalSavingMapper goalSavingMapper;
    private final MessageService messageService;

    public BudgetService(BudgetMapper budgetMapper,
                         TransactionMapper transactionMapper,
                         GoalSavingMapper goalSavingMapper,
                         MessageService messageService) {
        this.budgetMapper = budgetMapper;
        this.transactionMapper = transactionMapper;
        this.goalSavingMapper = goalSavingMapper;
        this.messageService = messageService;
    }

    /** 设置/更新某月某分类预算 */
    public Budget setBudget(Long userId, String month, String category, BigDecimal amount) {
        Budget budget = budgetMapper.selectOne(
                Wrappers.<Budget>lambdaQuery()
                        .eq(Budget::getUserId, userId)
                        .eq(Budget::getBudgetMonth, month)
                        .eq(Budget::getCategory, category)
                        .eq(Budget::getStatus, 0));
        if (budget == null) {
            budget = new Budget();
            budget.setUserId(userId);
            budget.setBudgetMonth(month);
            budget.setCategory(category);
            budget.setBudgetAmount(amount);
            budget.setUsedAmount(BigDecimal.ZERO);
            budget.setRemindLevel(0);
            budget.setStatus(0);
            budgetMapper.insert(budget);
        } else {
            budget.setBudgetAmount(amount);
            budgetMapper.updateById(budget);
        }
        recomputeAndSave(budget, false);
        return budget;
    }

    /**
     * 记录一笔支出：MCC 模拟归类 → 预算扣减 → 分级提醒
     */
    @Transactional
    public Transaction recordExpense(Long userId, String merchantName, BigDecimal amount) {
        Transaction tx = new Transaction();
        tx.setUserId(userId);
        tx.setAccountNo("****" + (userId % 10000));
        tx.setTransAmount(amount);
        tx.setTransTime(LocalDateTime.now());
        tx.setMerchantName(merchantName);
        tx.setCategory(classify(merchantName));
        tx.setClassifyMode(0); // 自动归类
        tx.setClassifyStatus(1);
        tx.setTransDirection(0);
        transactionMapper.insert(tx);

        String month = LocalDate.now().toString().substring(0, 7);
        Budget budget = budgetMapper.selectOne(
                Wrappers.<Budget>lambdaQuery()
                        .eq(Budget::getUserId, userId)
                        .eq(Budget::getBudgetMonth, month)
                        .eq(Budget::getCategory, tx.getCategory())
                        .eq(Budget::getStatus, 0));
        if (budget != null) {
            budget.setUsedAmount(budget.getUsedAmount().add(amount));
            recomputeAndSave(budget, true);
        }
        return tx;
    }

    /** 结余转储蓄（预算结余 → 心愿储蓄，演示落账） */
    @Transactional
    public Map<String, Object> carryOverToSaving(Long userId, Long budgetId, String goalName) {
        Budget budget = budgetMapper.selectById(budgetId);
        if (budget == null || !budget.getUserId().equals(userId)) {
            throw new BizException(3001, "预算不存在");
        }
        BigDecimal remaining = budget.getBudgetAmount().subtract(budget.getUsedAmount());
        if (remaining.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BizException(3001, "本期预算无结余，无法转储蓄");
        }
        GoalSaving saving = new GoalSaving();
        saving.setUserId(userId);
        saving.setGoalName(goalName == null || goalName.isBlank() ? "预算结余储蓄" : goalName);
        saving.setCurrentAmount(remaining);
        saving.setSourceType(1); // 预算结余
        saving.setRateGrade("低风险档（模拟）");
        saving.setOpenDate(LocalDate.now());
        saving.setStatus(0);
        goalSavingMapper.insert(saving);

        budget.setCarryoverAmount(remaining);
        budget.setStatus(1);
        budgetMapper.updateById(budget);

        messageService.add(userId, "预算提醒",
                "已将「" + budget.getCategory() + "」结余 " + remaining + " 元转入心愿储蓄（模拟）。");
        return Map.of("savingId", saving.getSavingId(), "carryoverAmount", remaining);
    }

    public List<Budget> listByUserMonth(Long userId, String month) {
        return budgetMapper.selectList(
                Wrappers.<Budget>lambdaQuery()
                        .eq(Budget::getUserId, userId)
                        .eq(month != null && !month.isBlank(), Budget::getBudgetMonth, month)
                        .orderByAsc(Budget::getCategory));
    }

    /**
     * 商户关键词归类（MCC 模拟规则）
     */
    private String classify(String merchantName) {
        if (merchantName == null) {
            return "其他";
        }
        String[] dining = {"餐", "饭", "奶茶", "咖啡", "外卖", "食堂", "夜宵", "美食"};
        String[] entertainment = {"影", "KTV", "游戏", "剧本", "网咖", "演出", "乐"};
        String[] shopping = {"淘宝", "京东", "拼多多", "超市", "商场", "便利店", "优衣库"};
        String[] traffic = {"地铁", "公交", "滴滴", "打车", "加油", "铁路", "出行"};
        if (containsAny(merchantName, dining)) {
            return "餐饮";
        }
        if (containsAny(merchantName, entertainment)) {
            return "娱乐";
        }
        if (containsAny(merchantName, shopping)) {
            return "购物";
        }
        if (containsAny(merchantName, traffic)) {
            return "交通";
        }
        return "其他";
    }

    private boolean containsAny(String text, String[] words) {
        for (String w : words) {
            if (text.contains(w)) {
                return true;
            }
        }
        return false;
    }

    /** 重算剩余比例与提醒等级，必要时发送站内提醒 */
    private void recomputeAndSave(Budget budget, boolean mayNotify) {
        if (budget.getBudgetAmount() == null || budget.getBudgetAmount().compareTo(BigDecimal.ZERO) <= 0) {
            budget.setRemainRatio(BigDecimal.ZERO);
            budget.setRemindLevel(0);
            budgetMapper.updateById(budget);
            return;
        }
        BigDecimal remain = budget.getBudgetAmount().subtract(budget.getUsedAmount());
        BigDecimal ratio = remain.multiply(new BigDecimal("100"))
                .divide(budget.getBudgetAmount(), 2, RoundingMode.HALF_UP);
        budget.setRemainRatio(ratio);
        int level;
        if (budget.getUsedAmount().compareTo(budget.getBudgetAmount()) >= 0) {
            level = 3;
        } else if (ratio.compareTo(RATIO_20) <= 0) {
            level = 2;
        } else if (ratio.compareTo(RATIO_50) <= 0) {
            level = 1;
        } else {
            level = 0;
        }
        boolean levelChanged = budget.getRemindLevel() == null || budget.getRemindLevel() != level;
        budget.setRemindLevel(level);
        budgetMapper.updateById(budget);
        if (mayNotify && levelChanged && level >= 1) {
            String content = switch (level) {
                case 3 -> "【预算超支】您的「" + budget.getCategory() + "」预算已超支 "
                        + budget.getUsedAmount().subtract(budget.getBudgetAmount()) + " 元。";
                case 2 -> "【预算提醒】您的「" + budget.getCategory() + "」预算剩余不足 20%（剩余 " + ratio + "%）。";
                default -> "【预算提醒】您的「" + budget.getCategory() + "」预算已使用过半，剩余 " + ratio + "%。";
            };
            messageService.add(budget.getUserId(), "预算提醒", content);
        }
    }
}
