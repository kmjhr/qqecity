package com.icbc.qingqi.module.budget.service;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.icbc.qingqi.common.BizException;
import com.icbc.qingqi.common.ErrorCode;
import com.icbc.qingqi.module.budget.dto.BudgetSettingDTO;
import com.icbc.qingqi.module.budget.dto.TransactionDTO;
import com.icbc.qingqi.module.budget.dto.TransferSavingDTO;
import com.icbc.qingqi.module.budget.entity.*;
import com.icbc.qingqi.module.budget.mapper.*;
import com.icbc.qingqi.module.message.entity.SysMessage;
import com.icbc.qingqi.module.message.mapper.SysMessageMapper;
import com.icbc.qingqi.module.risk.entity.BizRiskWarning;
import com.icbc.qingqi.module.risk.mapper.BizRiskWarningMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * 碎片消费治理服务
 * <p>
 * 覆盖 C-1 ~ C-4：
 * - C-1 分类预算设置
 * - C-2 模拟交易按 MCC 归类并实时扣减
 * - C-3 三级提醒（剩余50%→20%→超支）
 * - C-4 结余一键转入心愿储蓄
 */
@Slf4j
@Service
public class BudgetService {

    private final BizBudgetCategoryMapper categoryMapper;
    private final BizBudgetSettingMapper settingMapper;
    private final BizTransactionMapper transactionMapper;
    private final BizSavingGoalMapper savingGoalMapper;
    private final SysMessageMapper messageMapper;
    private final BizRiskWarningMapper riskWarningMapper;

    // MCC → 分类编码 映射
    private static final Map<String, String> MCC_TO_CATEGORY = Map.ofEntries(
            Map.entry("5812", "FOOD"),
            Map.entry("5814", "FOOD"),
            Map.entry("5811", "FOOD"),
            Map.entry("5462", "FOOD"),
            Map.entry("7832", "ENTERTAINMENT"),
            Map.entry("7833", "ENTERTAINMENT"),
            Map.entry("7922", "ENTERTAINMENT"),
            Map.entry("5699", "SHOPPING"),
            Map.entry("5311", "SHOPPING"),
            Map.entry("5411", "SHOPPING"),
            Map.entry("5651", "SHOPPING"),
            Map.entry("4111", "TRANSPORT"),
            Map.entry("4121", "TRANSPORT"),
            Map.entry("4131", "TRANSPORT"),
            Map.entry("6538", "HOUSING"),
            Map.entry("7011", "HOUSING")
    );

    public BudgetService(BizBudgetCategoryMapper categoryMapper,
                         BizBudgetSettingMapper settingMapper,
                         BizTransactionMapper transactionMapper,
                         BizSavingGoalMapper savingGoalMapper,
                         SysMessageMapper messageMapper,
                         BizRiskWarningMapper riskWarningMapper) {
        this.categoryMapper = categoryMapper;
        this.settingMapper = settingMapper;
        this.transactionMapper = transactionMapper;
        this.savingGoalMapper = savingGoalMapper;
        this.messageMapper = messageMapper;
        this.riskWarningMapper = riskWarningMapper;
    }

    // ============================================================
    //  C-1 分类预算设置
    // ============================================================

