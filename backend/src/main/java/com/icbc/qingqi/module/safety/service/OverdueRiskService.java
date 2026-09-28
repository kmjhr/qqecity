package com.icbc.qingqi.module.safety.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.icbc.qingqi.module.bookkeeping.entity.BizBookkeepingRecord;
import com.icbc.qingqi.module.bookkeeping.mapper.BizBookkeepingRecordMapper;
import com.icbc.qingqi.module.guarantee.entity.BizGuaranteeApplication;
import com.icbc.qingqi.module.guarantee.mapper.BizGuaranteeApplicationMapper;
import com.icbc.qingqi.module.loan.entity.BizCreditLimit;
import com.icbc.qingqi.module.loan.entity.BizCreditTxn;
import com.icbc.qingqi.module.loan.entity.BizEntrustPayment;
import com.icbc.qingqi.module.loan.mapper.BizCreditLimitMapper;
import com.icbc.qingqi.module.loan.mapper.BizCreditTxnMapper;
import com.icbc.qingqi.module.loan.mapper.BizEntrustPaymentMapper;
import com.icbc.qingqi.module.message.entity.SysMessage;
import com.icbc.qingqi.module.message.mapper.SysMessageMapper;
import com.icbc.qingqi.module.risk.entity.BizRiskWarning;
import com.icbc.qingqi.module.risk.mapper.BizRiskWarningMapper;
import com.icbc.qingqi.module.safety.dto.OverdueRiskVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 逾期风险预判服务（模拟）
 * <p>
 * 缺口 #15 逾期风险预判
 * 规则（演示口径）：
 *  - 聚合未来 N 天还款日历：A 类循环贷（提款计息）+ 受托支付（应付未付）+ 保函费（待缴）
 *  - 收支数据规则评分：
 *    1. 距到期日 ≤ aheadDays 且账户余额 < 应还总额 → HIGH
 *    2. 距到期日 ≤ aheadDays 且近 30 天净现金流 < 0 → MEDIUM
 *    3. 多笔（≥2）到期日临近且余额仅能覆盖 60% 以下 → CRITICAL
 *  - 命中落 biz_risk_warning（warning_type=OVERDUE_RISK）+ 站内信
 *  - 7 天内同用户不重复触发
 *  - 全程标注"模拟"，不接入真实征信/账务系统
 */
@Slf4j
@Service
public class OverdueRiskService {

    /** 默认预测窗口（天） */
    private static final int DEFAULT_AHEAD_DAYS = 7;

    /** 历史预警去重窗口（天） */
    private static final int DEDUP_WINDOW_DAYS = 7;

    /** A 类循环贷年化（与 LoanService 一致，3.85%） */
    private static final BigDecimal A_LOAN_ANNUAL_RATE = new BigDecimal("0.0385");

    private final BizCreditLimitMapper creditLimitMapper;
    private final BizCreditTxnMapper creditTxnMapper;
    private final BizEntrustPaymentMapper entrustPaymentMapper;
    private final BizGuaranteeApplicationMapper guaranteeAppMapper;
    private final BizBookkeepingRecordMapper bookkeepingMapper;
    private final BizRiskWarningMapper riskWarningMapper;
    private final SysMessageMapper messageMapper;

    public OverdueRiskService(BizCreditLimitMapper creditLimitMapper,
                              BizCreditTxnMapper creditTxnMapper,
                              BizEntrustPaymentMapper entrustPaymentMapper,
                              BizGuaranteeApplicationMapper guaranteeAppMapper,
                              BizBookkeepingRecordMapper bookkeepingMapper,
                              BizRiskWarningMapper riskWarningMapper,
                              SysMessageMapper messageMapper) {
        this.creditLimitMapper = creditLimitMapper;
        this.creditTxnMapper = creditTxnMapper;
        this.entrustPaymentMapper = entrustPaymentMapper;
        this.guaranteeAppMapper = guaranteeAppMapper;
        this.bookkeepingMapper = bookkeepingMapper;
        this.riskWarningMapper = riskWarningMapper;
        this.messageMapper = messageMapper;
    }

