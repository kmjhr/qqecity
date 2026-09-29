package com.icbc.qingqi.module.loan.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.icbc.qingqi.module.bookkeeping.entity.BizBookkeepingRecord;
import com.icbc.qingqi.module.bookkeeping.mapper.BizBookkeepingRecordMapper;
import com.icbc.qingqi.module.loan.entity.BizCreditLimit;
import com.icbc.qingqi.module.loan.entity.BizCreditTxn;
import com.icbc.qingqi.module.loan.entity.BizAiReviewLog;
import com.icbc.qingqi.module.loan.mapper.BizCreditLimitMapper;
import com.icbc.qingqi.module.loan.mapper.BizCreditTxnMapper;
import com.icbc.qingqi.module.profile.dto.ProfileVO;
import com.icbc.qingqi.module.profile.service.ProfileService;
import com.icbc.qingqi.module.risk.entity.BizRiskWarning;
import com.icbc.qingqi.module.risk.mapper.BizRiskWarningMapper;
import com.icbc.qingqi.module.loan.mapper.BizAiReviewLogMapper;
import com.icbc.qingqi.module.user.entity.SysUser;
import com.icbc.qingqi.module.user.mapper.SysUserMapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.icbc.qingqi.module.loan.entity.BizEntrustPayment;
import com.icbc.qingqi.module.loan.mapper.BizEntrustPaymentMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

/**
 * 借款前 AI 智能审查（模拟）
 * <p>
 * 演示口径（不查真实征信）：
 *  1. 画像审查：综合画像分 &lt; 40 → 收入/经营画像不足，暂不可借
 *  2. 收入客观性：近 30 天无收入记账或净现金流 &lt; 0 → 收入不客观，暂缓放款
 *  3. 多头借贷：A 类在途提款笔数 ≥ 5 → 已借多笔，请先还款
 *  4. 长期未还：存在在途超 60 天的提款 → 须结清后再借
 *  5. 逾期预警：存在未处理的 OVERDUE_RISK 预警 → 先处理风险
 * <p>
 * 命中任一规则即拒绝；全部通过放行。全程标注"模拟"。
 */
@Slf4j
@Service
public class LoanAiGuardService {

    /** 画像综合分门槛 */
    private static final int PROFILE_SCORE_MIN = 40;
    /** 在途提款笔数上限 */
    private static final int MAX_ONGOING_LOANS = 5;
    /** 长期未还天数 */
    private static final int LONG_OVERDUE_DAYS = 60;
    /** 收入统计窗口（天） */
    private static final int INCOME_WINDOW_DAYS = 30;

    private final BizCreditLimitMapper creditLimitMapper;
    private final BizCreditTxnMapper creditTxnMapper;
    private final BizEntrustPaymentMapper paymentMapper;
    private final BizBookkeepingRecordMapper bookkeepingMapper;
    private final BizRiskWarningMapper riskWarningMapper;
    private final ProfileService profileService;
    private final BizAiReviewLogMapper reviewLogMapper;
    private final SysUserMapper sysUserMapper;

    public LoanAiGuardService(BizCreditLimitMapper creditLimitMapper,
                              BizCreditTxnMapper creditTxnMapper,
                              BizEntrustPaymentMapper paymentMapper,
                              BizBookkeepingRecordMapper bookkeepingMapper,
                              BizRiskWarningMapper riskWarningMapper,
                              ProfileService profileService,
                              BizAiReviewLogMapper reviewLogMapper,
                              SysUserMapper sysUserMapper) {
        this.creditLimitMapper = creditLimitMapper;
        this.creditTxnMapper = creditTxnMapper;
        this.paymentMapper = paymentMapper;
        this.bookkeepingMapper = bookkeepingMapper;
        this.riskWarningMapper = riskWarningMapper;
        this.profileService = profileService;
        this.reviewLogMapper = reviewLogMapper;
        this.sysUserMapper = sysUserMapper;
    }

