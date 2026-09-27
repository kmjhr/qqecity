package com.icbc.qingqi.module.operation.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.icbc.qingqi.module.bookkeeping.entity.BizBookkeepingRecord;
import com.icbc.qingqi.module.bookkeeping.mapper.BizBookkeepingRecordMapper;
import com.icbc.qingqi.module.message.entity.SysMessage;
import com.icbc.qingqi.module.message.mapper.SysMessageMapper;
import com.icbc.qingqi.module.operation.dto.CashflowWarningVO;
import com.icbc.qingqi.module.risk.entity.BizRiskWarning;
import com.icbc.qingqi.module.risk.mapper.BizRiskWarningMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 现金流风险预警服务
 * <p>
 * 缺口 #12 现金流风险预警
 * 规则：近3月结余率<10% 且 存在>7天应收未收（INCOME + isConfirmed=0 + happenDate 早于今日>7天）
 * 命中落 biz_risk_warning（warning_type=CASHFLOW_WARNING, level=HIGH），同时发站内信
 * 文案给备货/淡旺季建议
 */
@Slf4j
@Service
public class CashflowWarningService {

    /** 结余率阈值（%）— 低于此值视为现金紧张 */
    private static final BigDecimal SURPLUS_RATE_THRESHOLD = new BigDecimal("10");

    /** 应收未收天数阈值 — 超过此值视为应收未收 */
    private static final long PENDING_DAYS_THRESHOLD = 7L;

    /** 历史预警去重窗口（天）— 同一用户7天内不重复触发 */
    private static final int DEDUP_WINDOW_DAYS = 7;

    private final BizBookkeepingRecordMapper recordMapper;
    private final BizRiskWarningMapper riskWarningMapper;
    private final SysMessageMapper messageMapper;

    public CashflowWarningService(BizBookkeepingRecordMapper recordMapper,
                                  BizRiskWarningMapper riskWarningMapper,
                                  SysMessageMapper messageMapper) {
        this.recordMapper = recordMapper;
        this.riskWarningMapper = riskWarningMapper;
        this.messageMapper = messageMapper;
    }

