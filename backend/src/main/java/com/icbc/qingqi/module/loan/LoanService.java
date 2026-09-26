package com.icbc.qingqi.module.loan;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.icbc.qingqi.common.BizException;
import com.icbc.qingqi.common.ErrorCode;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 模块2 轻创业智能授信：青创e贷
 * 银行侧能力（征信查询、放款）为模拟实现。
 * 核心规则：B类两步式——免费预审不查征信 → 100%受托支付；
 * A类建议最高5万元，B类预审额度区间1—2万元。
 */
@Service
public class LoanService {

    private final LoanApplyMapper loanApplyMapper;
    private final CreditLimitMapper creditLimitMapper;
    private final EntrustedPaymentMapper paymentMapper;

    public LoanService(LoanApplyMapper loanApplyMapper,
                       CreditLimitMapper creditLimitMapper,
                       EntrustedPaymentMapper paymentMapper) {
        this.loanApplyMapper = loanApplyMapper;
        this.creditLimitMapper = creditLimitMapper;
        this.paymentMapper = paymentMapper;
    }

    /**
     * 申请并执行免费预审（B类不查征信，仅做额度区间预估）
     */
    @Transactional
    public Long apply(Long userId, String loanType, BigDecimal amount, String purpose, String bizPlanUrl) {
        if (!"A".equals(loanType) && !"B".equals(loanType)) {
            throw new BizException(3001, "贷款类型仅支持 A/B");
        }
        LoanApply apply = new LoanApply();
        apply.setUserId(userId);
        apply.setLoanType(loanType);
        apply.setApplyAmount(amount);
        apply.setLoanPurpose(purpose);
        apply.setBizPlanUrl(bizPlanUrl);
        apply.setApproveStatus(0);
        apply.setCreateTime(LocalDateTime.now());

        // 免费预审：不触发征信硬查询（设计说明书 B 类两步式）
        apply.setPrecheckStatus(1);
        apply.setPrecheckRange("A".equals(loanType) ? "30000-50000" : "10000-20000");
        loanApplyMapper.insert(apply);
        return apply.getLoanApplyId();
    }

    /**
     * 模拟审批：预审通过后，按区间上限与申请金额取小
     */
    @Transactional
    public LoanApply approve(Long applyId) {
        LoanApply apply = mustGet(applyId);
        if (apply.getPrecheckStatus() == null || apply.getPrecheckStatus() != 1) {
            throw new BizException(3001, "请先完成免费预审");
        }
        BigDecimal maxAmount = "A".equals(apply.getLoanType())
                ? new BigDecimal("50000") : new BigDecimal("20000");
        BigDecimal approveAmount = apply.getApplyAmount().min(maxAmount);
        apply.setApproveStatus(1);
        apply.setApproveAmount(approveAmount);
        // 演示利率：B类财政贴息后年化3.95%，A类4.35%（模拟值）
        apply.setInterestRate("B".equals(apply.getLoanType())
                ? new BigDecimal("0.0395") : new BigDecimal("0.0435"));
        apply.setApprover("青启e城智能授信（模拟）");
        apply.setApproveTime(LocalDateTime.now());
        loanApplyMapper.updateById(apply);
        return apply;
    }

    /**
     * 100%受托支付（模拟）：定向支付至商户白名单，不进入个人账户
     */
    @Transactional
    public EntrustedPayment entrustedPay(Long applyId, String merchantName, BigDecimal amount) {
        LoanApply apply = mustGet(applyId);
        if (apply.getApproveStatus() == null || apply.getApproveStatus() != 1) {
            throw new BizException(3001, "贷款未审批通过，不能受托支付");
        }
        if (amount == null || amount.compareTo(apply.getApproveAmount()) > 0) {
            throw new BizException(3001, "受托支付金额不得超过审批额度");
        }
        EntrustedPayment payment = new EntrustedPayment();
        payment.setLoanApplyId(applyId);
        payment.setMerchantId(9001L); // 演示白名单商户
        payment.setMerchantName(merchantName);
        payment.setPayAmount(amount);
        payment.setPayTime(LocalDateTime.now());
        payment.setPayStatus(1);
        payment.setVoucherUrl("https://demo.qingqi.local/voucher/" + applyId);
        paymentMapper.insert(payment);

        // A类循环额度扣减演示
        CreditLimit limit = creditLimitMapper.selectOne(
                Wrappers.<CreditLimit>lambdaQuery()
                        .eq(CreditLimit::getUserId, apply.getUserId())
                        .eq(CreditLimit::getCreditType, "A")
                        .eq(CreditLimit::getCreditStatus, 0));
        if (limit != null) {
            limit.setUsedAmount(limit.getUsedAmount().add(amount));
            limit.setRemainAmount(limit.getCreditAmount().subtract(limit.getUsedAmount()));
            limit.setUpdateTime(LocalDateTime.now());
            creditLimitMapper.updateById(limit);
        }
        return payment;
    }

    public List<LoanApply> listByUser(Long userId) {
        return loanApplyMapper.selectList(
                Wrappers.<LoanApply>lambdaQuery()
                        .eq(LoanApply::getUserId, userId)
                        .orderByDesc(LoanApply::getCreateTime));
    }

    public Map<String, Object> detail(Long applyId) {
        LoanApply apply = mustGet(applyId);
        return Map.of(
                "apply", apply,
                "payments", paymentMapper.selectList(
                        Wrappers.<EntrustedPayment>lambdaQuery()
                                .eq(EntrustedPayment::getLoanApplyId, applyId)));
    }

    private LoanApply mustGet(Long applyId) {
        LoanApply apply = loanApplyMapper.selectById(applyId);
        if (apply == null) {
            throw new BizException(ErrorCode.NOT_FOUND);
        }
        return apply;
    }
}