    /**
     * 借款前 AI 审查（A 类提款 / B 类打款共用）
     *
     * @param creditType A_TYPE=循环贷提款（须 B 转 A 成功）；B_TYPE=定向贷打款（走 B 类自身流程）
     */
    public AiGuardVO guard(Long userId, String creditType) {
        AiGuardVO vo = new AiGuardVO();
        vo.setUserId(userId);
        vo.setReviewNo("AIG" + System.currentTimeMillis());
        List<String> passedItems = new ArrayList<>();
        List<String> rejectedItems = new ArrayList<>();
        int score = 100;

        // 0. A/B 贷款与 A/B 用户一一对应：
        //    A 类循环贷 → 仅限已完成 B 转 A 的 A 类用户（PROMOTED）；
        //    B 类定向贷 → 仅限 B 类用户（未转 A），已转 A 用户不再开放 B 类定向贷
        BizCreditLimit bLimit = creditLimitMapper.selectOne(
                new LambdaQueryWrapper<BizCreditLimit>()
                        .eq(BizCreditLimit::getUserId, userId)
                        .eq(BizCreditLimit::getCreditType, "B_TYPE")
                        .last("LIMIT 1"));
        boolean bPromoted = bLimit != null && "PROMOTED".equals(bLimit.getObservationStatus());
        if ("A_TYPE".equals(creditType)) {
            if (!bPromoted) {
                rejectedItems.add("尚未完成 B 转 A（B 类定向贷观察期达标并转 A 成功后方可借 A 类循环贷）");
                score -= 40;
            } else {
                passedItems.add("已完成 B 转 A（B 类定向贷观察期达标转 A），具备 A 类循环贷准入资格");
            }
        } else if ("B_TYPE".equals(creditType)) {
            if (bPromoted) {
                rejectedItems.add("您已是 A 类用户（已完成 B 转 A），定向贷仅限 B 类用户，请使用 A 类循环贷");
                score -= 40;
            } else {
                passedItems.add("B 类用户身份校验通过（定向贷仅限 B 类用户使用）");
            }
        }

        // 1. 画像审查：综合画像分不足 → 收入/经营画像弱
        ProfileVO profile = profileService.getProfile(userId);
        int overall = profile.getOverallScore() == null ? 0 : profile.getOverallScore();
        if (overall < PROFILE_SCORE_MIN) {
            rejectedItems.add("信用画像综合分 " + overall + " 分（< " + PROFILE_SCORE_MIN + " 分），收入与经营画像不足，暂不可借");
            score -= 40;
        } else {
            passedItems.add("信用画像综合分 " + overall + " 分，达标（≥ " + PROFILE_SCORE_MIN + " 分）");
        }

        // 2. 收入客观性：近 30 天无收入或净现金流为负
        LocalDateTime since = LocalDateTime.now().minusDays(INCOME_WINDOW_DAYS);
        List<BizBookkeepingRecord> records = bookkeepingMapper.selectList(
                new LambdaQueryWrapper<BizBookkeepingRecord>()
                        .eq(BizBookkeepingRecord::getUserId, userId)
                        .ge(BizBookkeepingRecord::getHappenDate, since.toLocalDate()));
        BigDecimal income = records.stream()
                .filter(r -> "INCOME".equals(r.getRecordType()))
                .map(BizBookkeepingRecord::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal expense = records.stream()
                .filter(r -> "EXPENSE".equals(r.getRecordType()))
                .map(BizBookkeepingRecord::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal net = income.subtract(expense);
        if (income.compareTo(BigDecimal.ZERO) <= 0 || net.compareTo(BigDecimal.ZERO) < 0) {
            rejectedItems.add("近 30 天收入记录不足（收入 ¥" + income + "，净现金流 ¥" + net + "），收入不客观");
            score -= 25;
        } else {
            passedItems.add("近 30 天收入 ¥" + income + "、净现金流 ¥" + net + "，收入来源可核验");
        }

        // 3. 多头借贷：未结清在途借款笔数（还款冲抵后，与还款面板未结清明细一致）
        List<OngoingLoan> ongoingLoans = buildOngoingLoans(userId, creditType);
        long ongoing = ongoingLoans.size();
        if (ongoing >= MAX_ONGOING_LOANS) {
            rejectedItems.add("已有 " + ongoing + " 笔未结清借款（≥ " + MAX_ONGOING_LOANS + " 笔），多头借贷风险，请先还款再借");
            score -= 20;
        } else if (ongoing > 0) {
            passedItems.add("当前未结清借款 " + ongoing + " 笔，未触及多头借贷上限");
        }

        // 4. 长期未还：未结清借款中已超 60 天的笔数
        long longOverdue = ongoingLoans.stream()
                .filter(o -> o.loanDate != null
                        && ChronoUnit.DAYS.between(o.loanDate, LocalDate.now()) > LONG_OVERDUE_DAYS)
                .count();
        if (longOverdue > 0) {
            rejectedItems.add("存在 " + longOverdue + " 笔借款已超过 " + LONG_OVERDUE_DAYS + " 天未还，须结清后再借");
            score -= 30;
        } else {
            passedItems.add("无长期未还款项（均在 " + LONG_OVERDUE_DAYS + " 天内）");
        }

        // 5. 逾期风险预警：未处理 OVERDUE_RISK
        Long warnCount = riskWarningMapper.selectCount(
                new LambdaQueryWrapper<BizRiskWarning>()
                        .eq(BizRiskWarning::getUserId, userId)
                        .eq(BizRiskWarning::getWarningType, "OVERDUE_RISK")
                        .eq(BizRiskWarning::getIsHandled, 0));
        if (warnCount != null && warnCount > 0) {
            rejectedItems.add("存在 " + warnCount + " 条未处理的逾期风险预警，需先处置风险再借款");
            score -= 20;
        } else {
            passedItems.add("无未处理的逾期风险预警");
        }

        vo.setAiScore(Math.max(0, Math.min(100, score)));
        boolean passed = rejectedItems.isEmpty();
        vo.setPassed(passed);
        vo.setPassedItems(passedItems);
        vo.setRejectedItems(rejectedItems);
        if (passed) {
            vo.setSummary("AI 审查通过（模拟）：可继续借款");
        } else {
            vo.setSummary("AI 审查未通过（模拟）：存在 " + rejectedItems.size() + " 项风险，请先处理后再借");
        }
        log.info("[借款AI审查] 用户={}, 通过={}, 评分={}, 原因={}", userId, passed, vo.getAiScore(), rejectedItems);
        // 落库 AI 审查日志（管理端「AI审核记录」展示；模拟 AI）
        try {
            ObjectMapper om = new ObjectMapper();
            BizAiReviewLog rl = new BizAiReviewLog();
            rl.setUserId(userId);
            SysUser u = sysUserMapper.selectById(userId);
            rl.setUserName(u != null ? (u.getRealName() != null && !u.getRealName().isBlank() ? u.getRealName() : u.getUsername()) : String.valueOf(userId));
            rl.setCreditType(creditType);
            rl.setBizType("A_TYPE".equals(creditType) ? "WITHDRAW" : "ENTRUST_PAY");
            rl.setResult(passed ? "PASS" : "REJECT");
            rl.setAiScore(vo.getAiScore());
            rl.setPassedItems(om.writeValueAsString(passedItems));
            rl.setRejectedItems(om.writeValueAsString(rejectedItems));
            rl.setRequestNo(vo.getReviewNo());
            reviewLogMapper.insert(rl);
        } catch (Exception ex) {
            log.warn("[借款AI审查] 日志落库失败：{}", ex.getMessage());
        }
        return vo;
    }

    /**
     * 借款前 AI 审查（默认 A 类语义：须 B 转 A 成功）
     */
    public AiGuardVO guard(Long userId) {
        return guard(userId, "A_TYPE");
    }

    /** 未结清借款（仅计数用：剩余本金 > 0） */
    private static final class OngoingLoan {
        final String loanNo;
        final LocalDate loanDate;
        final BigDecimal remaining;
        OngoingLoan(String loanNo, LocalDate loanDate, BigDecimal remaining) {
            this.loanNo = loanNo;
            this.loanDate = loanDate;
            this.remaining = remaining;
        }
    }

    /**
     * 未结清在途借款列表（按笔 FIFO 冲抵，与还款面板未结清明细同口径）：
     * A 类 = WITHDRAW 提款流水；B 类 = 受托支付成功流水；还款按先进先出/指定结清冲抵本金。
     * 全部还清后返回空列表 → 多头借贷不再拦截。
     */
    private List<OngoingLoan> buildOngoingLoans(Long userId, String creditType) {
        BizCreditLimit limit = creditLimitMapper.selectOne(
                new LambdaQueryWrapper<BizCreditLimit>()
                        .eq(BizCreditLimit::getUserId, userId)
                        .eq(BizCreditLimit::getCreditType, creditType)
                        .last("LIMIT 1"));
        if (limit == null) {
            return new ArrayList<>();
        }
        List<String> nos = new ArrayList<>();
        List<LocalDate> dates = new ArrayList<>();
        List<BigDecimal> remaining = new ArrayList<>();
        if ("A_TYPE".equals(creditType)) {
            List<BizCreditTxn> ws = creditTxnMapper.selectList(new LambdaQueryWrapper<BizCreditTxn>()
                    .eq(BizCreditTxn::getCreditLimitId, limit.getId())
                    .eq(BizCreditTxn::getTxnType, "WITHDRAW")
                    .orderByAsc(BizCreditTxn::getTxnTime)
                    .orderByAsc(BizCreditTxn::getId));
            for (BizCreditTxn w : ws) {
                nos.add(w.getTxnNo());
                dates.add(w.getTxnTime() == null ? LocalDate.now() : w.getTxnTime().toLocalDate());
                remaining.add(w.getPrincipalAmount());
            }
        } else {
            List<BizEntrustPayment> ps = paymentMapper.selectList(new LambdaQueryWrapper<BizEntrustPayment>()
                    .eq(BizEntrustPayment::getUserId, userId)
                    .eq(BizEntrustPayment::getPaymentStatus, "SUCCESS")
                    .orderByAsc(BizEntrustPayment::getPaymentTime)
                    .orderByAsc(BizEntrustPayment::getId));
            for (BizEntrustPayment p : ps) {
                nos.add(p.getPaymentNo());
                dates.add(p.getPaymentTime() == null ? LocalDate.now() : p.getPaymentTime().toLocalDate());
                remaining.add(p.getAmount());
            }
        }
        List<BizCreditTxn> repays = creditTxnMapper.selectList(new LambdaQueryWrapper<BizCreditTxn>()
                .eq(BizCreditTxn::getCreditLimitId, limit.getId())
                .eq(BizCreditTxn::getTxnType, "REPAY")
                .orderByAsc(BizCreditTxn::getTxnTime)
                .orderByAsc(BizCreditTxn::getId));
        // ① 指定结清（target_loan_no）精确冲抵
        for (BizCreditTxn r : repays) {
            String target = r.getTargetLoanNo();
            if (target == null || target.isBlank()) continue;
            int idx = nos.indexOf(target);
            if (idx >= 0) {
                BigDecimal take = remaining.get(idx).min(r.getPrincipalAmount());
                remaining.set(idx, remaining.get(idx).subtract(take));
            }
        }
        // ② 普通还款 FIFO 冲抵
        for (BizCreditTxn r : repays) {
            if (r.getTargetLoanNo() != null && !r.getTargetLoanNo().isBlank()) continue;
            BigDecimal remain = r.getPrincipalAmount();
            for (int i = 0; i < remaining.size() && remain.compareTo(BigDecimal.ZERO) > 0; i++) {
                BigDecimal take = remaining.get(i).min(remain);
                remaining.set(i, remaining.get(i).subtract(take));
                remain = remain.subtract(take);
            }
        }
        List<OngoingLoan> result = new ArrayList<>();
        for (int i = 0; i < nos.size(); i++) {
            if (remaining.get(i).compareTo(BigDecimal.ZERO) <= 0) continue;
            result.add(new OngoingLoan(nos.get(i), dates.get(i), remaining.get(i)));
        }
        return result;
    }

    /**
     * 审查结果 VO
     */
    @lombok.Data
    public static class AiGuardVO {
        private Long userId;
        private String reviewNo;
        private Boolean passed;
        private Integer aiScore;
        private String summary;
        private List<String> passedItems = new ArrayList<>();
        private List<String> rejectedItems = new ArrayList<>();
    }
}