    /**
     * 检测当前用户现金流风险，命中则落库预警 + 发站内信
     *
     * @param userId 用户ID
     * @return 检测结果（含命中原因、建议文案、预警ID）
     */
    @Transactional(rollbackFor = Exception.class)
    public CashflowWarningVO detect(Long userId) {
        LocalDate today = LocalDate.now();
        LocalDate periodStart = today.minusMonths(3).withDayOfMonth(1);
        LocalDate periodEnd = today.minusDays(1);

        // 1. 聚合近3月记账数据
        List<BizBookkeepingRecord> recentRecords = recordMapper.selectList(
                new LambdaQueryWrapper<BizBookkeepingRecord>()
                        .eq(BizBookkeepingRecord::getUserId, userId)
                        .between(BizBookkeepingRecord::getHappenDate, periodStart, periodEnd));

        BigDecimal totalIncome = recentRecords.stream()
                .filter(r -> "INCOME".equals(r.getRecordType()))
                .map(BizBookkeepingRecord::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalExpense = recentRecords.stream()
                .filter(r -> "EXPENSE".equals(r.getRecordType()))
                .map(BizBookkeepingRecord::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal netCashFlow = totalIncome.subtract(totalExpense);
        BigDecimal surplusRate = totalIncome.compareTo(BigDecimal.ZERO) > 0
                ? netCashFlow.multiply(new BigDecimal("100")).divide(totalIncome, 2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        // 2. 识别应收未收（INCOME + isConfirmed=0 + happenDate 早于今日>7天）
        List<BizBookkeepingRecord> pendingReceivables = recentRecords.stream()
                .filter(r -> "INCOME".equals(r.getRecordType()))
                .filter(r -> r.getIsConfirmed() != null && r.getIsConfirmed() == 0)
                .filter(r -> r.getHappenDate() != null
                        && ChronoUnit.DAYS.between(r.getHappenDate(), today) > PENDING_DAYS_THRESHOLD)
                .collect(Collectors.toList());

        int pendingCount = pendingReceivables.size();
        int maxPendingDays = pendingReceivables.stream()
                .mapToInt(r -> (int) ChronoUnit.DAYS.between(r.getHappenDate(), today))
                .max()
                .orElse(0);

        // 3. 判定是否命中预警
        List<String> reasons = new ArrayList<>();
        boolean lowSurplus = surplusRate.compareTo(SURPLUS_RATE_THRESHOLD) < 0;
        boolean hasPending = pendingCount > 0;

        CashflowWarningVO vo = new CashflowWarningVO();
        vo.setUserId(userId);
        vo.setPeriodStart(periodStart);
        vo.setPeriodEnd(periodEnd);
        vo.setTotalIncome(totalIncome);
        vo.setTotalExpense(totalExpense);
        vo.setNetCashFlow(netCashFlow);
        vo.setSurplusRate(surplusRate);
        vo.setPendingReceivableCount(pendingCount);
        vo.setMaxPendingDays(maxPendingDays);

        boolean triggered = lowSurplus && hasPending;
        vo.setWarningTriggered(triggered);

        if (triggered) {
            reasons.add("近3月结余率仅 " + surplusRate + "%，低于健康阈值 10%");
            reasons.add("存在 " + pendingCount + " 笔应收未收（INCOME 待确认 > 7 天），最长逾期 "
                    + maxPendingDays + " 天");
            vo.setTriggerReasons(reasons);
            vo.setAdvice(buildAdvice(surplusRate, pendingCount, maxPendingDays));

            // 4. 历史去重：7天内已触发过则不重复落库
            LocalDateTime dedupSince = LocalDateTime.now().minusDays(DEDUP_WINDOW_DAYS);
            Long existingCount = riskWarningMapper.selectCount(
                    new LambdaQueryWrapper<BizRiskWarning>()
                            .eq(BizRiskWarning::getUserId, userId)
                            .eq(BizRiskWarning::getWarningType, "CASHFLOW_WARNING")
                            .ge(BizRiskWarning::getWarningTime, dedupSince));
            if (existingCount != null && existingCount > 0) {
                log.info("[现金流预警] 用户={} 近{}天已触发过，跳过落库", userId, DEDUP_WINDOW_DAYS);
                return vo;
            }

            // 5. 落 biz_risk_warning
            BizRiskWarning warning = new BizRiskWarning();
            warning.setUserId(userId);
            warning.setWarningType("CASHFLOW_WARNING");
            warning.setWarningLevel("HIGH");
            warning.setWarningTitle("现金流风险预警：账面盈利但现金紧张");
            warning.setWarningContent(buildWarningContent(surplusRate, pendingCount,
                    maxPendingDays, totalIncome, totalExpense, netCashFlow));
            warning.setRelatedModule("BOOKKEEPING");
            warning.setIsRead(0);
            warning.setIsHandled(0);
            warning.setWarningTime(LocalDateTime.now());
            riskWarningMapper.insert(warning);
            vo.setWarningId(warning.getId());

            // 6. 发站内信
            SysMessage msg = new SysMessage();
            msg.setUserId(userId);
            msg.setTitle("现金流风险预警");
            msg.setContent("您近3月结余率仅 " + surplusRate + "%，且存在 " + pendingCount
                    + " 笔应收未收（最长 " + maxPendingDays + " 天），现金流转紧张。\n"
                    + vo.getAdvice());
            msg.setType("SAFETY");
            msg.setBizType("CASHFLOW_WARNING");
            msg.setBizId(warning.getId());
            msg.setIsRead(0);
            messageMapper.insert(msg);

            log.info("[现金流预警] 用户={} 命中预警，warningId={}, 结余率={}%, 应收未收={}笔",
                    userId, warning.getId(), surplusRate, pendingCount);
        } else {
            vo.setTriggerReasons(List.of());
            vo.setAdvice("现金流健康，无需预警。");
        }

        return vo;
    }

    /**
     * 查询当前用户的历史现金流预警记录
     */
    public List<BizRiskWarning> listWarnings(Long userId) {
        return riskWarningMapper.selectList(
                new LambdaQueryWrapper<BizRiskWarning>()
                        .eq(BizRiskWarning::getUserId, userId)
                        .eq(BizRiskWarning::getWarningType, "CASHFLOW_WARNING")
                        .orderByDesc(BizRiskWarning::getWarningTime));
    }

    // ============================================================
    //  文案构建
    // ============================================================

    private String buildWarningContent(BigDecimal surplusRate, int pendingCount,
                                       int maxPendingDays, BigDecimal income,
                                       BigDecimal expense, BigDecimal netCashFlow) {
        StringBuilder sb = new StringBuilder();
        sb.append("【现金流风险预警】\n");
        sb.append("近3月总收入：").append(income).append("元\n");
        sb.append("近3月总支出：").append(expense).append("元\n");
        sb.append("净现金流：").append(netCashFlow).append("元\n");
        sb.append("结余率：").append(surplusRate).append("%（低于健康阈值10%）\n");
        sb.append("应收未收：").append(pendingCount).append("笔，最长逾期").append(maxPendingDays).append("天\n\n");
        sb.append(buildAdvice(surplusRate, pendingCount, maxPendingDays));
        return sb.toString();
    }

    /**
     * 备货/淡旺季建议文案
     */
    private String buildAdvice(BigDecimal surplusRate, int pendingCount, int maxPendingDays) {
        StringBuilder sb = new StringBuilder();
        sb.append("【经营建议】\n");
        sb.append("1. 应收账款催收：优先跟进").append(pendingCount).append("笔逾期应收，")
                .append("最长逾期").append(maxPendingDays).append("天，建议建立催收台账。\n");
        sb.append("2. 备货节奏调整：当前结余率仅").append(surplusRate).append("%，")
                .append("建议压缩非必要备货，优先保障经营性现金流。\n");
        sb.append("3. 淡旺季预案：根据近3月收支趋势识别淡旺季，")
                .append("旺季前预留至少1个月固定支出作为风险准备金，")
                .append("淡季可采用小批量多频次采购降低资金占用。\n");
        sb.append("4. 应急融资：可结合青创e贷A类循环贷（年化3.85%）平滑现金流，")
                .append("详情见「青创e贷」模块。\n");
        sb.append("（以上为模拟建议，不构成投资/融资邀约）");
        return sb.toString();
    }
}
