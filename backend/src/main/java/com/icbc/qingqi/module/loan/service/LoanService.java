package com.icbc.qingqi.module.loan.service;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.icbc.qingqi.common.BizException;
import com.icbc.qingqi.common.ErrorCode;
import com.icbc.qingqi.module.loan.dto.*;
import com.icbc.qingqi.module.loan.entity.*;
import com.icbc.qingqi.module.loan.mapper.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * 轻创业智能授信服务
 * <p>
 * 覆盖 L-1 ~ L-3：
 * - L-1 B 类免费预审：不查征信，按创业计划 + 人群资质给出 1—2 万元额度区间
 * - L-2 A/B 双轨额度展示：A 类最高 5 万循环额度、B 类小额定向
 * - L-3 受托支付模拟：100% 定向打给预置商户，资金不经过个人账户
 * <p>
 * 所有银行能力（授信、支付）均为模拟桩，演示数据标注"模拟"。
 */
@Slf4j
@Service
public class LoanService {

    private final BizMerchantMapper merchantMapper;
    private final BizLoanApplicationMapper applicationMapper;
    private final BizCreditLimitMapper creditLimitMapper;
    private final BizEntrustPaymentMapper paymentMapper;

    // 贷款类型
    private static final String TYPE_A = "A_TYPE";
    private static final String TYPE_B = "B_TYPE";

    // 预审结果
    private static final String PRE_ELIGIBLE = "ELIGIBLE";
    private static final String PRE_NOT_ELIGIBLE = "NOT_ELIGIBLE";
    private static final String PRE_NEED_INFO = "NEED_MORE_INFO";

    // 申请状态
    private static final String STATUS_PRE_CHECK = "PRE_CHECK";
    private static final String STATUS_APPROVED = "APPROVED";

    // 支付状态
    private static final String PAY_SUCCESS = "SUCCESS";

    // 扶持人群（额度上限 20000）
    private static final Set<String> SUPPORTED_CROWDS = Set.of(
            "STUDENT", "ENTREPRENEUR", "VETERAN", "DISABLED", "FARMER");

    // 创业计划关键词（命中则额度下限 10000）
    private static final List<String> PLAN_KEYWORDS = List.of(
            "市集", "摊位", "文创", "电商", "餐饮", "直播", "孵化", "小店", "手作", "农产品");

    // 额度常量
    private static final BigDecimal LIMIT_MAX_SUPPORTED = new BigDecimal("20000.00");
    private static final BigDecimal LIMIT_MAX_NORMAL = new BigDecimal("10000.00");
    private static final BigDecimal LIMIT_MIN_KEYWORD = new BigDecimal("10000.00");
    private static final BigDecimal LIMIT_MIN_NO_KEYWORD = new BigDecimal("5000.00");
    private static final BigDecimal A_TYPE_TOTAL = new BigDecimal("50000.00");
    private static final BigDecimal A_TYPE_RATE = new BigDecimal("0.0385");
    private static final BigDecimal B_TYPE_RATE = new BigDecimal("0.0435");

    public LoanService(BizMerchantMapper merchantMapper,
                       BizLoanApplicationMapper applicationMapper,
                       BizCreditLimitMapper creditLimitMapper,
                       BizEntrustPaymentMapper paymentMapper) {
        this.merchantMapper = merchantMapper;
        this.applicationMapper = applicationMapper;
        this.creditLimitMapper = creditLimitMapper;
        this.paymentMapper = paymentMapper;
    }

    // ============================================================
    //  L-1 B 类免费预审（不查征信）
    // ============================================================