    @Transactional(rollbackFor = Exception.class)
    public BizBudgetSetting setBudget(Long userId, BudgetSettingDTO dto) {
        BizBudgetCategory category = categoryMapper.selectById(dto.getCategoryId());
        if (category == null || category.getStatus() == null || category.getStatus() != 1) {
            throw new BizException(ErrorCode.BIZ_RULE_NOT_MET, "预算分类不存在或已禁用");
        }
        String period = dto.getBudgetPeriod() != null ? dto.getBudgetPeriod() : currentPeriod();

        // 查找已有设置（同用户+分类+周期），存在则更新，不存在则新建
        BizBudgetSetting existing = settingMapper.selectOne(
                new LambdaQueryWrapper<BizBudgetSetting>()
                        .eq(BizBudgetSetting::getUserId, userId)
                        .eq(BizBudgetSetting::getCategoryId, dto.getCategoryId())
                        .eq(BizBudgetSetting::getBudgetPeriod, period));
        if (existing != null) {
            existing.setBudgetAmount(dto.getBudgetAmount());
            existing.setRemainingAmount(dto.getBudgetAmount().subtract(
                    existing.getUsedAmount() != null ? existing.getUsedAmount() : BigDecimal.ZERO));
            existing.setUsagePercent(calcUsagePercent(existing.getUsedAmount(), dto.getBudgetAmount()));
            settingMapper.updateById(existing);
            return existing;
        }

        BizBudgetSetting setting = new BizBudgetSetting();
        setting.setUserId(userId);
        setting.setCategoryId(category.getId());
        setting.setCategoryCode(category.getCategoryCode());
        setting.setBudgetPeriod(period);
        setting.setBudgetAmount(dto.getBudgetAmount());
        setting.setUsedAmount(BigDecimal.ZERO);
        setting.setRemainingAmount(dto.getBudgetAmount());
        setting.setUsagePercent(BigDecimal.ZERO);
        setting.setRemind50Sent(0);
        setting.setRemind20Sent(0);
        setting.setRemindOverSent(0);
        settingMapper.insert(setting);
        return setting;
    }

    // ============================================================
    //  C-2 模拟交易（MCC 归类 + 实时扣减 + C-3 三级提醒）
    // ============================================================

    /**
     * 新增模拟交易，按 MCC 自动归类并扣减预算，返回触发的提醒级别
     *
     * @return Map 含 transaction 与 remindLevel（0无/1温和/2紧张/3超支）及 remindMessage
     */
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> addTransaction(Long userId, TransactionDTO dto) {
        // MCC → 分类
        String categoryCode = mapMccToCategory(dto.getMccCode());
        BizBudgetCategory category = categoryMapper.selectOne(
                new LambdaQueryWrapper<BizBudgetCategory>().eq(BizBudgetCategory::getCategoryCode, categoryCode));

        // 创建交易记录
        BizTransaction tx = new BizTransaction();
        tx.setUserId(userId);
        tx.setTransactionNo(generateNo("TX"));
        tx.setTransactionType(dto.getTransactionType());
        if (category != null) {
            tx.setCategoryId(category.getId());
            tx.setCategoryCode(category.getCategoryCode());
        }
        tx.setAmount(dto.getAmount());
        tx.setMerchantName(dto.getMerchantName());
        tx.setMccCode(dto.getMccCode());
        tx.setTransactionTime(LocalDateTime.now());
        tx.setDescription(dto.getDescription());
        tx.setSource("SIMULATED");

        Map<String, Object> result = new LinkedHashMap<>();
        int remindLevel = 0;
        String remindMessage = null;

        // 支出类交易扣减预算
        if ("EXPENSE".equals(dto.getTransactionType()) && category != null) {
            String period = currentPeriod();
            BizBudgetSetting setting = settingMapper.selectOne(
                    new LambdaQueryWrapper<BizBudgetSetting>()
                            .eq(BizBudgetSetting::getUserId, userId)
                            .eq(BizBudgetSetting::getCategoryId, category.getId())
                            .eq(BizBudgetSetting::getBudgetPeriod, period));

            if (setting != null) {
                BigDecimal newUsed = setting.getUsedAmount().add(dto.getAmount());
                BigDecimal newRemaining = setting.getBudgetAmount().subtract(newUsed);
                BigDecimal percent = calcUsagePercent(newUsed, setting.getBudgetAmount());

                setting.setUsedAmount(newUsed);
                setting.setRemainingAmount(newRemaining);
                setting.setUsagePercent(percent);
                tx.setBudgetId(setting.getId());

                // C-3 三级提醒判定
                remindLevel = calcRemindLevel(percent, setting);
                remindMessage = buildRemindMessage(remindLevel, category.getCategoryName(), newRemaining, percent);

                // 站内信 + 风险预警双通道触达（去重：每档仅首次触发）
                boolean shouldSend = shouldSendRemind(setting, remindLevel);
                if (shouldSend && remindLevel > 0) {
                    // 站内信
                    sendBudgetMessage(userId, remindLevel, remindMessage, setting.getId());
                    // 超支同步落 biz_risk_warning
                    if (remindLevel == 3) {
                        saveBudgetRiskWarning(userId, category.getCategoryName(),
                                newRemaining, percent, setting.getId());
                    }
                }

                // 标记提醒已发送（去重）
                markRemindSent(setting, remindLevel);
                settingMapper.updateById(setting);
            }
        }

        transactionMapper.insert(tx);

        result.put("transaction", tx);
        result.put("mappedCategory", category != null ? category.getCategoryName() : "其他支出");
        result.put("remindLevel", remindLevel);
        result.put("remindMessage", remindMessage);
        return result;
    }

