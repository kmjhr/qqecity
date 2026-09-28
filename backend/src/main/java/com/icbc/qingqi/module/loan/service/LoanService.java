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
import com.icbc.qingqi.module.pay.dto.PayOrderVO;
import com.icbc.qingqi.module.pay.service.PayService;
import com.icbc.qingqi.module.pay.service.PaySuccessEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
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
    private final com.icbc.qingqi.module.loan.mapper.BizEntrustReviewMapper entrustReviewMapper;
    private final BizCreditTxnMapper creditTxnMapper;
    private final BizBookkeepingRecordMapper bookkeepingMapper;
    private final SysMessageMapper messageMapper;
    private final PayService payService;

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
                       com.icbc.qingqi.module.loan.mapper.BizEntrustReviewMapper entrustReviewMapper,
                       BizCreditTxnMapper creditTxnMapper,
                       BizBookkeepingRecordMapper bookkeepingMapper,
                       SysMessageMapper messageMapper,
                       PayService payService) {
        this.merchantMapper = merchantMapper;
        this.applicationMapper = applicationMapper;
        this.creditLimitMapper = creditLimitMapper;
        this.paymentMapper = paymentMapper;
        this.entrustReviewMapper = entrustReviewMapper;
        this.creditTxnMapper = creditTxnMapper;
        this.bookkeepingMapper = bookkeepingMapper;
        this.messageMapper = messageMapper;
        this.payService = payService;
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

        // 到期自动失效（对齐《工行杯9.26》P76：预审额度在有效期后自动失效、零负债）
        LocalDate today = LocalDate.now();
        boolean changed = false;
        for (BizCreditLimit l : limits) {
            if ("ACTIVE".equals(l.getStatus()) && l.getExpireDate() != null
                    && l.getExpireDate().isBefore(today) && l.getUsedLimit().signum() == 0) {
                l.setStatus("CLOSED");
                creditLimitMapper.updateById(l);
                changed = true;
            }
        }
        if (changed) {
            limits = creditLimitMapper.selectList(
                    new LambdaQueryWrapper<BizCreditLimit>().eq(BizCreditLimit::getUserId, userId));
        }

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

    /**
     * 青创e贷 A/B 双轨产品规则（模块2 产品介绍）
     * <p>
     * A类：创业信用画像循环贷（5万循环、年化3.85%、随借随还）
     * B类：小额定向两步式授信（免费预审 + 100%受托支付白名单商户）
     * 通用风险揭示与银行信贷产品一致；全部为演示模拟口径。
     */
    public LoanProductRulesVO getProductRules() {
        LoanProductRulesVO vo = new LoanProductRulesVO();

        // ---------------- A 类 ----------------
        LoanProductRulesVO.ProductRule a = new LoanProductRulesVO.ProductRule();
        a.setName("A类 · 创业信用画像循环贷");
        a.setSlogan("有画像即可贷：把真实经营流水变成信用证明，随借随还");
        a.setTarget("已有真实经营画像的青年创业者（经营流水/受托支付/记账数据回流≥3个月、画像经营力达标；在校生、毕业2年内等扶持人群优先）");
        a.setLimitDesc("循环授信额度最高 ¥50,000（按画像评分授信；安居稳定+经营良好可联动提额，最高上浮20%）");
        a.setRateDesc("年化利率 3.85%（单利），随借随还按实际用款天数计息");
        a.setTermDesc("额度有效期 1 年，有效期内循环使用，可申请续贷评估");
        a.setInterestDesc("利息 = 提款本金 × 3.85% ÷ 365 × 实际用款天数（用几天算几天，还款当日不计息）");
        a.setRepayDesc("随借随还：可随时全额/部分还款；还款后额度即时恢复，可再次提款，无提前还款违约金");
        a.setAccessDesc("准入规则：① 人群白名单（在校大学生/毕业2年内/退役军人等扶持人群）② 创业信用画像评分达标 ③ 无重大逾期记录 ④ 通过AI初审+人工复核");
        a.setFundFlowDesc("提款资金进入借款人本人工行账户，仅限合法经营用途（原料采购/摊位租赁/设备购置/线上推广），严禁流入股市、楼市或转借他人");
        a.setRules(List.of(
                "单笔提款最低 ¥1,000，可分次提款，累计不超过循环额度",
                "额度有效期 1 年，到期后未提用部分自动失效，已提用部分继续按日计息",
                "每笔提款/还款均生成流水记录，可在「我的申请-循环贷流水」查询",
                "画像联动：安居稳定 + 经营流水良好可申请提额并享利率优惠（联动演示）",
                "额度为动态管理：经营画像恶化或逾期将触发降额/冻结（模拟）"));
        a.setRisks(List.of(
                "本产品为参赛演示系统，放款、计息、征信影响均为模拟，不发生真实资金往来",
                "逾期将按日收取罚息（年化利率上浮50%），并可能冻结额度、影响后续授信（模拟）",
                "借款资金仅限合法经营用途，挪用将被风控实时监测并冻结额度",
                "循环贷额度不等于固定承诺：以画像评分与风控复核为准，额度可动态调整"));

        // ---------------- B 类 ----------------
        LoanProductRulesVO.ProductRule b = new LoanProductRulesVO.ProductRule();
        b.setName("B类 · 小额定向授信（两步式 + 受托支付）");
        b.setSlogan("零历史也能贷：免费预审 + 100%受托支付，小额定向、专款专用");
        b.setTarget("无历史经营数据的初创青年（城市市集、文创创作、校园服务、本地生活等新业态）");
        b.setLimitDesc("预审额度区间 ¥5,000 ~ ¥20,000（扶持人群上限2万、普通上限1万；创业计划命中经营关键词确定额度下限）");
        b.setRateDesc("年化利率 4.35%（单利），按受托支付金额与实际用款天数计息");
        b.setTermDesc("额度有效期 1 年；未发生真实交易的预审额度到期自动失效，零成本、零负债");
        b.setInterestDesc("利息 = 受托支付金额 × 4.35% ÷ 365 × 实际用款天数");
        b.setRepayDesc("按合同约定期限还款，支持提前还款；资金由银行直接支付商户账户，不经过借款人个人账户");
        b.setAccessDesc("准入规则：① 人群白名单 ② 创业计划通过经营关键词校验 ③ 指定收款商户须为白名单（VERIFIED）商户 ④ 通过AI初审+人工复核");
        b.setFundFlowDesc("100%受托支付：贷款资金由银行直接支付给指定白名单收款商户账户，资金不经过借款人个人账户，从源头保障专款专用");
        b.setRules(List.of(
                "第一步·免费预审：提交创业计划+人群资质 → 准入判断并给出额度区间；不查询征信、不产生硬查询记录、不收费",
                "第二步·正式提款：确认真实交易（物料采购/摊位租赁/设备购置）→ 指定白名单收款商户 → 100%受托支付直付商户账户",
                "预审额度不构成实际放款：未提用无成本、无负债，有效期后自动失效",
                "提款后进入 6 个月观察期：受托支付/收款码/AI记账数据回流 → 经营稳定达标转A类循环贷并提额；数据不足维持小额或退出",
                "提款后经营失败：进入逾期催收流程，先由保险/担保代偿再依法追偿，可申请展期/续贷（模拟）",
                "资金仅限合法经营用途，严禁信用卡资金流入经营领域"));
        b.setRisks(List.of(
                "本产品为参赛演示系统，放款、受托支付、代偿均为模拟，不发生真实资金往来",
                "B类预审不查征信；正式提款申请将依法查询征信（模拟），逾期记录将报送征信系统",
                "受托支付商户须真实经营，虚构交易将被风控识别并追责（模拟）",
                "提款后经营失败可能面临逾期罚息、催收与依法追偿，请合理规划还款来源"));

        // ---------------- 通用风险揭示 ----------------
        vo.setGeneralRisks(List.of(
                "利率与费用：平台展示年化利率均为单利口径，实际利息按实际用款天数计算；除利息外无其他手续费（模拟）",
                "征信提示：B类免费预审不查询征信、不产生硬查询；正式提款申请依法查询征信；逾期记录将报送征信系统，影响后续信贷（模拟）",
                "逾期后果：逾期按合同收取罚息（年化利率上浮50%），并可能面临催收、依法追偿；请按时还款、量入为出",
                "用途限制：贷款资金仅限合法经营用途，严禁流入股市、楼市、虚拟货币或转借他人；违反用途将触发风控冻结",
                "个人信息保护：依据《个人信息保护法》《数据安全法》，您的信息仅用于授信评估，传输加密、脱敏建模、操作留痕，可随时查询与删除授权",
                "模拟声明：本系统为参赛演示系统，全部银行能力为模拟桩，不发生真实资金往来、不产生真实征信影响，请以真实银行产品为准"));

        vo.setProductA(a);
        vo.setProductB(b);
        return vo;
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
        // 归属校验（用户自定义商户仅本人可用）
        if ("USER_CUSTOM".equals(merchant.getMerchantSource())) {
            if (merchant.getApplicantUserId() == null || !merchant.getApplicantUserId().equals(userId)) {
                throw new BizException(ErrorCode.FORBIDDEN,
                        "商户「" + merchant.getMerchantName() + "」为其他用户的自定义商户，不可使用");
            }
        }

        // 支付金额不得超过申请批准额度（用户看到"申请到能用的钱"）
        if (app.getApproveAmount() != null && dto.getAmount().compareTo(app.getApproveAmount()) > 0) {
            throw new BizException(ErrorCode.BIZ_RULE_NOT_MET,
                    "支付金额超过申请批准额度（批准额度：¥" + app.getApproveAmount() + "）");
        }

        // 用户自定义商户：每单复核（两步式）→ 生成复核单，不放款，待管理端 banker 复核通过后执行放款
        if ("USER_CUSTOM".equals(merchant.getMerchantSource())) {
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
            BizEntrustReview review = new BizEntrustReview();
            review.setReviewNo(generateNo("ERR"));
            review.setUserId(userId);
            review.setLoanApplicationId(app.getId());
            review.setMerchantId(merchant.getId());
            review.setMerchantName(merchant.getMerchantName());
            review.setAmount(dto.getAmount());
            review.setPurpose(dto.getPurpose());
            review.setTradeProof(dto.getTradeProof());
            review.setStatus("PENDING");
            entrustReviewMapper.insert(review);

            EntrustPaymentVO vo = new EntrustPaymentVO();
            vo.setId(review.getId());
            vo.setPaymentNo(review.getReviewNo());
            vo.setUserId(userId);
            vo.setLoanApplicationId(app.getId());
            vo.setMerchantId(merchant.getId());
            vo.setMerchantName(merchant.getMerchantName());
            vo.setAmount(dto.getAmount());
            vo.setPurpose(dto.getPurpose());
            vo.setPaymentStatus("PENDING_REVIEW");
            vo.setPaymentStatusName("待银行复核");
            vo.setPendingReview(Boolean.TRUE);
            vo.setReviewNo(review.getReviewNo());
            vo.setReviewStatus("PENDING");
            vo.setReviewStatusName("待复核");
            vo.setFundPath("自定义商户每单复核：复核通过后 100% 直付商户账户（不经过个人账户）【模拟】");
            log.info("[受托支付-每单复核] 用户={}, 商户={}, 金额={}, 复核单={}, 待管理端复核放款【模拟】",
                    userId, merchant.getMerchantName(), dto.getAmount(), review.getReviewNo());
            return vo;
        }

        // 平台通用商户：直接放款（100% 受托支付）
        return doEntrustPay(userId, app, merchant, dto.getAmount(), dto.getPurpose(), dto.getTradeProof());
    }

    /**
     * 执行受托支付放款（平台商户直接调用；自定义商户复核通过后调用）
     * 扣减额度 + 生成 EP 流水 + 商户收款入账（资金不经过个人账户）
     */
    @Transactional(rollbackFor = Exception.class)
    public EntrustPaymentVO doEntrustPay(Long userId, BizLoanApplication app, BizMerchant merchant,
                                         BigDecimal amount, String purpose, String tradeProof) {
        // 扣减额度（按申请类型对应的授信额度）
        BizCreditLimit limit = creditLimitMapper.selectOne(
                new LambdaQueryWrapper<BizCreditLimit>()
                        .eq(BizCreditLimit::getUserId, userId)
                        .eq(BizCreditLimit::getCreditType, app.getLoanType()));
        if (limit == null) {
            throw new BizException(ErrorCode.BIZ_RULE_NOT_MET, "无可用授信额度");
        }
        if (limit.getAvailableLimit().compareTo(amount) < 0) {
            throw new BizException(ErrorCode.BIZ_RULE_NOT_MET,
                    "可用额度不足（可用：" + limit.getAvailableLimit() + "）");
        }

        limit.setUsedLimit(limit.getUsedLimit().add(amount));
        limit.setAvailableLimit(limit.getAvailableLimit().subtract(amount));
        creditLimitMapper.updateById(limit);

        // 生成受托支付流水
        BizEntrustPayment payment = new BizEntrustPayment();
        payment.setPaymentNo(generateNo("EP"));
        payment.setUserId(userId);
        payment.setLoanApplicationId(app.getId());
        payment.setMerchantId(merchant.getId());
        payment.setMerchantName(merchant.getMerchantName());
        payment.setAmount(amount);
        payment.setPurpose(purpose);
        payment.setTradeProof(tradeProof);
        payment.setPaymentStatus(PAY_SUCCESS);
        payment.setPaymentTime(LocalDateTime.now());
        paymentMapper.insert(payment);

        // 支付中台记账：银行资金直付商户账户（不经过个人钱包）
        payService.incomeToMerchant(merchant.getId(), amount, payment.getPaymentNo(),
                PayService.BIZ_ENTRUST_PAY, payment.getPaymentNo(),
                "受托支付（" + merchant.getMerchantName() + "）");

        log.info("[受托支付放款] 用户={}, 商户={}, 金额={}, 授信类型={}, 资金定向打款至商户（不经过个人账户）【模拟】",
                userId, merchant.getMerchantName(), amount, app.getLoanType());

        return toPaymentVO(payment);
    }

    // ============================================================
    //  商户列表 / 申请列表 / 详情
    // ============================================================

    public List<BizMerchant> listMerchants(Long userId) {
        // 用户端只返回白名单商户：平台通用（SYSTEM，全部用户可选）+ 本人自定义（USER_CUSTOM，仅归属本人）
        // 自定义商户其他用户不可见不可用（一对一归属）
        return merchantMapper.selectList(
                new LambdaQueryWrapper<BizMerchant>()
                        .eq(BizMerchant::getStatus, 1)
                        .eq(BizMerchant::getVerifyStatus, "VERIFIED")
                        .and(w -> w.eq(BizMerchant::getMerchantSource, "SYSTEM")
                                .or(o -> o.eq(BizMerchant::getMerchantSource, "USER_CUSTOM")
                                        .eq(BizMerchant::getApplicantUserId, userId))));
    }

    /**
     * 用户提交自定义商户（B类受托支付收款方）
     * 进入 PENDING 灰名单，由管理端 banker 审核通过（VERIFIED）后方可用于受托支付
     */
    @Transactional(rollbackFor = Exception.class)
    public BizMerchant applyMerchant(Long userId, MerchantApplyDTO dto) {
        BizMerchant m = new BizMerchant();
        m.setMerchantName(dto.getMerchantName());
        m.setMerchantType(dto.getMerchantType());
        m.setContactName(dto.getContactName());
        m.setContactPhone(dto.getContactPhone());
        m.setAddress(dto.getAddress());
        m.setBusinessLicense(dto.getBusinessLicense());
        m.setBankAccount(dto.getBankAccount());
        m.setBankName(dto.getBankName());
        m.setApplyRemark(dto.getApplyRemark());
        m.setMerchantSource("USER_CUSTOM");
        m.setApplicantUserId(userId);
        m.setVerifyStatus("PENDING");
        m.setStatus(1);
        merchantMapper.insert(m);
        log.info("[自定义商户申请] 用户={}, 商户={}, ID={}", userId, m.getMerchantName(), m.getId());
        return m;
    }

    /**
     * 我的自定义商户申请列表（含状态：PENDING待审 / VERIFIED已通过 / REJECTED已拒绝）
     */
    /**
     * 打款记录（用户端）：受托支付成功流水（SUCCESS）+ 自定义商户复核单（PENDING_REVIEW / APPROVED / REJECTED）
     */
    public List<EntrustRecordVO> listEntrustRecords(Long userId) {
        List<EntrustRecordVO> list = new ArrayList<>();
        List<BizEntrustPayment> payments = paymentMapper.selectList(
                new LambdaQueryWrapper<BizEntrustPayment>()
                        .eq(BizEntrustPayment::getUserId, userId)
                        .eq(BizEntrustPayment::getPaymentStatus, PAY_SUCCESS)
                        .orderByDesc(BizEntrustPayment::getPaymentTime));
        for (BizEntrustPayment p : payments) {
            EntrustRecordVO vo = new EntrustRecordVO();
            vo.setRecordNo(p.getPaymentNo());
            vo.setRecordType("ENTRUST_PAY");
            vo.setStatus("SUCCESS");
            vo.setStatusName("打款成功");
            vo.setMerchantId(p.getMerchantId());
            vo.setMerchantName(p.getMerchantName());
            BizMerchant m = merchantMapper.selectById(p.getMerchantId());
            vo.setMerchantSource(m != null ? m.getMerchantSource() : "SYSTEM");
            vo.setAmount(p.getAmount());
            vo.setPurpose(p.getPurpose());
            vo.setRecordTime(p.getPaymentTime());
            vo.setFundPath("100%受托支付：银行直付商户账户（不经过个人账户）【模拟】");
            list.add(vo);
        }
        List<BizEntrustReview> reviews = entrustReviewMapper.selectList(
                new LambdaQueryWrapper<BizEntrustReview>()
                        .eq(BizEntrustReview::getUserId, userId)
                        .orderByDesc(BizEntrustReview::getId));
        for (BizEntrustReview r : reviews) {
            EntrustRecordVO vo = new EntrustRecordVO();
            vo.setRecordNo(r.getReviewNo());
            vo.setRecordType("ENTRUST_REVIEW");
            vo.setMerchantId(r.getMerchantId());
            vo.setMerchantName(r.getMerchantName());
            vo.setMerchantSource("USER_CUSTOM");
            vo.setAmount(r.getAmount());
            vo.setPurpose(r.getPurpose());
            vo.setReviewRemark(r.getReviewRemark());
            if ("PENDING".equals(r.getStatus())) {
                vo.setStatus("PENDING_REVIEW");
                vo.setStatusName("待银行复核");
            } else if ("APPROVED".equals(r.getStatus())) {
                vo.setStatus("SUCCESS");
                vo.setStatusName("已复核放款");
            } else {
                vo.setStatus("REJECTED");
                vo.setStatusName("已驳回");
            }
            vo.setRecordTime(r.getCreateTime());
            vo.setFundPath("自定义商户每单复核：复核通过后 100% 直付商户账户【模拟】");
            list.add(vo);
        }
        list.sort((a, b) -> {
            java.time.LocalDateTime ta = a.getRecordTime() == null ? java.time.LocalDateTime.MIN : a.getRecordTime();
            java.time.LocalDateTime tb = b.getRecordTime() == null ? java.time.LocalDateTime.MIN : b.getRecordTime();
            return tb.compareTo(ta);
        });
        return list;
    }

    /**
     * 管理端：受托支付复核单列表（自定义商户每单复核）
     */
    public List<BizEntrustReview> listEntrustReviews(String status) {
        return entrustReviewMapper.selectList(
                new LambdaQueryWrapper<BizEntrustReview>()
                        .eq(status != null && !status.isBlank(), BizEntrustReview::getStatus, status)
                        .orderByAsc(BizEntrustReview::getStatus)
                        .orderByDesc(BizEntrustReview::getId));
    }

    /**
     * 管理端：受托支付复核（每单复核）——通过后执行放款；驳回不打款、额度不动
     */
    @Transactional(rollbackFor = Exception.class)
    public BizEntrustReview auditEntrustReview(Long id, boolean approve, String reason, Long reviewerId) {
        BizEntrustReview review = entrustReviewMapper.selectById(id);
        if (review == null) {
            throw new BizException(ErrorCode.BIZ_RULE_NOT_MET, "复核单不存在");
        }
        if (!"PENDING".equals(review.getStatus())) {
            throw new BizException(ErrorCode.BIZ_RULE_NOT_MET, "该复核单已处理（当前状态：" + review.getStatus() + "）");
        }
        review.setReviewerId(reviewerId);
        review.setReviewRemark(reason);
        review.setReviewTime(LocalDateTime.now());
        if (!approve) {
            review.setStatus("REJECTED");
            entrustReviewMapper.updateById(review);
            log.info("[受托支付复核-驳回] 复核单={}, 驳回原因={}, 复核人={}", review.getReviewNo(), reason, reviewerId);
            return review;
        }
        // 通过 → 执行放款
        BizLoanApplication app = applicationMapper.selectById(review.getLoanApplicationId());
        BizMerchant merchant = merchantMapper.selectById(review.getMerchantId());
        if (app == null || merchant == null || !"USER_CUSTOM".equals(merchant.getMerchantSource())) {
            throw new BizException(ErrorCode.BIZ_RULE_NOT_MET, "申请或商户不存在/已变更");
        }
        EntrustPaymentVO vo = doEntrustPay(review.getUserId(), app, merchant, review.getAmount(),
                review.getPurpose(), review.getTradeProof());
        review.setStatus("APPROVED");
        review.setPaymentId(vo.getId());
        entrustReviewMapper.updateById(review);
        log.info("[受托支付复核-通过并放款] 复核单={}, EP流水={}, 金额={}, 复核人={}",
                review.getReviewNo(), vo.getPaymentNo(), review.getAmount(), reviewerId);
        return review;
    }

    public List<BizMerchant> myMerchantApplications(Long userId) {
        return merchantMapper.selectList(
                new LambdaQueryWrapper<BizMerchant>()
                        .eq(BizMerchant::getApplicantUserId, userId)
                        .orderByDesc(BizMerchant::getId));
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

        // 放款（模拟）：资金直接到账，不经平台钱包（演示口径）
        log.info("[A类提款] 用户={}, 金额={}, 可用余额={}（放款成功，模拟到账）", userId, dto.getAmount(), limit.getAvailableLimit());
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

        // 钱包扣款（还本付息，模拟）：余额不足提示充值
        payService.payFromWallet(userId, dto.getAmount(), "青创e贷A类还款（模拟）", null);

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

        // 恢复额度：按本金归还恢复；利息计入应还（全额结清 = 本金+利息）
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
     * 两步式还款（扫码支付）：创建还款支付订单（LOAN_REPAY），支付成功后由 onLoanRepayPaid 执行还本付息
     * <p>
     * 金额口径（amountType）：PRINCIPAL=本金（默认，利息按笔自动结算）；TOTAL=本息合计（输入含息金额，自动拆分本金+利息）
     */
    @Transactional(rollbackFor = Exception.class)
    public PayOrderVO createRepayOrder(Long userId, String creditType, RepayDTO dto) {
        boolean aType = TYPE_A.equals(creditType);
        BizCreditLimit limit = aType ? getActiveACreditLimit(userId) : getActiveBCreditLimit(userId);
        if (limit.getUsedLimit() == null || limit.getUsedLimit().compareTo(BigDecimal.ZERO) <= 0) {
            throw new BizException(ErrorCode.BIZ_RULE_NOT_MET, aType ? "当前无待还本金" : "当前无 B类待还本金");
        }
        BigDecimal rate = aType ? A_TYPE_RATE : B_TYPE_RATE;
        boolean totalMode = dto.getAmountType() != null && "TOTAL".equalsIgnoreCase(dto.getAmountType());
        String remark;
        String subject;
        BigDecimal orderAmount;

        if (dto.getLoanNo() != null && !dto.getLoanNo().isBlank()) {
            // 结清指定借款（按笔）：金额必须等于该笔剩余本金（PRINCIPAL）或本息合计（TOTAL），利息按该笔天数结算
            List<LoanItemVO> loans = buildLoanItems(userId, creditType, rate, limit);
            LoanItemVO target = loans.stream()
                    .filter(l -> dto.getLoanNo().equals(l.getLoanNo()))
                    .findFirst()
                    .orElseThrow(() -> new BizException(ErrorCode.BIZ_RULE_NOT_MET,
                            "未找到该笔借款（可能已结清）"));
            if (totalMode) {
                BigDecimal totalDue = target.getRemainingPrincipal().add(target.getInterestPreview());
                if (dto.getAmount().compareTo(totalDue) != 0) {
                    throw new BizException(ErrorCode.BIZ_RULE_NOT_MET,
                            "结清借款「" + target.getLoanNo() + "」需支付本息合计 ¥" + totalDue
                                    + "（本金 ¥" + target.getRemainingPrincipal() + "＋利息 ¥" + target.getInterestPreview() + "）");
                }
                orderAmount = totalDue;
                remark = "TOTAL#LOAN#" + target.getLoanNo();
                subject = (aType ? "青创e贷A类还款" : "青创e贷B类还款") + "（结清借款" + target.getLoanNo() + "，本息合计）";
            } else {
                if (dto.getAmount().compareTo(target.getRemainingPrincipal()) != 0) {
                    throw new BizException(ErrorCode.BIZ_RULE_NOT_MET,
                            "结清借款「" + target.getLoanNo() + "」需还款本金 ¥" + target.getRemainingPrincipal());
                }
                orderAmount = target.getRemainingPrincipal();
                remark = "LOAN#" + target.getLoanNo();
                subject = (aType ? "青创e贷A类还款" : "青创e贷B类还款") + "（结清借款" + target.getLoanNo() + "）";
            }
        } else if (totalMode) {
            // 本息合计口径：FIFO 拆分为本金+利息，订单金额=实际本息合计（用户输入零头自动吸收）
            List<LoanItemVO> loans = buildLoanItems(userId, creditType, rate, limit);
            BigDecimal totalDueAll = loans.stream()
                    .map(LoanItemVO::getTotalDue)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            if (dto.getAmount().compareTo(totalDueAll) > 0) {
                throw new BizException(ErrorCode.BIZ_RULE_NOT_MET,
                        "还款金额不能超过待还本息合计（待还本息：¥" + totalDueAll + "）");
            }
            BigDecimal[] split = splitRepay(dto.getAmount(), loans, rate);
            if (split[0].compareTo(BigDecimal.ZERO) <= 0) {
                throw new BizException(ErrorCode.BIZ_RULE_NOT_MET, "金额过小，至少需还本金 ¥0.01");
            }
            orderAmount = split[0].add(split[1]).setScale(2, RoundingMode.HALF_UP);
            remark = "TOTAL#FIFO";
            subject = aType ? "青创e贷A类还款（本息合计·扫码支付）" : "青创e贷B类还款（本息合计·扫码支付）";
        } else {
            if (dto.getAmount().compareTo(limit.getUsedLimit()) > 0) {
                throw new BizException(ErrorCode.BIZ_RULE_NOT_MET,
                        "还款金额不能超过待还本金（待还：¥" + limit.getUsedLimit() + "）");
            }
            orderAmount = dto.getAmount();
            remark = null;
            subject = aType ? "青创e贷A类还款（扫码支付）" : "青创e贷B类还款（扫码支付）";
        }
        log.info("[创建还款订单] 用户={}, 类型={}, 金额={}, 借款={}, 口径={}", userId, creditType, orderAmount, remark,
                totalMode ? "TOTAL" : "PRINCIPAL");
        return payService.createBizOrder(userId, "LOAN_REPAY", limit.getId(), subject, orderAmount, null, remark);
    }

    /**
     * 本息合计拆分（FIFO，利随本清）：给定含息总金额，按时间序逐笔冲抵
     * 每笔结清需本息=剩余本金+该笔利息；金额不足以结清某笔时，按 本金=金额/(1+日利率×天数) 拆出部分本金
     *
     * @return [本金, 利息]
     */
    private BigDecimal[] splitRepay(BigDecimal totalAmount, List<LoanItemVO> loans, BigDecimal rate) {
        BigDecimal remain = totalAmount.setScale(2, RoundingMode.HALF_UP);
        BigDecimal principal = BigDecimal.ZERO;
        BigDecimal interest = BigDecimal.ZERO;
        BigDecimal daily = rate.divide(new BigDecimal("365"), 10, RoundingMode.HALF_UP);
        for (LoanItemVO l : loans) {
            if (remain.compareTo(BigDecimal.ZERO) <= 0) break;
            BigDecimal settle = l.getTotalDue();
            if (remain.compareTo(settle) >= 0) {
                principal = principal.add(l.getRemainingPrincipal());
                interest = interest.add(l.getInterestPreview());
                remain = remain.subtract(settle);
            } else {
                BigDecimal factor = BigDecimal.ONE.add(daily.multiply(new BigDecimal(l.getBorrowDays())));
                BigDecimal x = remain.divide(factor, 2, RoundingMode.HALF_UP);
                if (x.compareTo(new BigDecimal("0.01")) >= 0) {
                    BigDecimal li = x.multiply(rate).multiply(new BigDecimal(l.getBorrowDays()))
                            .divide(new BigDecimal("365"), 2, RoundingMode.HALF_UP);
                    principal = principal.add(x);
                    interest = interest.add(li);
                }
                break; // 剩余零头忽略（金额口径以实际本息为准）
            }
        }
        return new BigDecimal[]{principal, interest};
    }

    /**
     * 还款订单支付成功回调：执行还本付息（A/B 双轨，模拟）
     */
    @EventListener
    @Transactional(rollbackFor = Exception.class)
    public void onLoanRepayPaid(PaySuccessEvent event) {
        if (!"LOAN_REPAY".equals(event.getBizType())) return;
        Long limitId = event.getBizId();
        if (limitId == null) return;
        BizCreditLimit limit = creditLimitMapper.selectById(limitId);
        if (limit == null || !Objects.equals(event.getUserId(), limit.getUserId())) return;
        boolean aType = TYPE_A.equals(limit.getCreditType());
        BigDecimal rate = aType ? A_TYPE_RATE : B_TYPE_RATE;

        // 按笔计息（利随本清）：指定借款结清 或 FIFO 先进先出冲抵，被冲抵本金按所在借款的天数结息
        BigDecimal repayPrincipal = BigDecimal.ZERO;
        BigDecimal interest = BigDecimal.ZERO;
        int maxDays = 0;
        String detail;
        String remark = event.getRemark();
        List<LoanItemVO> loans = buildLoanItems(event.getUserId(), limit.getCreditType(), rate, limit);

        if (remark != null && remark.startsWith("TOTAL#LOAN#")) {
            // 本息合计口径 · 结清指定借款：金额=该笔剩余本金+该笔利息
            String loanNo = remark.substring("TOTAL#LOAN#".length());
            LoanItemVO t = loans.stream().filter(l -> loanNo.equals(l.getLoanNo())).findFirst().orElse(null);
            if (t == null) {
                log.warn("[还款回调] 指定借款不存在或已结清 loanNo={}", loanNo);
                return;
            }
            repayPrincipal = t.getRemainingPrincipal();
            interest = t.getInterestPreview();
            maxDays = t.getBorrowDays();
            detail = "结清借款" + t.getLoanNo() + "（本息合计）：本金¥" + repayPrincipal + "，利息¥" + interest
                    + "（" + t.getBorrowDays() + "天，年化" + (aType ? "3.85" : "4.35") + "%按笔模拟）";
        } else if (remark != null && remark.startsWith("LOAN#")) {
            String loanNo = remark.substring("LOAN#".length());
            LoanItemVO t = loans.stream().filter(l -> loanNo.equals(l.getLoanNo())).findFirst().orElse(null);
            if (t == null) {
                log.warn("[还款回调] 指定借款不存在或已结清 loanNo={}", loanNo);
                return;
            }
            repayPrincipal = t.getRemainingPrincipal();
            interest = t.getInterestPreview();
            maxDays = t.getBorrowDays();
            detail = "结清借款" + t.getLoanNo() + "：本金¥" + repayPrincipal + "，利息¥" + interest
                    + "（" + t.getBorrowDays() + "天，年化" + (aType ? "3.85" : "4.35") + "%按笔模拟）";
        } else if (remark != null && remark.startsWith("TOTAL#FIFO")) {
            // 本息合计口径 · 先进先出：金额=本息合计，按 FIFO 拆分本金+利息
            BigDecimal[] split = splitRepay(event.getAmount(), loans, rate);
            repayPrincipal = split[0];
            interest = split[1];
            maxDays = loans.isEmpty() ? 0 : loans.get(0).getBorrowDays();
            detail = "先进先出冲抵（本息合计 ¥" + event.getAmount() + "）：本金¥" + repayPrincipal
                    + "，利息¥" + interest + "（按笔计息，模拟）";
        } else {
            // 本金口径 · 先进先出：金额=本金，冲抵本金后按所在借款天数结息
            BigDecimal remain = event.getAmount();
            List<String> parts = new ArrayList<>();
            for (LoanItemVO l : loans) {
                if (remain.compareTo(BigDecimal.ZERO) <= 0) break;
                BigDecimal take = l.getRemainingPrincipal().min(remain);
                if (take.compareTo(BigDecimal.ZERO) <= 0) continue;
                BigDecimal loanInterest = take.multiply(rate).multiply(new BigDecimal(l.getBorrowDays()))
                        .divide(new BigDecimal("365"), 2, RoundingMode.HALF_UP);
                repayPrincipal = repayPrincipal.add(take);
                interest = interest.add(loanInterest);
                maxDays = Math.max(maxDays, l.getBorrowDays());
                parts.add(l.getLoanNo() + "冲" + take.stripTrailingZeros().toPlainString()
                        + "（息" + loanInterest.stripTrailingZeros().toPlainString() + "）");
                remain = remain.subtract(take);
            }
            detail = "先进先出冲抵：" + String.join("；", parts) + "（按笔计息，模拟）";
        }

        limit.setUsedLimit(limit.getUsedLimit().subtract(repayPrincipal));
        limit.setAvailableLimit(limit.getAvailableLimit().add(repayPrincipal));
        creditLimitMapper.updateById(limit);

        BizCreditTxn txn = new BizCreditTxn();
        txn.setTxnNo(generateNo("CR"));
        txn.setUserId(event.getUserId());
        txn.setCreditLimitId(limit.getId());
        txn.setTxnType("REPAY");
        txn.setPrincipalAmount(repayPrincipal);
        txn.setInterestAmount(interest);
        txn.setBorrowDays(maxDays);
        if (remark != null && remark.startsWith("LOAN#")) {
            txn.setTargetLoanNo(remark.substring("LOAN#".length()));
        } else if (remark != null && remark.startsWith("TOTAL#LOAN#")) {
            txn.setTargetLoanNo(remark.substring("TOTAL#LOAN#".length()));
        }
        txn.setBalanceAfter(limit.getAvailableLimit());
        txn.setRemark((aType ? "A类" : "B类") + "扫码还款（订单" + event.getOrderNo() + "）：" + detail);
        txn.setTxnTime(LocalDateTime.now());
        creditTxnMapper.insert(txn);

        log.info("[扫码还款成功] 订单={}, 用户={}, 类型={}, 本金={}, 利息={}",
                event.getOrderNo(), event.getUserId(), limit.getCreditType(), repayPrincipal, interest);
    }

    /**
     * 还款试算预览（A/B 双轨，按笔计息）：待还本金、利息、应还合计 + 每笔借款明细
     */
    public RepayPreviewVO repayPreview(Long userId, String creditType) {
        BigDecimal rate = TYPE_A.equals(creditType) ? A_TYPE_RATE : B_TYPE_RATE;
        BizCreditLimit limit = TYPE_A.equals(creditType) ? getActiveACreditLimit(userId) : getActiveBCreditLimit(userId);
        List<LoanItemVO> loans = buildLoanItems(userId, creditType, rate, limit);
        BigDecimal used = limit.getUsedLimit() == null ? BigDecimal.ZERO : limit.getUsedLimit();
        BigDecimal interestTotal = loans.stream().map(LoanItemVO::getInterestPreview)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        int maxDays = loans.stream().mapToInt(LoanItemVO::getBorrowDays).max().orElse(0);
        RepayPreviewVO vo = new RepayPreviewVO();
        vo.setCreditType(creditType);
        vo.setCreditTypeName(TYPE_A.equals(creditType) ? "A类循环额度" : "B类定向额度");
        vo.setUsedLimit(used);
        vo.setRate(rate);
        vo.setRateText(rate.multiply(new BigDecimal("100")).setScale(2, RoundingMode.HALF_UP).stripTrailingZeros().toPlainString() + "%");
        vo.setEarliestDate(loans.isEmpty() ? null : loans.get(0).getLoanDate());
        vo.setBorrowDays(maxDays);
        vo.setInterestPreview(interestTotal);
        vo.setTotalDue(used.add(interestTotal));
        vo.setPrincipalTotal(used);
        vo.setInterestTotal(interestTotal);
        vo.setLoans(loans);
        vo.setRemark(TYPE_A.equals(creditType)
                ? "按笔计息（随借随还）：每笔提款独立起息，利息 = 剩余本金 × 3.85% ÷ 365 × 借款天数；还款按先进先出冲抵本金，被冲抵本金对应利息随还款一并结清（利随本清，无违约金，模拟）"
                : "按笔计息（定向贷）：每笔受托支付独立起息，利息 = 剩余本金 × 4.35% ÷ 365 × 自放款日起天数；还款按先进先出冲抵本金，被冲抵本金对应利息随还款一并结清（利随本清，无违约金，模拟）");
        return vo;
    }

    /**
     * 构建未结清借款明细（按笔计息，FIFO 冲抵，实时重算）：
     * A 类 = 每笔提款流水；B 类 = 每笔受托支付成功；还款流水按时间先进先出冲抵本金
     */
    private List<LoanItemVO> buildLoanItems(Long userId, String creditType, BigDecimal rate, BizCreditLimit limit) {
        List<String> loanNos = new ArrayList<>();
        List<LocalDate> loanDates = new ArrayList<>();
        List<BigDecimal> principals = new ArrayList<>();
        List<BigDecimal> remaining = new ArrayList<>();

        if (TYPE_A.equals(creditType)) {
            List<BizCreditTxn> ws = creditTxnMapper.selectList(new LambdaQueryWrapper<BizCreditTxn>()
                    .eq(BizCreditTxn::getCreditLimitId, limit.getId())
                    .eq(BizCreditTxn::getTxnType, "WITHDRAW")
                    .orderByAsc(BizCreditTxn::getTxnTime)
                    .orderByAsc(BizCreditTxn::getId));
            for (BizCreditTxn w : ws) {
                loanNos.add(w.getTxnNo());
                loanDates.add(w.getTxnTime() == null ? LocalDate.now() : w.getTxnTime().toLocalDate());
                principals.add(w.getPrincipalAmount());
                remaining.add(w.getPrincipalAmount());
            }
        } else {
            List<BizEntrustPayment> ps = paymentMapper.selectList(new LambdaQueryWrapper<BizEntrustPayment>()
                    .eq(BizEntrustPayment::getUserId, userId)
                    .eq(BizEntrustPayment::getPaymentStatus, PAY_SUCCESS)
                    .orderByAsc(BizEntrustPayment::getPaymentTime)
                    .orderByAsc(BizEntrustPayment::getId));
            for (BizEntrustPayment p : ps) {
                loanNos.add(p.getPaymentNo());
                loanDates.add(p.getPaymentTime() == null ? LocalDate.now() : p.getPaymentTime().toLocalDate());
                principals.add(p.getAmount());
                remaining.add(p.getAmount());
            }
        }
        // 还款冲抵：① 指定结清某笔（target_loan_no）→ 精确冲抵该笔借款（不参与 FIFO，不漂移）
        List<BizCreditTxn> repays = creditTxnMapper.selectList(new LambdaQueryWrapper<BizCreditTxn>()
                .eq(BizCreditTxn::getCreditLimitId, limit.getId())
                .eq(BizCreditTxn::getTxnType, "REPAY")
                .orderByAsc(BizCreditTxn::getTxnTime)
                .orderByAsc(BizCreditTxn::getId));
        for (BizCreditTxn r : repays) {
            String target = r.getTargetLoanNo();
            if (target == null || target.isBlank()) continue;
            int idx = loanNos.indexOf(target);
            if (idx >= 0) {
                BigDecimal take = remaining.get(idx).min(r.getPrincipalAmount());
                remaining.set(idx, remaining.get(idx).subtract(take));
            }
        }
        // ② 剩余普通还款 FIFO 冲抵（每笔还款先冲最早的借款）
        for (BizCreditTxn r : repays) {
            if (r.getTargetLoanNo() != null && !r.getTargetLoanNo().isBlank()) continue;
            BigDecimal remain = r.getPrincipalAmount();
            for (int i = 0; i < remaining.size() && remain.compareTo(BigDecimal.ZERO) > 0; i++) {
                BigDecimal take = remaining.get(i).min(remain);
                remaining.set(i, remaining.get(i).subtract(take));
                remain = remain.subtract(take);
            }
        }

        List<LoanItemVO> loans = new ArrayList<>();
        for (int i = 0; i < loanNos.size(); i++) {
            if (remaining.get(i).compareTo(BigDecimal.ZERO) <= 0) continue;
            int days = (int) ChronoUnit.DAYS.between(loanDates.get(i), LocalDate.now());
            if (days < 1) days = 1;
            BigDecimal rem = remaining.get(i);
            BigDecimal interest = rem.multiply(rate).multiply(new BigDecimal(days))
                    .divide(new BigDecimal("365"), 2, RoundingMode.HALF_UP);
            BigDecimal dailyPct = rate.divide(new BigDecimal("365"), 8, RoundingMode.HALF_UP)
                    .multiply(new BigDecimal("100")).setScale(4, RoundingMode.HALF_UP).stripTrailingZeros();
            LoanItemVO vo = new LoanItemVO();
            vo.setLoanNo(loanNos.get(i));
            vo.setLoanDate(loanDates.get(i));
            vo.setPrincipal(principals.get(i));
            vo.setRepaidPrincipal(principals.get(i).subtract(rem));
            vo.setRemainingPrincipal(rem);
            vo.setBorrowDays(days);
            vo.setRate(rate);
            vo.setDailyRateText(dailyPct.toPlainString() + "%/日");
            vo.setInterestPreview(interest);
            vo.setTotalDue(rem.add(interest));
            loans.add(vo);
        }
        return loans;
    }

    /**
     * B类定向额度还款：归还受托支付本金 + 按日计息（年化4.35%模拟）
     */
    public CreditTxnVO entrustRepay(Long userId, RepayDTO dto) {
        BizCreditLimit limit = getActiveBCreditLimit(userId);
        if (limit.getUsedLimit().compareTo(BigDecimal.ZERO) <= 0) {
            throw new BizException(ErrorCode.BIZ_RULE_NOT_MET, "当前无 B类待还本金");
        }

        // 钱包扣款（还本付息，模拟）：余额不足提示充值
        payService.payFromWallet(userId, dto.getAmount(), "青创e贷B类还款（模拟）", null);

        // 最早一笔成功受托支付日起算计息天数
        BizEntrustPayment earliest = paymentMapper.selectOne(
                new LambdaQueryWrapper<BizEntrustPayment>()
                        .eq(BizEntrustPayment::getUserId, userId)
                        .eq(BizEntrustPayment::getPaymentStatus, PAY_SUCCESS)
                        .orderByAsc(BizEntrustPayment::getPaymentTime)
                        .last("LIMIT 1"));
        int borrowDays = 1;
        if (earliest != null && earliest.getPaymentTime() != null) {
            borrowDays = (int) ChronoUnit.DAYS.between(earliest.getPaymentTime().toLocalDate(), LocalDate.now());
            if (borrowDays < 1) borrowDays = 1;
        }
        BigDecimal repayPrincipal = dto.getAmount().min(limit.getUsedLimit());
        BigDecimal interest = repayPrincipal.multiply(B_TYPE_RATE).multiply(new BigDecimal(borrowDays))
                .divide(new BigDecimal("365"), 2, RoundingMode.HALF_UP);
        limit.setUsedLimit(limit.getUsedLimit().subtract(repayPrincipal));
        limit.setAvailableLimit(limit.getAvailableLimit().add(repayPrincipal));
        creditLimitMapper.updateById(limit);

        BizCreditTxn txn = new BizCreditTxn();
        txn.setTxnNo(generateNo("CR"));
        txn.setUserId(userId);
        txn.setCreditLimitId(limit.getId());
        txn.setTxnType("REPAY");
        txn.setPrincipalAmount(repayPrincipal);
        txn.setInterestAmount(interest);
        txn.setBorrowDays(borrowDays);
        txn.setBalanceAfter(limit.getAvailableLimit());
        txn.setRemark("B类受托支付还款，利息¥" + interest + "（" + borrowDays + "天，年化4.35%模拟）");
        txn.setTxnTime(LocalDateTime.now());
        creditTxnMapper.insert(txn);

        log.info("[B类还款] 用户={}, 本金={}, 利息={}, 天数={}, 剩余待还={}",
                userId, repayPrincipal, interest, borrowDays, limit.getUsedLimit());
        return toTxnVO(txn);
    }

    private BizCreditLimit getActiveBCreditLimit(Long userId) {
        BizCreditLimit limit = creditLimitMapper.selectOne(
                new LambdaQueryWrapper<BizCreditLimit>()
                        .eq(BizCreditLimit::getUserId, userId)
                        .eq(BizCreditLimit::getCreditType, TYPE_B));
        if (limit == null) {
            BizLoanApplication last = applicationMapper.selectOne(
                    new LambdaQueryWrapper<BizLoanApplication>()
                            .eq(BizLoanApplication::getUserId, userId)
                            .eq(BizLoanApplication::getLoanType, TYPE_B)
                            .eq(BizLoanApplication::getPreCheckResult, PRE_ELIGIBLE)
                            .orderByDesc(BizLoanApplication::getCreateTime)
                            .last("LIMIT 1"));
            BigDecimal bTotal = (last != null && last.getPreCheckMaxAmount() != null)
                    ? last.getPreCheckMaxAmount() : LIMIT_MAX_NORMAL;
            limit = createCreditLimit(userId, TYPE_B, bTotal, B_TYPE_RATE);
        }
        if (!"ACTIVE".equals(limit.getStatus())) {
            throw new BizException(ErrorCode.BIZ_RULE_NOT_MET, "B类额度状态为" + limit.getStatus() + "，不可操作");
        }
        return limit;
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
    public BizMerchant auditMerchant(Long merchantId, String verifyStatus, String reason, Long reviewerId) {
        BizMerchant merchant = merchantMapper.selectById(merchantId);
        if (merchant == null) {
            throw new BizException(ErrorCode.BIZ_RULE_NOT_MET, "商户不存在");
        }
        if (!"PENDING".equals(merchant.getVerifyStatus())) {
            throw new BizException(ErrorCode.BIZ_RULE_NOT_MET, "该商户不在待审队列（当前状态：" + merchant.getVerifyStatus() + "）");
        }
        if (!"VERIFIED".equals(verifyStatus) && !"REJECTED".equals(verifyStatus)) {
            throw new BizException(ErrorCode.PARAM_ERROR, "审核结论只能为 VERIFIED 或 REJECTED");
        }
        if ("REJECTED".equals(verifyStatus) && (reason == null || reason.isBlank())) {
            throw new BizException(ErrorCode.PARAM_ERROR, "拒绝时必须填写驳回原因");
        }
        merchant.setVerifyStatus(verifyStatus);
        merchant.setReviewRemark(reason);
        merchant.setReviewerId(reviewerId);
        merchant.setReviewTime(LocalDateTime.now());
        merchantMapper.updateById(merchant);

        log.info("[商户审核] 商户={}, 审核结果={}, 原因={}, 审核人={}",
                merchant.getMerchantName(), verifyStatus, reason, reviewerId);
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