    /**
     * B 类免费预审
     * <p>
     * 规则（服务层模拟，不查征信）：
     * - 扶持人群（STUDENT/ENTREPRENEUR/VETERAN/DISABLED/FARMER）→ 上限 20000；普通人群 → 上限 10000
     * - 创业计划命中关键词 → 下限 10000；未命中 → 下限 5000
     * - 扶持+命中 → [10000, 20000]；扶持+未命中 → [5000, 20000]
     * - 普通+命中 → 10000；普通+未命中 → NEED_MORE_INFO
     */
    @Transactional(rollbackFor = Exception.class)
    public LoanPrecheckVO precheck(Long userId, LoanPrecheckDTO dto) {
        boolean supported = SUPPORTED_CROWDS.contains(dto.getCrowdType());
        boolean keywordHit = PLAN_KEYWORDS.stream().anyMatch(k ->
                dto.getBusinessPlan() != null && dto.getBusinessPlan().contains(k));

        String result;
        BigDecimal min;
        BigDecimal max;
        List<String> details = new ArrayList<>();

        if (supported) {
            max = LIMIT_MAX_SUPPORTED;
            min = keywordHit ? LIMIT_MIN_KEYWORD : LIMIT_MIN_NO_KEYWORD;
            result = PRE_ELIGIBLE;
            details.add("人群资质[" + crowdName(dto.getCrowdType()) + "]属于政策扶持范围，额度上限2万元");
        } else {
            max = LIMIT_MAX_NORMAL;
            if (keywordHit) {
                min = LIMIT_MIN_KEYWORD;
                result = PRE_ELIGIBLE;
                details.add("人群资质[" + crowdName(dto.getCrowdType()) + "]，创业计划明确，可授信1万元");
            } else {
                min = BigDecimal.ZERO;
                result = PRE_NEED_INFO;
                details.add("人群资质[" + crowdName(dto.getCrowdType()) + "]，创业计划描述不足，请补充经营计划后再申请");
            }
        }

        if (keywordHit) {
            details.add("创业计划命中经营关键词，额度下限1万元");
        }
        details.add("本预审为模拟服务，不查询个人征信。【模拟】");

        String detailText = String.join("；", details);

        // 落库贷款申请（B 类预审）
        BizLoanApplication app = new BizLoanApplication();
        app.setApplyNo(generateNo("LA"));
        app.setUserId(userId);
        app.setLoanType(TYPE_B);
        app.setApplyAmount(dto.getApplyAmount());
        app.setPurpose(dto.getPurpose());
        app.setBusinessPlan(dto.getBusinessPlan());
        app.setCrowdType(dto.getCrowdType());
        app.setPreCheckResult(result);
        app.setPreCheckMinAmount(min);
        app.setPreCheckMaxAmount(max);
        app.setPreCheckDetail(detailText);
        // 演示口径：B 类预审通过即自动审批通过（无需人工审批），便于走通受托支付闭环
        if (PRE_ELIGIBLE.equals(result)) {
            app.setApplyStatus(STATUS_APPROVED);
            app.setApproveAmount(max);
            app.setApproveTime(LocalDateTime.now());
        } else {
            app.setApplyStatus(STATUS_PRE_CHECK);
        }
        app.setSubmitTime(LocalDateTime.now());
        applicationMapper.insert(app);

        log.info("[B类预审] 用户={}, 人群={}, 关键词命中={}, 结果={}, 额度区间=[{}, {}]",
                userId, dto.getCrowdType(), keywordHit, result, min, max);

        // 预审通过时自动生成 B 类授信额度（演示）
        if (PRE_ELIGIBLE.equals(result)) {
            ensureBCreditLimit(userId, max);
        }

        return toPrecheckVO(app);
    }

    // ============================================================
    //  L-2 A/B 双轨额度展示
    // ============================================================

    /**
     * 查询当前用户的 A/B 双轨授信额度
     * <p>
     * - A 类：最高 5 万循环额度，随借随还
     * - B 类：小额定向，按预审额度
     * 若用户暂无记录，按演示规则生成（A 类默认 5 万，B 类取预审上限）
     */
    public List<CreditLimitVO> myCreditLimits(Long userId) {
        List<BizCreditLimit> limits = creditLimitMapper.selectList(
                new LambdaQueryWrapper<BizCreditLimit>().eq(BizCreditLimit::getUserId, userId));

        // 若没有 A 类额度，生成演示 A 类 5 万循环额度
        boolean hasA = limits.stream().anyMatch(l -> TYPE_A.equals(l.getCreditType()));
        if (!hasA) {
            BizCreditLimit a = createCreditLimit(userId, TYPE_A, A_TYPE_TOTAL, A_TYPE_RATE);
            limits.add(a);
        }
        // 若没有 B 类额度，尝试用最近一次预审上限生成
        boolean hasB = limits.stream().anyMatch(l -> TYPE_B.equals(l.getCreditType()));
        if (!hasB) {
            BizLoanApplication last = applicationMapper.selectOne(
                    new LambdaQueryWrapper<BizLoanApplication>()
                            .eq(BizLoanApplication::getUserId, userId)
                            .eq(BizLoanApplication::getLoanType, TYPE_B)
                            .eq(BizLoanApplication::getPreCheckResult, PRE_ELIGIBLE)
                            .orderByDesc(BizLoanApplication::getCreateTime)
                            .last("LIMIT 1"));
            BigDecimal bTotal = (last != null && last.getPreCheckMaxAmount() != null)
                    ? last.getPreCheckMaxAmount() : LIMIT_MAX_NORMAL;
            BizCreditLimit b = createCreditLimit(userId, TYPE_B, bTotal, B_TYPE_RATE);
            limits.add(b);
        }

        return limits.stream().map(this::toCreditLimitVO).toList();
    }