    /**
     * 获取还款日历（未来 N 天）
     */
    public OverdueRiskVO getCalendar(Long userId, Integer days) {
        int ahead = days == null || days <= 0 ? DEFAULT_AHEAD_DAYS : days;
        OverdueRiskVO vo = new OverdueRiskVO();
        vo.setUserId(userId);
        vo.setAheadDays(ahead);
        vo.setCalendar(buildCalendar(userId, ahead));
        fillProjection(vo, userId);
        vo.setWarningTriggered(false);
        vo.setTriggerReasons(List.of());
        vo.setAdvice("当前无逾期风险预警（模拟）。");
        return vo;
    }

    /**
     * 执行逾期风险预判，命中则落库 OVERDUE_RISK + 站内信
     */
    @Transactional(rollbackFor = Exception.class)
    public OverdueRiskVO predict(Long userId, Integer aheadDays) {
        int ahead = aheadDays == null || aheadDays <= 0 ? DEFAULT_AHEAD_DAYS : aheadDays;
        LocalDate today = LocalDate.now();
        LocalDate dueWindowEnd = today.plusDays(ahead);

        // 1. 拉取还款日历
        List<OverdueRiskVO.RepaymentItem> calendar = buildCalendar(userId, ahead);
        BigDecimal totalDue = calendar.stream()
                .map(OverdueRiskVO.RepaymentItem::getTotalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // 2. 账户余额（模拟）：近 30 天 INCOME - EXPENSE
        LocalDate since30 = today.minusDays(30);
        List<BizBookkeepingRecord> recentRecords = bookkeepingMapper.selectList(
                new LambdaQueryWrapper<BizBookkeepingRecord>()
                        .eq(BizBookkeepingRecord::getUserId, userId)
                        .ge(BizBookkeepingRecord::getHappenDate, since30));
        BigDecimal income30 = recentRecords.stream()
                .filter(r -> "INCOME".equals(r.getRecordType()))
                .map(BizBookkeepingRecord::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal expense30 = recentRecords.stream()
                .filter(r -> "EXPENSE".equals(r.getRecordType()))
                .map(BizBookkeepingRecord::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal netCashFlow30 = income30.subtract(expense30);
        // 模拟账户余额：净现金流 + 一个保底 5000 元（演示用，避免空数据）
        BigDecimal accountBalance = netCashFlow30.add(new BigDecimal("5000"));

        // 3. 未来 N 天预计净现金流（模拟）：取近 30 天均值 * ahead/30
        BigDecimal projectedNetCashFlow = BigDecimal.ZERO;
        if (income30.compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal dailyAvg = netCashFlow30.divide(new BigDecimal("30"), 2, RoundingMode.HALF_UP);
            projectedNetCashFlow = dailyAvg.multiply(new BigDecimal(ahead));
        }

        // 4. 规则评分
        List<String> reasons = new ArrayList<>();
        String level = null;
        int riskScore = 0;

        // 规则1：余额 < 应还总额
        if (totalDue.compareTo(BigDecimal.ZERO) > 0
                && accountBalance.compareTo(totalDue) < 0) {
            reasons.add("未来 " + ahead + " 天应还总额 ¥" + totalDue
                    + "，账户余额仅 ¥" + accountBalance + "（不足覆盖）");
            level = "HIGH";
            riskScore += 50;
        }

        // 规则2：近 30 天净现金流 < 0
        if (netCashFlow30.compareTo(BigDecimal.ZERO) < 0) {
            reasons.add("近 30 天净现金流为 -¥" + netCashFlow30.abs()
                    + "（支出大于收入），还款资金压力较大");
            if (level == null) level = "MEDIUM";
            riskScore += 25;
        }

        // 规则3：多笔到期日临近且余额覆盖率 < 60%
        if (calendar.size() >= 2 && totalDue.compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal coverage = accountBalance.divide(totalCost(totalDue), 4, RoundingMode.HALF_UP);
            if (coverage.compareTo(new BigDecimal("0.60")) < 0) {
                reasons.add("未来 " + ahead + " 天有 " + calendar.size()
                        + " 笔还款到期，账户余额覆盖率仅 "
                        + coverage.multiply(new BigDecimal("100")).setScale(0, RoundingMode.HALF_UP)
                        + "%（<60%）");
                level = "CRITICAL";
                riskScore += 30;
            }
        }

        // 规则4：受托支付应付未付且距到期 ≤ 3 天
        long urgentPayments = calendar.stream()
                .filter(i -> "ENTRUST_PAYMENT".equals(i.getType()))
                .filter(i -> i.getDaysToDue() != null && i.getDaysToDue() <= 3)
                .count();
        if (urgentPayments > 0) {
            reasons.add("有 " + urgentPayments + " 笔受托支付在 3 天内到期，需立即备款");
            if (level == null) level = "MEDIUM";
            riskScore += 10;
        }

        OverdueRiskVO vo = new OverdueRiskVO();
        vo.setUserId(userId);
        vo.setAheadDays(ahead);
        vo.setCalendar(calendar);
        vo.setAccountBalance(accountBalance);
        vo.setProjectedNetCashFlow(projectedNetCashFlow);
        vo.setRiskScore(Math.min(100, riskScore));

        if (level != null) {
            vo.setWarningTriggered(true);
            vo.setTriggerReasons(reasons);
            vo.setWarningLevel(level);
            vo.setAdvice(buildAdvice(level, totalDue, accountBalance, calendar.size()));

            // 7 天去重
            LocalDateTime dedupSince = LocalDateTime.now().minusDays(DEDUP_WINDOW_DAYS);
            Long existing = riskWarningMapper.selectCount(
                    new LambdaQueryWrapper<BizRiskWarning>()
                            .eq(BizRiskWarning::getUserId, userId)
                            .eq(BizRiskWarning::getWarningType, "OVERDUE_RISK")
                            .ge(BizRiskWarning::getWarningTime, dedupSince));
            if (existing != null && existing > 0) {
                log.info("[逾期预判] 用户={} 近{}天已触发 OVERDUE_RISK，跳过落库", userId, DEDUP_WINDOW_DAYS);
                return vo;
            }

            BizRiskWarning warning = new BizRiskWarning();
            warning.setUserId(userId);
            warning.setWarningType("OVERDUE_RISK");
            warning.setWarningLevel(level);
            warning.setWarningTitle("逾期风险预警");
            warning.setWarningContent(buildWarningContent(reasons, totalDue, accountBalance,
                    netCashFlow30, calendar.size(), ahead));
            warning.setRelatedModule("LOAN");
            warning.setIsRead(0);
            warning.setIsHandled(0);
            warning.setWarningTime(LocalDateTime.now());
            riskWarningMapper.insert(warning);
            vo.setWarningId(warning.getId());

            SysMessage msg = new SysMessage();
            msg.setUserId(userId);
            msg.setTitle("逾期风险预警（" + level + "）");
            msg.setContent("未来 " + ahead + " 天有 " + calendar.size()
                    + " 笔还款到期，应还 ¥" + totalDue + "，账户余额 ¥" + accountBalance
                    + "。" + vo.getAdvice());
            msg.setType("SAFETY");
            msg.setBizType("OVERDUE_RISK");
            msg.setBizId(warning.getId());
            msg.setIsRead(0);
            messageMapper.insert(msg);

            log.info("[逾期预判] 用户={}, 等级={}, warningId={}, 应还={}, 余额={}",
                    userId, level, warning.getId(), totalDue, accountBalance);
        } else {
            vo.setWarningTriggered(false);
            vo.setTriggerReasons(List.of());
            vo.setAdvice("未来 " + ahead + " 天无到期还款或资金充足，暂无逾期风险（模拟）。");
        }

        return vo;
    }

    /**
     * 还款后自动复检（模拟）：还本付息完成后触发一次逾期预判；
     * 若复检无风险，则将当前用户未处理的 OVERDUE_RISK 预警自动置为已处理（动态闭环演示）。
     *
     * @return 复检结果（predict 返回）
     */
    @Transactional(rollbackFor = Exception.class)
    public OverdueRiskVO refreshAfterRepay(Long userId) {
        OverdueRiskVO vo = predict(userId, DEFAULT_AHEAD_DAYS);
        boolean triggered = vo.getWarningTriggered() != null && vo.getWarningTriggered();
        if (!triggered) {
            LocalDateTime now = LocalDateTime.now();
            List<BizRiskWarning> unhandled = riskWarningMapper.selectList(
                    new LambdaQueryWrapper<BizRiskWarning>()
                            .eq(BizRiskWarning::getUserId, userId)
                            .eq(BizRiskWarning::getWarningType, "OVERDUE_RISK")
                            .eq(BizRiskWarning::getIsHandled, 0));
            for (BizRiskWarning w : unhandled) {
                w.setIsHandled(1);
                w.setHandleNote("还款后自动复检通过，风险解除（模拟）");
                w.setHandleTime(now);
                riskWarningMapper.updateById(w);
            }
            log.info("[逾期复检] 用户={} 还款后复检无风险，自动处理 {} 条 OVERDUE_RISK 预警", userId, unhandled.size());
        }
        return vo;
    }

    public List<BizRiskWarning> listWarnings(Long userId) {
        return riskWarningMapper.selectList(
                new LambdaQueryWrapper<BizRiskWarning>()
                        .eq(BizRiskWarning::getUserId, userId)
                        .eq(BizRiskWarning::getWarningType, "OVERDUE_RISK")
                        .orderByDesc(BizRiskWarning::getWarningTime));
    }

    // ============================================================
    //  内部方法
    // ============================================================

    private List<OverdueRiskVO.RepaymentItem> buildCalendar(Long userId, int ahead) {
        LocalDate today = LocalDate.now();
        LocalDate end = today.plusDays(ahead);
        List<OverdueRiskVO.RepaymentItem> items = new ArrayList<>();

        // 1. A 类循环贷：基于未还提款流水，按"模拟还款日 = 提款日 + 30 天"构造还款日历
        List<BizCreditTxn> withdraws = creditTxnMapper.selectList(
                new LambdaQueryWrapper<BizCreditTxn>()
                        .eq(BizCreditTxn::getUserId, userId)
                        .eq(BizCreditTxn::getTxnType, "WITHDRAW"));
        for (BizCreditTxn w : withdraws) {
            LocalDate dueDate = w.getTxnTime().toLocalDate().plusDays(30);
            if (dueDate.isBefore(today) || dueDate.isAfter(end)) {
                continue;
            }
            long days = ChronoUnit.DAYS.between(today, dueDate);
            BigDecimal interest = w.getPrincipalAmount()
                    .multiply(A_LOAN_ANNUAL_RATE)
                    .multiply(new BigDecimal("30"))
                    .divide(new BigDecimal("365"), 2, RoundingMode.HALF_UP);
            OverdueRiskVO.RepaymentItem item = new OverdueRiskVO.RepaymentItem();
            item.setType("LOAN");
            item.setBizId(w.getId());
            item.setBizNo(w.getTxnNo());
            item.setPrincipal(w.getPrincipalAmount());
            item.setInterest(interest);
            item.setTotalAmount(w.getPrincipalAmount().add(interest));
            item.setDueDate(dueDate);
            item.setDaysToDue((int) days);
            item.setRemark("A 类循环贷提款 ¥" + w.getPrincipalAmount() + "（年化 3.85%，按日计息，模拟30天到期）");
            items.add(item);
        }

        // 2. 受托支付：未成功的应付（PENDING / PROCESSING / FAILED）→ 以创建日 + 7 天为模拟到期
        List<BizEntrustPayment> payments = entrustPaymentMapper.selectList(
                new LambdaQueryWrapper<BizEntrustPayment>()
                        .eq(BizEntrustPayment::getUserId, userId)
                        .in(BizEntrustPayment::getPaymentStatus, "PENDING", "PROCESSING", "FAILED"));
        for (BizEntrustPayment p : payments) {
            LocalDate dueDate = p.getCreateTime().toLocalDate().plusDays(7);
            if (dueDate.isBefore(today) || dueDate.isAfter(end)) {
                continue;
            }
            long days = ChronoUnit.DAYS.between(today, dueDate);
            OverdueRiskVO.RepaymentItem item = new OverdueRiskVO.RepaymentItem();
            item.setType("ENTRUST_PAYMENT");
            item.setBizId(p.getId());
            item.setBizNo(p.getPaymentNo());
            item.setPrincipal(p.getAmount());
            item.setInterest(BigDecimal.ZERO);
            item.setTotalAmount(p.getAmount());
            item.setDueDate(dueDate);
            item.setDaysToDue((int) days);
            item.setRemark("受托支付应付 " + p.getMerchantName() + "（模拟7天到期，状态：" + p.getPaymentStatus() + "）");
            items.add(item);
        }

        // 3. 保函费：申请待缴费（PENDING_PAY）→ 模拟到期 = 提交日 + 3 天
        List<BizGuaranteeApplication> pendingApps = guaranteeAppMapper.selectList(
                new LambdaQueryWrapper<BizGuaranteeApplication>()
                        .eq(BizGuaranteeApplication::getTenantId, userId)
                        .eq(BizGuaranteeApplication::getApplyStatus, "PENDING_PAY"));
        for (BizGuaranteeApplication a : pendingApps) {
            LocalDate dueDate = a.getSubmitTime().toLocalDate().plusDays(3);
            if (dueDate.isBefore(today) || dueDate.isAfter(end)) {
                continue;
            }
            long days = ChronoUnit.DAYS.between(today, dueDate);
            OverdueRiskVO.RepaymentItem item = new OverdueRiskVO.RepaymentItem();
            item.setType("GUARANTEE_FEE");
            item.setBizId(a.getId());
            item.setBizNo(a.getApplyNo());
            item.setPrincipal(a.getGuaranteeFee());
            item.setInterest(BigDecimal.ZERO);
            item.setTotalAmount(a.getGuaranteeFee());
            item.setDueDate(dueDate);
            item.setDaysToDue((int) days);
            item.setRemark("保函费 ¥" + a.getGuaranteeFee() + "（" + a.getApplyNo() + "，模拟3天到期）");
            items.add(item);
        }

        // 按到期日升序
        return items.stream()
                .sorted(Comparator.comparing(OverdueRiskVO.RepaymentItem::getDueDate))
                .collect(Collectors.toList());
    }

    private void fillProjection(OverdueRiskVO vo, Long userId) {
        LocalDate today = LocalDate.now();
        LocalDate since30 = today.minusDays(30);
        List<BizBookkeepingRecord> records = bookkeepingMapper.selectList(
                new LambdaQueryWrapper<BizBookkeepingRecord>()
                        .eq(BizBookkeepingRecord::getUserId, userId)
                        .ge(BizBookkeepingRecord::getHappenDate, since30));
        BigDecimal income = records.stream()
                .filter(r -> "INCOME".equals(r.getRecordType()))
                .map(BizBookkeepingRecord::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal expense = records.stream()
                .filter(r -> "EXPENSE".equals(r.getRecordType()))
                .map(BizBookkeepingRecord::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal net = income.subtract(expense);
        vo.setAccountBalance(net.add(new BigDecimal("5000")));
        if (income.compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal dailyAvg = net.divide(new BigDecimal("30"), 2, RoundingMode.HALF_UP);
            vo.setProjectedNetCashFlow(dailyAvg.multiply(new BigDecimal(vo.getAheadDays())));
        } else {
            vo.setProjectedNetCashFlow(BigDecimal.ZERO);
        }
    }

    /**
     * 用于 BigDecimal 除法 divisor 包装（避免精度问题）
     */
    private BigDecimal totalCost(BigDecimal divisor) {
        return divisor.compareTo(BigDecimal.ZERO) == 0 ? BigDecimal.ONE : divisor;
    }

    private String buildAdvice(String level, BigDecimal totalDue,
                                BigDecimal balance, int itemCount) {
        StringBuilder sb = new StringBuilder("【建议】\n");
        sb.append("1. 立即梳理未来到期清单（共 ").append(itemCount).append(" 笔，应还 ¥").append(totalDue).append("）；\n");
        sb.append("2. 优先安排高利率贷款还款，避免逾期上征信（模拟说明）；\n");
        sb.append("3. 可通过青启e城「心愿储蓄」预留应急资金，建议保留 1-2 个月支出的安全垫；\n");
        sb.append("4. 必要时可申请 A 类循环贷（年化 3.85%，随借随还）平滑短期资金压力，详见「青创e贷」；\n");
        sb.append("5. 如已出现逾期，请尽快结清并联系债权方出具结清证明。\n");
        sb.append("（以上为模拟建议，不构成投资/融资邀约）");
        return sb.toString();
    }

    private String buildWarningContent(List<String> reasons, BigDecimal totalDue,
                                       BigDecimal balance, BigDecimal netCashFlow30,
                                       int itemCount, int ahead) {
        StringBuilder sb = new StringBuilder("【逾期风险预判·模拟】\n");
        sb.append("未来 ").append(ahead).append(" 天到期笔数：").append(itemCount).append("\n");
        sb.append("应还总额：¥").append(totalDue).append("\n");
        sb.append("账户余额（模拟）：¥").append(balance).append("\n");
        sb.append("近 30 天净现金流：¥").append(netCashFlow30).append("\n");
        sb.append("触发原因：\n");
        for (String r : reasons) {
            sb.append("- ").append(r).append("\n");
        }
        return sb.toString();
    }
}