    // ============================================================
    //  C-3 预算列表（含提醒级别）
    // ============================================================

    public List<Map<String, Object>> getBudgetList(Long userId, String period) {
        String p = period != null ? period : currentPeriod();
        List<BizBudgetSetting> settings = settingMapper.selectList(
                new LambdaQueryWrapper<BizBudgetSetting>()
                        .eq(BizBudgetSetting::getUserId, userId)
                        .eq(BizBudgetSetting::getBudgetPeriod, p));

        List<Map<String, Object>> list = new ArrayList<>();
        for (BizBudgetSetting s : settings) {
            BizBudgetCategory cat = categoryMapper.selectById(s.getCategoryId());
            int level = calcRemindLevel(s.getUsagePercent(), s);
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("id", s.getId());
            item.put("categoryId", s.getCategoryId());
            item.put("categoryCode", s.getCategoryCode());
            item.put("categoryName", cat != null ? cat.getCategoryName() : s.getCategoryCode());
            item.put("budgetAmount", s.getBudgetAmount());
            item.put("usedAmount", s.getUsedAmount());
            item.put("remainingAmount", s.getRemainingAmount());
            item.put("usagePercent", s.getUsagePercent());
            item.put("remindLevel", level);
            item.put("remindName", remindName(level));
            list.add(item);
        }
        return list;
    }

    // ============================================================
    //  C-4 结余一键转入心愿储蓄
    // ============================================================

    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> transferSaving(Long userId, TransferSavingDTO dto) {
        String period = currentPeriod();
        List<BizBudgetSetting> settings = settingMapper.selectList(
                new LambdaQueryWrapper<BizBudgetSetting>()
                        .eq(BizBudgetSetting::getUserId, userId)
                        .eq(BizBudgetSetting::getBudgetPeriod, period));

        // 结余 = 各分类剩余金额之和（仅正数）
        BigDecimal totalSurplus = settings.stream()
                .map(s -> s.getRemainingAmount() != null ? s.getRemainingAmount() : BigDecimal.ZERO)
                .filter(v -> v.compareTo(BigDecimal.ZERO) > 0)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        if (totalSurplus.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BizException(ErrorCode.BIZ_RULE_NOT_MET, "本月无结余可转存");
        }

        // 查找或创建心愿储蓄
        BizSavingGoal goal;
        if (dto.getGoalId() != null) {
            goal = savingGoalMapper.selectById(dto.getGoalId());
            if (goal == null || !goal.getUserId().equals(userId)) {
                throw new BizException(ErrorCode.BIZ_RULE_NOT_MET, "心愿储蓄不存在");
            }
        } else {
            goal = new BizSavingGoal();
            goal.setUserId(userId);
            goal.setGoalName(dto.getGoalName() != null ? dto.getGoalName() : "预算结余储蓄");
            goal.setTargetAmount(dto.getTargetAmount() != null ? dto.getTargetAmount() : totalSurplus);
            goal.setCurrentAmount(BigDecimal.ZERO);
            goal.setStatus("ACTIVE");
            savingGoalMapper.insert(goal);
        }

        BigDecimal newCurrent = goal.getCurrentAmount().add(totalSurplus);
        goal.setCurrentAmount(newCurrent);
        goal.setProgressPercent(newCurrent.multiply(new BigDecimal("100"))
                .divide(goal.getTargetAmount(), 2, RoundingMode.HALF_UP));
        savingGoalMapper.updateById(goal);

        // 结余清零（模拟转存后预算已用尽）
        for (BizBudgetSetting s : settings) {
            if (s.getRemainingAmount() != null && s.getRemainingAmount().compareTo(BigDecimal.ZERO) > 0) {
                s.setUsedAmount(s.getBudgetAmount());
                s.setRemainingAmount(BigDecimal.ZERO);
                s.setUsagePercent(new BigDecimal("100.00"));
                settingMapper.updateById(s);
            }
        }

        log.info("[结余转储蓄] 用户={}, 转存金额={}, 目标={}", userId, totalSurplus, goal.getGoalName());

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("goalId", goal.getId());
        result.put("goalName", goal.getGoalName());
        result.put("transferAmount", totalSurplus);
        result.put("currentAmount", goal.getCurrentAmount());
        result.put("progressPercent", goal.getProgressPercent());
        result.put("message", "结余" + totalSurplus + "元已转入心愿储蓄（模拟，不涉及真实资金划转）");
        return result;
    }