    private BizCreditLimit createCreditLimit(Long userId, String type, BigDecimal total, BigDecimal rate) {
        BizCreditLimit limit = new BizCreditLimit();
        limit.setUserId(userId);
        limit.setCreditType(type);
        limit.setTotalLimit(total);
        limit.setUsedLimit(BigDecimal.ZERO);
        limit.setAvailableLimit(total);
        limit.setInterestRate(rate);
        limit.setStatus("ACTIVE");
        limit.setEffectiveDate(LocalDate.now());
        limit.setExpireDate(LocalDate.now().plusYears(1));
        creditLimitMapper.insert(limit);
        return limit;
    }

    /**
     * 确保 B 类授信额度存在（预审通过时调用）
     */
    private void ensureBCreditLimit(Long userId, BigDecimal total) {
        BizCreditLimit existing = creditLimitMapper.selectOne(
                new LambdaQueryWrapper<BizCreditLimit>()
                        .eq(BizCreditLimit::getUserId, userId)
                        .eq(BizCreditLimit::getCreditType, TYPE_B));
        if (existing == null) {
            createCreditLimit(userId, TYPE_B, total, B_TYPE_RATE);
        }
    }

    // ============================================================
    //  L-3 受托支付（100% 定向商户，不经过个人账户）
    // ============================================================

    /**
     * 受托支付
     * <p>
     * 校验：申请状态=APPROVED、金额≤可用额度、商户存在且正常；
     * 资金路径：借款人授信额度 → 商户收款账户（不经过借款人个人账户）。
     */
    @Transactional(rollbackFor = Exception.class)
    public EntrustPaymentVO entrustPay(Long userId, EntrustPayDTO dto) {
        BizLoanApplication app = applicationMapper.selectById(dto.getLoanApplicationId());
        if (app == null) {
            throw new BizException(ErrorCode.BIZ_RULE_NOT_MET, "贷款申请不存在");
        }
        if (!app.getUserId().equals(userId)) {
            throw new BizException(ErrorCode.FORBIDDEN, "无权操作该贷款申请");
        }
        if (!STATUS_APPROVED.equals(app.getApplyStatus())) {
            throw new BizException(ErrorCode.BIZ_RULE_NOT_MET,
                    "申请状态为" + statusName(app.getApplyStatus()) + "，不可受托支付");
        }

        // 商户校验
        BizMerchant merchant = merchantMapper.selectById(dto.getMerchantId());
        if (merchant == null || merchant.getStatus() == null || merchant.getStatus() != 1) {
            throw new BizException(ErrorCode.BIZ_RULE_NOT_MET, "收款商户不存在或未启用");
        }

        // 扣减额度（按申请类型对应的授信额度）
        BizCreditLimit limit = creditLimitMapper.selectOne(
                new LambdaQueryWrapper<BizCreditLimit>()
                        .eq(BizCreditLimit::getUserId, userId)
                        .eq(BizCreditLimit::getCreditType, app.getLoanType()));
        if (limit == null) {
            throw new BizException(ErrorCode.BIZ_RULE_NOT_MET, "无可用授信额度");
        }
        if (limit.getAvailableLimit().compareTo(dto.getAmount()) < 0) {
            throw new BizException(ErrorCode.BIZ_RULE_NOT_MET,
                    "可用额度不足（可用：" + limit.getAvailableLimit() + "）");
        }

        // 扣减额度
        limit.setUsedLimit(limit.getUsedLimit().add(dto.getAmount()));
        limit.setAvailableLimit(limit.getAvailableLimit().subtract(dto.getAmount()));
        creditLimitMapper.updateById(limit);

        // 生成受托支付流水
        BizEntrustPayment payment = new BizEntrustPayment();
        payment.setPaymentNo(generateNo("EP"));
        payment.setUserId(userId);
        payment.setLoanApplicationId(app.getId());
        payment.setMerchantId(merchant.getId());
        payment.setMerchantName(merchant.getMerchantName());
        payment.setAmount(dto.getAmount());
        payment.setPurpose(dto.getPurpose());
        payment.setTradeProof(dto.getTradeProof());
        payment.setPaymentStatus(PAY_SUCCESS);
        payment.setPaymentTime(LocalDateTime.now());
        paymentMapper.insert(payment);

        log.info("[受托支付] 用户={}, 商户={}, 金额={}, 授信类型={}, 资金定向打款至商户（不经过个人账户）【模拟】",
                userId, merchant.getMerchantName(), dto.getAmount(), app.getLoanType());

        return toPaymentVO(payment);
    }

