package com.icbc.qingqi.module.guarantee;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.icbc.qingqi.common.BizException;
import com.icbc.qingqi.common.ErrorCode;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

/**
 * 模块1 安居金融风控：租房履约保函业务
 * 银行侧能力（开函、赔付）为模拟实现
 */
@Service
public class GuaranteeService {

    /** 演示默认费率：年化1.0%（试点区间0.8%—1.5%） */
    private static final BigDecimal DEFAULT_FEE_RATE = new BigDecimal("0.0100");

    private final LeaseContractMapper leaseContractMapper;
    private final GuaranteeApplyMapper applyMapper;
    private final GuaranteeInfoMapper infoMapper;

    public GuaranteeService(LeaseContractMapper leaseContractMapper,
                            GuaranteeApplyMapper applyMapper,
                            GuaranteeInfoMapper infoMapper) {
        this.leaseContractMapper = leaseContractMapper;
        this.applyMapper = applyMapper;
        this.infoMapper = infoMapper;
    }

    /**
     * ①发起申请 + ③AI合同复审（模拟规则）：低风险自动通过，命中规则转人工
     */
    @Transactional
    public Long apply(Long userId, LeaseContract contract, Integer termMonths) {
        if (termMonths == null || termMonths <= 0) {
            termMonths = 12;
        }
        contract.setUserId(userId);
        contract.setCreateTime(LocalDateTime.now());
        String risk = reviewContract(contract);
        contract.setReviewStatus(risk == null ? 1 : 2);
        contract.setReviewResult(risk == null ? "低风险，自动通过（模拟）" : risk);
        contract.setReviewTime(LocalDateTime.now());
        leaseContractMapper.insert(contract);

        GuaranteeApply apply = new GuaranteeApply();
        apply.setContractId(contract.getContractId());
        apply.setUserId(userId);
        apply.setGuaranteeAmount(contract.getDepositAmount());
        apply.setGuaranteeTermMonths(termMonths);
        apply.setFeeRate(DEFAULT_FEE_RATE);
        apply.setFeeAmount(calcFee(contract.getDepositAmount(), DEFAULT_FEE_RATE, termMonths));
        apply.setApplyStatus(0);
        apply.setPayStatus(0);
        apply.setCreateTime(LocalDateTime.now());
        applyMapper.insert(apply);
        return apply.getApplyId();
    }

    /**
     * ②房东在线确认（模拟电子签署）
     */
    public void landlordConfirm(Long applyId) {
        GuaranteeApply apply = mustGetApply(applyId);
        if (apply.getApplyStatus() != 0) {
            throw new BizException(3001, "当前状态不允许确认");
        }
        apply.setApplyStatus(1);
        applyMapper.updateById(apply);
    }

    /**
     * ④缴费出函（模拟）：生成电子保函
     */
    @Transactional
    public void payAndIssue(Long applyId) {
        GuaranteeApply apply = mustGetApply(applyId);
        if (apply.getApplyStatus() != 1) {
            throw new BizException(3001, "需先完成房东确认");
        }
        apply.setPayStatus(1);
        apply.setApplyStatus(2);
        applyMapper.updateById(apply);

        LeaseContract contract = leaseContractMapper.selectById(apply.getContractId());
        GuaranteeInfo info = new GuaranteeInfo();
        info.setApplyId(applyId);
        info.setGuaranteeNo("GQ" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss")));
        info.setApplicantId(apply.getUserId());
        info.setBeneficiary(contract.getLandlordName());
        info.setGuaranteeAmount(apply.getGuaranteeAmount());
        info.setIssueDate(LocalDate.now());
        info.setExpireDate(LocalDate.now().plusMonths(apply.getGuaranteeTermMonths()));
        info.setGuaranteeStatus(0);
        info.setEGuaranteeUrl("https://demo.qingqi.local/guarantee/" + info.getGuaranteeNo());
        infoMapper.insert(info);
    }

    public List<GuaranteeApply> listByUser(Long userId) {
        return applyMapper.selectList(
                Wrappers.<GuaranteeApply>lambdaQuery()
                        .eq(GuaranteeApply::getUserId, userId)
                        .orderByDesc(GuaranteeApply::getCreateTime));
    }

    public Map<String, Object> detail(Long applyId) {
        GuaranteeApply apply = mustGetApply(applyId);
        return Map.of(
                "apply", apply,
                "contract", leaseContractMapper.selectById(apply.getContractId()),
                "guarantee", infoMapper.selectOne(
                        Wrappers.<GuaranteeInfo>lambdaQuery().eq(GuaranteeInfo::getApplyId, applyId)));
    }

    private GuaranteeApply mustGetApply(Long applyId) {
        GuaranteeApply apply = applyMapper.selectById(applyId);
        if (apply == null) {
            throw new BizException(ErrorCode.NOT_FOUND);
        }
        return apply;
    }

    /**
     * AI 合同复审模拟规则：押金异常/月租异常/风险词命中 → 转人工
     */
    private String reviewContract(LeaseContract c) {
        if (c.getDepositAmount() == null || c.getMonthlyRent() == null) {
            return "关键字段缺失，转人工复核";
        }
        if (c.getDepositAmount().compareTo(c.getMonthlyRent().multiply(BigDecimal.valueOf(3))) > 0) {
            return "押金金额异常（超过3个月租金），转人工复核";
        }
        if (c.getMonthlyRent().compareTo(BigDecimal.valueOf(100)) < 0) {
            return "月租金异常偏低，转人工复核";
        }
        String addr = c.getHouseAddress() == null ? "" : c.getHouseAddress();
        if (addr.contains("租金贷") || addr.contains("霸王条款")) {
            return "命中高风险词，转人工复核";
        }
        return null;
    }

    /** 保函费 = 保函金额 × 年化费率 × 期限月 / 12 */
    private BigDecimal calcFee(BigDecimal amount, BigDecimal rate, int months) {
        return amount.multiply(rate).multiply(BigDecimal.valueOf(months))
                .divide(BigDecimal.valueOf(12), 2, RoundingMode.HALF_UP);
    }
}