    // ============================================================
    //  辅助：分类列表 / 储蓄列表 / 概览
    // ============================================================

    public List<BizBudgetCategory> listCategories() {
        return categoryMapper.selectList(
                new LambdaQueryWrapper<BizBudgetCategory>()
                        .eq(BizBudgetCategory::getStatus, 1)
                        .orderByAsc(BizBudgetCategory::getSortOrder));
    }

    public List<BizSavingGoal> listSavings(Long userId) {
        return savingGoalMapper.selectList(
                new LambdaQueryWrapper<BizSavingGoal>()
                        .eq(BizSavingGoal::getUserId, userId)
                        .orderByDesc(BizSavingGoal::getCreateTime));
    }

    public Map<String, Object> getOverview(Long userId) {
        String period = currentPeriod();
        List<BizBudgetSetting> settings = settingMapper.selectList(
                new LambdaQueryWrapper<BizBudgetSetting>()
                        .eq(BizBudgetSetting::getUserId, userId)
                        .eq(BizBudgetSetting::getBudgetPeriod, period));
        BigDecimal totalBudget = settings.stream()
                .map(BizBudgetSetting::getBudgetAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalUsed = settings.stream()
                .map(s -> s.getUsedAmount() != null ? s.getUsedAmount() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalRemain = totalBudget.subtract(totalUsed);
        BigDecimal remainRatio = totalBudget.compareTo(BigDecimal.ZERO) > 0
                ? totalRemain.multiply(new BigDecimal("100")).divide(totalBudget, 2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        // 整体最高提醒级别
        int maxLevel = settings.stream()
                .mapToInt(s -> calcRemindLevel(s.getUsagePercent(), s))
                .max().orElse(0);

        Map<String, Object> map = new LinkedHashMap<>();
        map.put("period", period);
        map.put("totalBudget", totalBudget);
        map.put("totalUsed", totalUsed);
        map.put("totalRemain", totalRemain);
        map.put("remainRatio", remainRatio);
        map.put("remindLevel", maxLevel);
        map.put("remindName", remindName(maxLevel));
        return map;
    }

    // ============================================================
    //  内部工具
    // ============================================================

    private String mapMccToCategory(String mcc) {
        if (mcc == null) return "OTHER";
        return MCC_TO_CATEGORY.getOrDefault(mcc, "OTHER");
    }

    private String currentPeriod() {
        return LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM"));
    }

    private BigDecimal calcUsagePercent(BigDecimal used, BigDecimal budget) {
        if (budget == null || budget.compareTo(BigDecimal.ZERO) == 0) return BigDecimal.ZERO;
        return used.multiply(new BigDecimal("100")).divide(budget, 2, RoundingMode.HALF_UP);
    }

    /**
     * 三级提醒：usage≥100% 超支(3)；usage≥80%(剩余≤20%) 紧张(2)；usage≥50%(剩余≤50%) 温和(1)；否则 0
     */
    private int calcRemindLevel(BigDecimal percent, BizBudgetSetting setting) {
        if (percent == null) return 0;
        if (percent.compareTo(new BigDecimal("100")) >= 0) return 3;
        if (percent.compareTo(new BigDecimal("80")) >= 0) return 2;
        if (percent.compareTo(new BigDecimal("50")) >= 0) return 1;
        return 0;
    }

    private String remindName(int level) {
        return switch (level) {
            case 1 -> "温和提醒";
            case 2 -> "紧张提醒";
            case 3 -> "超支预警";
            default -> "正常";
        };
    }

    private String buildRemindMessage(int level, String categoryName, BigDecimal remaining, BigDecimal percent) {
        return switch (level) {
            case 1 -> "【温和提醒】" + categoryName + "预算已使用" + percent + "%，剩余" + remaining + "元，请合理消费。";
            case 2 -> "【紧张提醒】" + categoryName + "预算已使用" + percent + "%，剩余" + remaining + "元，建议控制支出。";
            case 3 -> "【超支预警】" + categoryName + "预算已超支" + remaining.abs() + "元，请及时调整消费计划！";
            default -> null;
        };
    }

    private void markRemindSent(BizBudgetSetting setting, int level) {
        if (level >= 1 && (setting.getRemind50Sent() == null || setting.getRemind50Sent() == 0)) {
            setting.setRemind50Sent(1);
        }
        if (level >= 2 && (setting.getRemind20Sent() == null || setting.getRemind20Sent() == 0)) {
            setting.setRemind20Sent(1);
        }
        if (level >= 3 && (setting.getRemindOverSent() == null || setting.getRemindOverSent() == 0)) {
            setting.setRemindOverSent(1);
        }
    }

    /**
     * 判断当前提醒级别是否需要发送（每档仅首次触发）
     */
    private boolean shouldSendRemind(BizBudgetSetting setting, int level) {
        return switch (level) {
            case 1 -> setting.getRemind50Sent() == null || setting.getRemind50Sent() == 0;
            case 2 -> setting.getRemind20Sent() == null || setting.getRemind20Sent() == 0;
            case 3 -> setting.getRemindOverSent() == null || setting.getRemindOverSent() == 0;
            default -> false;
        };
    }

    /**
     * 发送预算提醒站内信
     */
    private void sendBudgetMessage(Long userId, int level, String message, Long settingId) {
        String title = switch (level) {
            case 1 -> "预算温和提醒";
            case 2 -> "预算紧张提醒";
            case 3 -> "预算超支预警";
            default -> "预算提醒";
        };
        SysMessage msg = new SysMessage();
        msg.setUserId(userId);
        msg.setTitle(title);
        msg.setContent(message + " 【模拟】");
        msg.setType("BUDGET");
        msg.setBizType("BUDGET");
        msg.setBizId(settingId);
        msg.setIsRead(0);
        messageMapper.insert(msg);
    }

    /**
     * 超支写入 biz_risk_warning（warning_type=BUDGET_OVER）
     */
    private void saveBudgetRiskWarning(Long userId, String categoryName,
                                        BigDecimal remaining, BigDecimal percent, Long settingId) {
        BizRiskWarning warning = new BizRiskWarning();
        warning.setUserId(userId);
        warning.setWarningType("BUDGET_OVER");
        warning.setWarningLevel("HIGH");
        warning.setWarningTitle("预算超支预警 - " + categoryName);
        warning.setWarningContent(categoryName + "预算已超支" + remaining.abs() + "元（使用率" + percent + "%），请及时调整消费计划。【模拟】");
        warning.setRelatedModule("BUDGET");
        warning.setRelatedId(settingId);
        warning.setIsRead(0);
        warning.setIsHandled(0);
        warning.setWarningTime(LocalDateTime.now());
        riskWarningMapper.insert(warning);
    }

    private String generateNo(String prefix) {
        return prefix + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS"));
    }
}