    // ============================================================
    //  商户列表 / 申请列表 / 详情
    // ============================================================

    public List<BizMerchant> listMerchants() {
        return merchantMapper.selectList(
                new LambdaQueryWrapper<BizMerchant>().eq(BizMerchant::getStatus, 1));
    }

    public Page<BizLoanApplication> pageApplications(Long userId, int pageNum, int pageSize, String status) {
        Page<BizLoanApplication> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<BizLoanApplication> wrapper = new LambdaQueryWrapper<BizLoanApplication>()
                .eq(BizLoanApplication::getUserId, userId)
                .orderByDesc(BizLoanApplication::getCreateTime);
        if (status != null && !status.isEmpty()) {
            wrapper.eq(BizLoanApplication::getApplyStatus, status);
        }
        return applicationMapper.selectPage(page, wrapper);
    }

    public BizLoanApplication getApplication(Long userId, Long id) {
        BizLoanApplication app = applicationMapper.selectById(id);
        if (app == null) {
            throw new BizException(ErrorCode.BIZ_RULE_NOT_MET, "贷款申请不存在");
        }
        if (!app.getUserId().equals(userId)) {
            throw new BizException(ErrorCode.FORBIDDEN, "无权查看该申请");
        }
        return app;
    }

    // ============================================================
    //  工具方法
    // ============================================================

    private String generateNo(String prefix) {
        return prefix + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS"));
    }

    private String crowdName(String crowdType) {
        return switch (crowdType == null ? "" : crowdType) {
            case "STUDENT" -> "在校大学生";
            case "ENTREPRENEUR" -> "青年创业者";
            case "VETERAN" -> "退役军人";
            case "DISABLED" -> "残障青年";
            case "FARMER" -> "返乡青年";
            case "UNEMPLOYED" -> "失业青年";
            default -> "其他";
        };
    }

    private String preCheckResultName(String result) {
        return switch (result == null ? "" : result) {
            case PRE_ELIGIBLE -> "预审通过";
            case PRE_NOT_ELIGIBLE -> "预审不通过";
            case PRE_NEED_INFO -> "需补充材料";
            default -> result;
        };
    }

    private String statusName(String status) {
        return switch (status == null ? "" : status) {
            case STATUS_PRE_CHECK -> "预审中";
            case "PENDING_APPROVAL" -> "待审批";
            case STATUS_APPROVED -> "已通过";
            case "REJECTED" -> "已拒绝";
            case "CANCELLED" -> "已取消";
            default -> status;
        };
    }

    private String creditTypeName(String type) {
        return TYPE_A.equals(type) ? "A类5万循环" : "B类小额定向";
    }

    private String payStatusName(String status) {
        return switch (status == null ? "" : status) {
            case "PENDING" -> "待支付";
            case "PROCESSING" -> "处理中";
            case PAY_SUCCESS -> "支付成功";
            case "FAILED" -> "支付失败";
            default -> status;
        };
    }

    private LoanPrecheckVO toPrecheckVO(BizLoanApplication app) {
        LoanPrecheckVO vo = new LoanPrecheckVO();
        BeanUtil.copyProperties(app, vo);
        vo.setPreCheckResultName(preCheckResultName(app.getPreCheckResult()));
        return vo;
    }

    private CreditLimitVO toCreditLimitVO(BizCreditLimit limit) {
        CreditLimitVO vo = new CreditLimitVO();
        BeanUtil.copyProperties(limit, vo);
        vo.setCreditTypeName(creditTypeName(limit.getCreditType()));
        vo.setStatusName("ACTIVE".equals(limit.getStatus()) ? "正常" : limit.getStatus());
        vo.setRemark(TYPE_A.equals(limit.getCreditType())
                ? "随借随还，按实际用款计息"
                : "定向受托支付，仅限指定商户");
        return vo;
    }

    private EntrustPaymentVO toPaymentVO(BizEntrustPayment p) {
        EntrustPaymentVO vo = new EntrustPaymentVO();
        BeanUtil.copyProperties(p, vo);
        vo.setPaymentStatusName(payStatusName(p.getPaymentStatus()));
        vo.setFundPath("借款人授信额度 → 商户「" + p.getMerchantName() + "」收款账户（不经过借款人个人账户）【模拟】");
        return vo;
    }
}
