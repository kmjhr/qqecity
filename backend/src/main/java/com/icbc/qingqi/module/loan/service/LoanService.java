package com.icbc.qingqi.module.loan.service;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.icbc.qingqi.common.BizException;
import com.icbc.qingqi.common.ErrorCode;
import com.icbc.qingqi.module.bookkeeping.entity.BizBookkeepingRecord;
import com.icbc.qingqi.module.bookkeeping.mapper.BizBookkeepingRecordMapper;
import com.icbc.qingqi.module.loan.dto.*;
import com.icbc.qingqi.module.loan.entity.*;
import com.icbc.qingqi.module.loan.mapper.*;
import com.icbc.qingqi.module.message.entity.SysMessage;
import com.icbc.qingqi.module.message.mapper.SysMessageMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
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
    private final BizCreditTxnMapper creditTxnMapper;
    private final BizBookkeepingRecordMapper bookkeepingMapper;
    private final SysMessageMapper messageMapper;

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
                       BizEntrustPaymentMapper paymentMapper,
                       BizCreditTxnMapper creditTxnMapper,
                       BizBookkeepingRecordMapper bookkeepingMapper,
                       SysMessageMapper messageMapper) {
        this.merchantMapper = merchantMapper;
        this.applicationMapper = applicationMapper;
        this.creditLimitMapper = creditLimitMapper;
        this.paymentMapper = paymentMapper;
        this.creditTxnMapper = creditTxnMapper;
        this.bookkeepingMapper = bookkeepingMapper;
        this.messageMapper = messageMapper;
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
     * B 类预审通过后进入 6 个月观察期（缺口 #11）
     */
    private void ensureBCreditLimit(Long userId, BigDecimal total) {
        BizCreditLimit existing = creditLimitMapper.selectOne(
                new LambdaQueryWrapper<BizCreditLimit>()
                        .eq(BizCreditLimit::getUserId, userId)
                        .eq(BizCreditLimit::getCreditType, TYPE_B));
        if (existing == null) {
            BizCreditLimit b = createCreditLimit(userId, TYPE_B, total, B_TYPE_RATE);
            // 启动观察期
            b.setObservationStatus("OBSERVING");
            b.setObservationStart(LocalDate.now());
            b.setObservationMonths(0);
            b.setObservationScore(0);
            creditLimitMapper.updateById(b);

            // 站内信：观察期开始
            sendInternalMessage(userId, "B类授信观察期已开始",
                    "您的B类预审已通过，进入6个月观察期。观察期内通过受托支付、记账等积累信用，达标可转A类循环贷并提额。【模拟】",
                    "LOAN", b.getId());
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
        // 白名单校验（缺口 #6）：仅白名单（VERIFIED）商户可受托支付
        if (!"VERIFIED".equals(merchant.getVerifyStatus())) {
            String statusName = "PENDING".equals(merchant.getVerifyStatus()) ? "灰名单审核中" : "未认证";
            throw new BizException(ErrorCode.BIZ_RULE_NOT_MET,
                    "商户「" + merchant.getMerchantName() + "」当前为" + statusName + "，非白名单商户不可受托支付");
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

    // ============================================================
    //  L-3-补 A 类循环贷随借随还（缺口 #10）
    // ============================================================

    /**
     * A 类循环贷提款
     * <p>
     * 从 A 类 5 万循环额度中分次提款，按日计息（年化 3.85% 模拟）
     */
    @Transactional(rollbackFor = Exception.class)
    public CreditTxnVO withdraw(Long userId, WithdrawDTO dto) {
        BizCreditLimit limit = getActiveACreditLimit(userId);
        if (limit.getAvailableLimit().compareTo(dto.getAmount()) < 0) {
            throw new BizException(ErrorCode.BIZ_RULE_NOT_MET,
                    "可用额度不足（可用：¥" + limit.getAvailableLimit() + "）");
        }

        // 扣减额度
        limit.setUsedLimit(limit.getUsedLimit().add(dto.getAmount()));
        limit.setAvailableLimit(limit.getAvailableLimit().subtract(dto.getAmount()));
        creditLimitMapper.updateById(limit);

        // 生成提款流水
        BizCreditTxn txn = new BizCreditTxn();
        txn.setTxnNo(generateNo("CW"));
        txn.setUserId(userId);
        txn.setCreditLimitId(limit.getId());
        txn.setTxnType("WITHDRAW");
        txn.setPrincipalAmount(dto.getAmount());
        txn.setInterestAmount(BigDecimal.ZERO);
        txn.setBorrowDays(0);
        txn.setBalanceAfter(limit.getAvailableLimit());
        txn.setRemark(dto.getPurpose() != null ? dto.getPurpose() : "循环贷提款（模拟）");
        txn.setTxnTime(LocalDateTime.now());
        creditTxnMapper.insert(txn);

        log.info("[A类提款] 用户={}, 金额={}, 可用余额={}", userId, dto.getAmount(), limit.getAvailableLimit());
        return toTxnVO(txn);
    }

    /**
     * A 类循环贷还款
     * <p>
     * 归还后额度自动恢复，按实际用款天数和利率计息（年化 3.85% 模拟）
     */
    @Transactional(rollbackFor = Exception.class)
    public CreditTxnVO repay(Long userId, RepayDTO dto) {
        BizCreditLimit limit = getActiveACreditLimit(userId);
        if (limit.getUsedLimit().compareTo(BigDecimal.ZERO) <= 0) {
            throw new BizException(ErrorCode.BIZ_RULE_NOT_MET, "当前无待还本金");
        }

        // 找最早一笔未还完的提款记录计算计息天数
        BizCreditTxn earliestWithdraw = creditTxnMapper.selectOne(
                new LambdaQueryWrapper<BizCreditTxn>()
                        .eq(BizCreditTxn::getUserId, userId)
                        .eq(BizCreditTxn::getTxnType, "WITHDRAW")
                        .orderByAsc(BizCreditTxn::getTxnTime)
                        .last("LIMIT 1"));

        int borrowDays = 1;
        if (earliestWithdraw != null && earliestWithdraw.getTxnTime() != null) {
            borrowDays = (int) ChronoUnit.DAYS.between(earliestWithdraw.getTxnTime().toLocalDate(), LocalDate.now());
            if (borrowDays < 1) borrowDays = 1;
        }

        // 计息：本金 × 年化利率 × 天数 / 365
        BigDecimal repayPrincipal = dto.getAmount().min(limit.getUsedLimit());
        BigDecimal interest = repayPrincipal
                .multiply(A_TYPE_RATE)
                .multiply(new BigDecimal(borrowDays))
                .divide(new BigDecimal("365"), 2, RoundingMode.HALF_UP);

        // 恢复额度（本金+利息都归还）
        BigDecimal totalRepay = repayPrincipal.add(interest);
        limit.setUsedLimit(limit.getUsedLimit().subtract(repayPrincipal));
        limit.setAvailableLimit(limit.getAvailableLimit().add(repayPrincipal));
        creditLimitMapper.updateById(limit);

        // 生成还款流水
        BizCreditTxn txn = new BizCreditTxn();
        txn.setTxnNo(generateNo("CR"));
        txn.setUserId(userId);
        txn.setCreditLimitId(limit.getId());
        txn.setTxnType("REPAY");
        txn.setPrincipalAmount(repayPrincipal);
        txn.setInterestAmount(interest);
        txn.setBorrowDays(borrowDays);
        txn.setBalanceAfter(limit.getAvailableLimit());
        txn.setRemark("循环贷还款，利息¥" + interest + "（" + borrowDays + "天，年化3.85%模拟）");
        txn.setTxnTime(LocalDateTime.now());
        creditTxnMapper.insert(txn);

        log.info("[A类还款] 用户={}, 本金={}, 利息={}, 天数={}, 可用余额={}",
                userId, repayPrincipal, interest, borrowDays, limit.getAvailableLimit());
        return toTxnVO(txn);
    }

    /**
     * 查询循环贷流水
     */
    public List<CreditTxnVO> listCreditTxns(Long userId) {
        List<BizCreditTxn> txns = creditTxnMapper.selectList(
                new LambdaQueryWrapper<BizCreditTxn>()
                        .eq(BizCreditTxn::getUserId, userId)
                        .orderByDesc(BizCreditTxn::getTxnTime));
        return txns.stream().map(this::toTxnVO).toList();
    }

    // ============================================================
    //  B 转 A 观察期（缺口 #11）
    // ============================================================

    /**
     * 查询观察期状态
     */
    public ObservationVO getObservationStatus(Long userId) {
        BizCreditLimit bLimit = creditLimitMapper.selectOne(
                new LambdaQueryWrapper<BizCreditLimit>()
                        .eq(BizCreditLimit::getUserId, userId)
                        .eq(BizCreditLimit::getCreditType, TYPE_B));
        if (bLimit == null) {
            throw new BizException(ErrorCode.BIZ_RULE_NOT_MET, "暂无B类授信，需先完成B类预审");
        }

        ObservationVO vo = new ObservationVO();
        vo.setCreditLimitId(bLimit.getId());
        vo.setCreditType(TYPE_B);
        vo.setObservationStatus(bLimit.getObservationStatus() != null ? bLimit.getObservationStatus() : "NONE");
        vo.setObservationStatusName(observationStatusName(bLimit.getObservationStatus()));
        vo.setObservationMonths(bLimit.getObservationMonths() != null ? bLimit.getObservationMonths() : 0);
        vo.setObservationScore(bLimit.getObservationScore() != null ? bLimit.getObservationScore() : 0);
        vo.setMonthsRemaining(Math.max(0, 6 - vo.getObservationMonths()));
        vo.setPromotionThreshold(60);
        vo.setTotalLimit(bLimit.getTotalLimit());
        vo.setAvailableLimit(bLimit.getAvailableLimit());
        vo.setPromoted("PROMOTED".equals(bLimit.getObservationStatus()));

        // 查A类额度（如已转A）
        if (vo.getPromoted()) {
            BizCreditLimit aLimit = creditLimitMapper.selectOne(
                    new LambdaQueryWrapper<BizCreditLimit>()
                            .eq(BizCreditLimit::getUserId, userId)
                            .eq(BizCreditLimit::getCreditType, TYPE_A));
            if (aLimit != null) {
                vo.setPromotedLimit(aLimit.getTotalLimit());
            }
        }
        return vo;
    }

    /**
     * 模拟月份推进（加速观察）
     * <p>
     * 每次调用推进 1 个月，根据受托支付+记账+还款数据计算月度评分。
     * 6 个月后达标（累计分≥60）→ 转A类+提额；不达标 → 维持小额或退出。
     */
    @Transactional(rollbackFor = Exception.class)
    public ObservationVO advanceObservation(Long userId) {
        BizCreditLimit bLimit = creditLimitMapper.selectOne(
                new LambdaQueryWrapper<BizCreditLimit>()
                        .eq(BizCreditLimit::getUserId, userId)
                        .eq(BizCreditLimit::getCreditType, TYPE_B));
        if (bLimit == null) {
            throw new BizException(ErrorCode.BIZ_RULE_NOT_MET, "暂无B类授信，需先完成B类预审");
        }
        if (!"OBSERVING".equals(bLimit.getObservationStatus())) {
            throw new BizException(ErrorCode.BIZ_RULE_NOT_MET,
                    "观察期状态为" + observationStatusName(bLimit.getObservationStatus()) + "，不可推进");
        }

        int currentMonth = (bLimit.getObservationMonths() != null ? bLimit.getObservationMonths() : 0) + 1;

        // 月度评分
        int monthScore = calculateMonthScore(userId);
        int totalScore = (bLimit.getObservationScore() != null ? bLimit.getObservationScore() : 0) + monthScore;

        bLimit.setObservationMonths(currentMonth);
        bLimit.setObservationScore(totalScore);
        String detail = "第" + currentMonth + "月评分" + monthScore + "分，累计" + totalScore + "分（阈值60）。";

        if (currentMonth >= 6) {
            // 观察期结束，评估
            if (totalScore >= 60) {
                // 达标 → 转A类+提额
                bLimit.setObservationStatus("PROMOTED");
                detail += "观察期结束，累计评分达标，已转A类循环贷并提额至5万。";

                // 创建/升级 A 类额度
                BizCreditLimit aLimit = creditLimitMapper.selectOne(
                        new LambdaQueryWrapper<BizCreditLimit>()
                                .eq(BizCreditLimit::getUserId, userId)
                                .eq(BizCreditLimit::getCreditType, TYPE_A));
                if (aLimit == null) {
                    aLimit = createCreditLimit(userId, TYPE_A, A_TYPE_TOTAL, A_TYPE_RATE);
                } else {
                    aLimit.setTotalLimit(A_TYPE_TOTAL);
                    aLimit.setAvailableLimit(A_TYPE_TOTAL.subtract(aLimit.getUsedLimit()));
                    aLimit.setStatus("ACTIVE");
                    creditLimitMapper.updateById(aLimit);
                }

                sendInternalMessage(userId, "观察期达标 - 已转A类循环贷",
                        "恭喜！6个月观察期累计评分" + totalScore + "分（≥60），已升级为A类5万循环额度（年化3.85%），支持随借随还。【模拟】",
                        "LOAN", bLimit.getId());
            } else {
                // 不达标 → 退出
                bLimit.setObservationStatus("EXITED");
                detail += "观察期结束，累计评分未达标，维持B类小额授信。";

                sendInternalMessage(userId, "观察期结束 - 维持B类授信",
                        "6个月观察期累计评分" + totalScore + "分（<60），暂未达到A类升级标准，维持B类小额授信。建议持续经营积累信用。【模拟】",
                        "LOAN", bLimit.getId());
            }
        } else {
            detail += "剩余" + (6 - currentMonth) + "个月观察期。";
        }

        creditLimitMapper.updateById(bLimit);
        log.info("[观察期推进] 用户={}, 第{}月, 月度分={}, 累计分={}, 状态={}",
                userId, currentMonth, monthScore, totalScore, bLimit.getObservationStatus());

        ObservationVO vo = getObservationStatus(userId);
        vo.setCurrentMonthDetail(detail);
        return vo;
    }

    /**
     * 计算月度评分（满分100）
     * - 受托支付活跃度（max 30）：每笔+10
     * - 记账收入记录（max 30）：每笔+5
     * - 循环贷还款行为（max 20）：有还款+20
     * - 预算合规（max 20）：基础分20
     */
    private int calculateMonthScore(Long userId) {
        // 受托支付笔数
        long paymentCount = paymentMapper.selectCount(
                new LambdaQueryWrapper<BizEntrustPayment>()
                        .eq(BizEntrustPayment::getUserId, userId)
                        .eq(BizEntrustPayment::getPaymentStatus, PAY_SUCCESS));
        int usageScore = (int) Math.min(paymentCount * 10, 30);

        // 记账收入记录数
        long incomeCount = bookkeepingMapper.selectCount(
                new LambdaQueryWrapper<BizBookkeepingRecord>()
                        .eq(BizBookkeepingRecord::getUserId, userId)
                        .eq(BizBookkeepingRecord::getRecordType, "INCOME"));
        int incomeScore = (int) Math.min(incomeCount * 5, 30);

        // 循环贷还款笔数
        long repayCount = creditTxnMapper.selectCount(
                new LambdaQueryWrapper<BizCreditTxn>()
                        .eq(BizCreditTxn::getUserId, userId)
                        .eq(BizCreditTxn::getTxnType, "REPAY"));
        int repayScore = repayCount > 0 ? 20 : 0;

        // 基础分
        int baseScore = 20;

        return Math.min(usageScore + incomeScore + repayScore + baseScore, 100);
    }

    // ============================================================
    //  商户白名单管理（缺口 #6）
    // ============================================================

    /**
     * 按认证状态筛选商户列表
     */
    public List<BizMerchant> listMerchantsByVerifyStatus(String verifyStatus) {
        LambdaQueryWrapper<BizMerchant> wrapper = new LambdaQueryWrapper<BizMerchant>()
                .eq(BizMerchant::getStatus, 1)
                .orderByAsc(BizMerchant::getId);
        if (verifyStatus != null && !verifyStatus.isEmpty()) {
            wrapper.eq(BizMerchant::getVerifyStatus, verifyStatus);
        }
        return merchantMapper.selectList(wrapper);
    }

    /**
     * banker 审核商户白名单
     *
     * @param merchantId   商户 ID
     * @param verifyStatus VERIFIED-白名单 / REJECTED-拒绝
     */
    @Transactional(rollbackFor = Exception.class)
    public BizMerchant auditMerchant(Long merchantId, String verifyStatus) {
        BizMerchant merchant = merchantMapper.selectById(merchantId);
        if (merchant == null) {
            throw new BizException(ErrorCode.BIZ_RULE_NOT_MET, "商户不存在");
        }
        if (!"VERIFIED".equals(verifyStatus) && !"REJECTED".equals(verifyStatus)) {
            throw new BizException(ErrorCode.PARAM_ERROR, "审核结论只能为 VERIFIED 或 REJECTED");
        }
        merchant.setVerifyStatus(verifyStatus);
        merchantMapper.updateById(merchant);

        log.info("[商户审核] 商户={}, 审核结果={}", merchant.getMerchantName(), verifyStatus);
        return merchant;
    }

    // ============================================================
    //  工具方法
    // ============================================================

    private BizCreditLimit getActiveACreditLimit(Long userId) {
        BizCreditLimit limit = creditLimitMapper.selectOne(
                new LambdaQueryWrapper<BizCreditLimit>()
                        .eq(BizCreditLimit::getUserId, userId)
                        .eq(BizCreditLimit::getCreditType, TYPE_A));
        if (limit == null) {
            // 自动创建 A 类额度
            limit = createCreditLimit(userId, TYPE_A, A_TYPE_TOTAL, A_TYPE_RATE);
        }
        if (!"ACTIVE".equals(limit.getStatus())) {
            throw new BizException(ErrorCode.BIZ_RULE_NOT_MET, "A类额度状态为" + limit.getStatus() + "，不可操作");
        }
        return limit;
    }

    private CreditTxnVO toTxnVO(BizCreditTxn txn) {
        CreditTxnVO vo = new CreditTxnVO();
        BeanUtil.copyProperties(txn, vo);
        vo.setTxnTypeName("WITHDRAW".equals(txn.getTxnType()) ? "提款" : "还款");
        return vo;
    }

    private String observationStatusName(String status) {
        if (status == null) return "未开始";
        return switch (status) {
            case "OBSERVING" -> "观察中";
            case "PROMOTED" -> "已转A类";
            case "EXITED" -> "观察期退出";
            default -> status;
        };
    }

    private void sendInternalMessage(Long userId, String title, String content, String bizType, Long bizId) {
        SysMessage msg = new SysMessage();
        msg.setUserId(userId);
        msg.setTitle(title);
        msg.setContent(content);
        msg.setType("BUSINESS");
        msg.setBizType(bizType);
        msg.setBizId(bizId);
        msg.setIsRead(0);
        messageMapper.insert(msg);
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
