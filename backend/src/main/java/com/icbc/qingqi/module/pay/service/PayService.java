package com.icbc.qingqi.module.pay.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.icbc.qingqi.common.BizException;
import com.icbc.qingqi.common.ErrorCode;
import com.icbc.qingqi.module.budget.entity.BizTransaction;
import com.icbc.qingqi.module.budget.mapper.BizTransactionMapper;
import com.icbc.qingqi.module.loan.entity.BizMerchant;
import com.icbc.qingqi.module.loan.mapper.BizMerchantMapper;
import com.icbc.qingqi.module.pay.dto.*;
import com.icbc.qingqi.module.pay.entity.*;
import com.icbc.qingqi.module.pay.mapper.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 模拟支付中台服务（模块0 设计书）
 * <p>
 * 覆盖：钱包（开通/充值/支付密码）、订单（模拟消费下单/收银台支付/关闭/退款）、
 * 流水、商户收款账户、业务资金接入（提款入账/还款扣款/受托支付商户入账/保函费订单）。
 * 全部为模拟桩，标注"模拟"，不产生真实资金。
 */
@Slf4j
@Service
public class PayService {

    public static final String BIZ_GUARANTEE_FEE = "GUARANTEE_FEE";
    public static final String BIZ_LOAN_REPAY = "LOAN_REPAY";
    public static final String BIZ_ENTRUST_PAY = "ENTRUST_PAY";
    public static final String BIZ_MERCHANT_CONSUME = "MERCHANT_CONSUME";
    public static final String BIZ_RECHARGE = "RECHARGE";

    public static final String ORDER_PENDING_PAY = "PENDING_PAY";
    public static final String ORDER_PAID = "PAID";
    public static final String ORDER_CLOSED = "CLOSED";
    public static final String ORDER_REFUNDED = "REFUNDED";

    private static final String DEFAULT_PAY_PASSWORD = "123456";
    private static final BigDecimal DAYS = new BigDecimal("365");

    /** 反诈高危词（支付前风控，模拟） */
    private static final List<String> FRAUD_KEYWORDS = List.of("垫资", "保证金", "解冻", "卡单", "刷单", "先垫", "激活", "手续费提前");

    private final PayWalletMapper walletMapper;
    private final PayMerchantAccountMapper merchantAccountMapper;
    private final PayOrderMapper orderMapper;
    private final PayTransactionMapper txnMapper;
    private final BizMerchantMapper merchantMapper;
    private final BizTransactionMapper bizTransactionMapper;
    private final ApplicationEventPublisher eventPublisher;

    public PayService(PayWalletMapper walletMapper,
                      PayMerchantAccountMapper merchantAccountMapper,
                      PayOrderMapper orderMapper,
                      PayTransactionMapper txnMapper,
                      BizMerchantMapper merchantMapper,
                      BizTransactionMapper bizTransactionMapper,
                      ApplicationEventPublisher eventPublisher) {
        this.walletMapper = walletMapper;
        this.merchantAccountMapper = merchantAccountMapper;
        this.orderMapper = orderMapper;
        this.txnMapper = txnMapper;
        this.merchantMapper = merchantMapper;
        this.bizTransactionMapper = bizTransactionMapper;
        this.eventPublisher = eventPublisher;
    }

    // ============================================================
    //  钱包
    // ============================================================

    /** 获取（自动开通）钱包 */
    @Transactional(rollbackFor = Exception.class)
    public WalletVO getWallet(Long userId) {
        PayWallet wallet = findWallet(userId);
        if (wallet == null) {
            wallet = new PayWallet();
            wallet.setUserId(userId);
            wallet.setBalance(BigDecimal.ZERO);
            wallet.setFrozen(BigDecimal.ZERO);
            wallet.setPayPassword(DEFAULT_PAY_PASSWORD);
            wallet.setStatus("ACTIVE");
            walletMapper.insert(wallet);
            log.info("[开通钱包] 用户={}（模拟）", userId);
        }
        return toWalletVO(wallet);
    }

    /** 模拟充值（银行卡/工行e支付） */
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> recharge(Long userId, RechargeDTO dto) {
        String method = dto.getMethod() != null ? dto.getMethod() : "CARD";
        if (!List.of("CARD", "SIM_BANK").contains(method)) {
            throw new BizException(ErrorCode.PARAM_ERROR, "不支持的充值方式");
        }
        PayWallet wallet = findWalletOrCreate(userId);
        BigDecimal newBalance = wallet.getBalance().add(dto.getAmount());
        wallet.setBalance(newBalance);
        walletMapper.updateById(wallet);

        PayOrder order = buildOrder(userId, BIZ_RECHARGE, null, null,
                "钱包充值（模拟，" + methodName(method) + "）", dto.getAmount(), null);
        order.setStatus(ORDER_PAID);
        order.setPayMethod(method);
        order.setPayTime(LocalDateTime.now());
        orderMapper.insert(order);

        insertTxn(userId, null, order, "IN", dto.getAmount(), newBalance, method,
                "钱包充值（模拟，" + methodName(method) + "）", null);

        log.info("[充值] 用户={}, 金额={}, 方式={}（模拟）", userId, dto.getAmount(), method);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("wallet", toWalletVO(wallet));
        result.put("orderNo", order.getOrderNo());
        result.put("remark", "充值成功（模拟，不涉及真实资金）");
        return result;
    }

    /** 设置支付密码（模拟6位） */
    @Transactional(rollbackFor = Exception.class)
    public WalletVO setPassword(Long userId, String password) {
        if (password == null || !password.matches("\\d{6}")) {
            throw new BizException(ErrorCode.PARAM_ERROR, "支付密码须为6位数字（模拟）");
        }
        PayWallet wallet = findWalletOrCreate(userId);
        wallet.setPayPassword(password);
        walletMapper.updateById(wallet);
        return toWalletVO(wallet);
    }

    /** 钱包收支流水（分页） */
    public Page<PayTxnVO> walletTransactions(Long userId, int pageNum, int pageSize, String direction) {
        Page<PayTransaction> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<PayTransaction> wrapper = new LambdaQueryWrapper<PayTransaction>()
                .eq(PayTransaction::getUserId, userId)
                .orderByDesc(PayTransaction::getCreateTime);
        if (direction != null && !direction.isEmpty()) {
            wrapper.eq(PayTransaction::getDirection, direction);
        }
        Page<PayTransaction> result = txnMapper.selectPage(page, wrapper);
        Page<PayTxnVO> voPage = new Page<>(pageNum, pageSize, result.getTotal());
        voPage.setRecords(result.getRecords().stream().map(this::toTxnVO).toList());
        return voPage;
    }

    // ============================================================
    //  订单 / 收银台
    // ============================================================

    /** 模拟消费下单（淘宝式）：反诈风控 → 创建待支付订单 */
    @Transactional(rollbackFor = Exception.class)
    public PayOrderVO consume(Long userId, ConsumeDTO dto) {
        BizMerchant merchant = merchantMapper.selectById(dto.getMerchantId());
        if (merchant == null || merchant.getStatus() == null || merchant.getStatus() != 1) {
            throw new BizException(ErrorCode.BIZ_RULE_NOT_MET, "商户不存在或已禁用");
        }
        // 支付前反诈风控（模拟）：命中高危词拦截
        String checkText = (dto.getSubject() == null ? "" : dto.getSubject())
                + (dto.getDescription() == null ? "" : dto.getDescription());
        for (String kw : FRAUD_KEYWORDS) {
            if (checkText.contains(kw)) {
                throw new BizException(ErrorCode.BIZ_RULE_NOT_MET,
                        "支付前风控提示：订单内容疑似高风险（命中反诈词「" + kw + "」），已拦截本次支付。如系误判请联系银行客服。【模拟】");
            }
        }
        String subject = dto.getSubject();
        String remark = buildConsumeRemark(dto.getCategoryCode(), dto.getMccCode(), dto.getDescription(), merchant.getMerchantName());
        PayOrder order = buildOrder(userId, BIZ_MERCHANT_CONSUME, null, dto.getMerchantId(),
                subject, dto.getAmount(), remark);
        orderMapper.insert(order);
        log.info("[消费下单] 用户={}, 商户={}, 金额={}, 订单={}（模拟）",
                userId, merchant.getMerchantName(), dto.getAmount(), order.getOrderNo());
        return toOrderVO(order);
    }

    /** 分页查询我的订单 */
    public Page<PayOrderVO> listOrders(Long userId, String status, int pageNum, int pageSize) {
        Page<PayOrder> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<PayOrder> wrapper = new LambdaQueryWrapper<PayOrder>()
                .eq(PayOrder::getUserId, userId)
                .orderByDesc(PayOrder::getCreateTime);
        if (status != null && !status.isEmpty()) {
            wrapper.eq(PayOrder::getStatus, status);
        }
        Page<PayOrder> result = orderMapper.selectPage(page, wrapper);
        Page<PayOrderVO> voPage = new Page<>(pageNum, pageSize, result.getTotal());
        voPage.setRecords(result.getRecords().stream().map(this::toOrderVO).toList());
        return voPage;
    }

    /** 订单详情（收银台数据） */
    public PayOrderVO getOrder(Long userId, String orderNo) {
        PayOrder order = findByOrderNo(orderNo);
        checkOwner(order, userId);
        return toOrderVO(order);
    }

    /** 收银台支付 */
    @Transactional(rollbackFor = Exception.class)
    public PayOrderVO pay(Long userId, String orderNo, PayDTO dto) {
        PayOrder order = findByOrderNo(orderNo);
        checkOwner(order, userId);
        if (!ORDER_PENDING_PAY.equals(order.getStatus())) {
            throw new BizException(ErrorCode.BIZ_RULE_NOT_MET,
                    ORDER_PAID.equals(order.getStatus()) ? "订单已支付，请勿重复支付"
                            : ORDER_REFUNDED.equals(order.getStatus()) ? "订单已退款" : "订单已关闭");
        }
        // 超时关闭
        if (order.getExpireTime() != null && order.getExpireTime().isBefore(LocalDateTime.now())) {
            order.setStatus(ORDER_CLOSED);
            order.setCloseTime(LocalDateTime.now());
            orderMapper.updateById(order);
            throw new BizException(ErrorCode.BIZ_RULE_NOT_MET, "订单已超过15分钟未支付，已自动关闭");
        }
        String method = dto.getPayMethod();
        boolean qrPay = method != null && method.startsWith("QR_");
        if (!List.of("WALLET", "CARD", "SIM_BANK", "QR_WECHAT", "QR_ALIPAY", "QR_UNIONPAY", "QR_ICBC").contains(method)) {
            throw new BizException(ErrorCode.PARAM_ERROR, "不支持的支付方式");
        }
        BigDecimal balanceAfter = BigDecimal.ZERO;
        if (qrPay) {
            // 扫码支付（模拟外部渠道）：由微信/支付宝/银行App扫码，模拟渠道回调确认成功，
            // 不涉及平台钱包与支付密码；流水 balance_after 取钱包当前余额作参考
            PayWallet w = findWallet(userId);
            if (w != null) balanceAfter = w.getBalance();
            log.info("[扫码支付·模拟渠道回调] 订单={}, 金额={}, 方式={}", orderNo, order.getAmount(), method);
        } else {
            PayWallet wallet = findWallet(userId);
            if (wallet == null) {
                throw new BizException(ErrorCode.BIZ_RULE_NOT_MET, "请先开通钱包（模拟）");
            }
            // 支付密码校验（模拟）
            String expectPwd = wallet.getPayPassword() != null ? wallet.getPayPassword() : DEFAULT_PAY_PASSWORD;
            if (!expectPwd.equals(dto.getPassword())) {
                throw new BizException(ErrorCode.BIZ_RULE_NOT_MET, "支付密码错误（模拟，默认123456）");
            }
            balanceAfter = wallet.getBalance();
            if ("WALLET".equals(method)) {
                if (wallet.getBalance().compareTo(order.getAmount()) < 0) {
                    throw new BizException(ErrorCode.BIZ_RULE_NOT_MET,
                            "钱包余额不足，请先充值（模拟）。当前余额¥" + wallet.getBalance() + "，需支付¥" + order.getAmount());
                }
                wallet.setBalance(wallet.getBalance().subtract(order.getAmount()));
                walletMapper.updateById(wallet);
                balanceAfter = wallet.getBalance();
            }
        }

        order.setStatus(ORDER_PAID);
        order.setPayMethod(method);
        order.setPayTime(LocalDateTime.now());
        orderMapper.updateById(order);

        // 支出流水
        insertTxn(userId, null, order, "OUT", order.getAmount(), balanceAfter, method,
                order.getSubject(), null);

        // 业务后置
        afterPaid(order);

        log.info("[支付成功] 用户={}, 订单={}, 金额={}, 方式={}（模拟）", userId, orderNo, order.getAmount(), method);
        return toOrderVO(order);
    }

    /** 关闭订单（取消/超时） */
    @Transactional(rollbackFor = Exception.class)
    public PayOrderVO closeOrder(Long userId, String orderNo) {
        PayOrder order = findByOrderNo(orderNo);
        checkOwner(order, userId);
        if (!ORDER_PENDING_PAY.equals(order.getStatus())) {
            throw new BizException(ErrorCode.BIZ_RULE_NOT_MET, "仅待支付订单可关闭");
        }
        order.setStatus(ORDER_CLOSED);
        order.setCloseTime(LocalDateTime.now());
        orderMapper.updateById(order);
        return toOrderVO(order);
    }

    /** 退款（模拟即时到账）：钱包原路退回 */
    @Transactional(rollbackFor = Exception.class)
    public PayOrderVO refund(Long userId, String orderNo, String reason) {
        PayOrder order = findByOrderNo(orderNo);
        checkOwner(order, userId);
        if (!ORDER_PAID.equals(order.getStatus())) {
            throw new BizException(ErrorCode.BIZ_RULE_NOT_MET, "仅已支付订单可退款");
        }
        if ("WALLET".equals(order.getPayMethod())) {
            PayWallet wallet = findWallet(userId);
            wallet.setBalance(wallet.getBalance().add(order.getAmount()));
            walletMapper.updateById(wallet);
            insertTxn(userId, null, order, "IN", order.getAmount(), wallet.getBalance(), "WALLET",
                    "订单退款：" + order.getSubject(), order.getOrderNo());
        } else if (order.getPayMethod() != null && order.getPayMethod().startsWith("QR_")) {
            // 扫码支付退款（模拟原路退回）：不动平台钱包，仅记流水
            PayWallet wallet = findWallet(userId);
            BigDecimal bal = wallet != null ? wallet.getBalance() : BigDecimal.ZERO;
            insertTxn(userId, null, order, "IN", order.getAmount(), bal, order.getPayMethod(),
                    "扫码支付退款（模拟原路退回）：" + order.getSubject(), order.getOrderNo());
        }
        order.setStatus(ORDER_REFUNDED);
        order.setCloseTime(LocalDateTime.now());
        orderMapper.updateById(order);
        log.info("[退款] 用户={}, 订单={}, 金额={}, 原因={}（模拟）", userId, orderNo, order.getAmount(), reason);
        return toOrderVO(order);
    }

    // ============================================================
    //  业务资金接入（供 LoanService / GuaranteeService 调用）
    // ============================================================

    /** 创建业务订单（保函缴费等），返回待支付订单 */
    public PayOrderVO createBizOrder(Long userId, String bizType, Long bizId, String subject,
                                     BigDecimal amount, Long merchantId) {
        PayOrder order = buildOrder(userId, bizType, bizId, merchantId, subject, amount, null);
        orderMapper.insert(order);
        return toOrderVO(order);
    }

    /** 钱包扣款（还款等业务直接扣，无收银台）：余额不足抛 3001 */
    @Transactional(rollbackFor = Exception.class)
    public PayWallet payFromWallet(Long userId, BigDecimal amount, String subject, String relatedNo) {
        PayWallet wallet = findWalletOrCreate(userId);
        if (wallet.getBalance().compareTo(amount) < 0) {
            throw new BizException(ErrorCode.BIZ_RULE_NOT_MET,
                    "钱包余额不足，请先充值（模拟）。当前余额¥" + wallet.getBalance() + "，需支付¥" + amount);
        }
        wallet.setBalance(wallet.getBalance().subtract(amount));
        walletMapper.updateById(wallet);
        PayOrder order = buildOrder(userId, BIZ_LOAN_REPAY, null, null, subject, amount, null);
        order.setStatus(ORDER_PAID);
        order.setPayMethod("WALLET");
        order.setPayTime(LocalDateTime.now());
        order.setRemark(relatedNo);
        orderMapper.insert(order);
        insertTxn(userId, null, order, "OUT", amount, wallet.getBalance(), "WALLET", subject, relatedNo);
        return wallet;
    }

    /** 放款入钱包（A类提款） */
    @Transactional(rollbackFor = Exception.class)
    public PayWallet creditToWallet(Long userId, BigDecimal amount, String subject, String relatedNo) {
        PayWallet wallet = findWalletOrCreate(userId);
        wallet.setBalance(wallet.getBalance().add(amount));
        walletMapper.updateById(wallet);
        PayOrder order = buildOrder(userId, "LOAN_WITHDRAW", null, null, subject, amount, null);
        order.setStatus(ORDER_PAID);
        order.setPayMethod("SIM_BANK");
        order.setPayTime(LocalDateTime.now());
        order.setRemark(relatedNo);
        orderMapper.insert(order);
        insertTxn(userId, null, order, "IN", amount, wallet.getBalance(), "SIM_BANK", subject, relatedNo);
        return wallet;
    }

    /** 商户入账（受托支付/消费收款）：商户余额 + 累计收款，写商户侧流水 */
    @Transactional(rollbackFor = Exception.class)
    public void incomeToMerchant(Long merchantId, BigDecimal amount, String orderNo, String bizType,
                                 String relatedNo, String subject) {
        PayMerchantAccount account = merchantAccountMapper.selectOne(
                new LambdaQueryWrapper<PayMerchantAccount>().eq(PayMerchantAccount::getMerchantId, merchantId));
        if (account == null) {
            account = new PayMerchantAccount();
            account.setMerchantId(merchantId);
            account.setBalance(BigDecimal.ZERO);
            account.setTotalIncome(BigDecimal.ZERO);
            account.setStatus("ACTIVE");
            merchantAccountMapper.insert(account);
        }
        account.setBalance(account.getBalance().add(amount));
        account.setTotalIncome(account.getTotalIncome().add(amount));
        merchantAccountMapper.updateById(account);

        PayTransaction txn = new PayTransaction();
        txn.setTxnNo(generateNo("PAY"));
        txn.setOrderId(0L);
        txn.setOrderNo(orderNo);
        txn.setMerchantId(merchantId);
        txn.setDirection("IN");
        txn.setAmount(amount);
        txn.setBalanceAfter(account.getBalance());
        txn.setPayMethod("SIM_BANK");
        txn.setStatus("SUCCESS");
        txn.setBizType(bizType);
        txn.setRelatedNo(relatedNo);
        txn.setRemark(subject + "（模拟，商户入账）");
        txnMapper.insert(txn);
        log.info("[商户入账] 商户={}, 金额={}, 订单={}, 类型={}（模拟）", merchantId, amount, orderNo, bizType);
    }

    // ============================================================
    //  管理端（ADMIN/banker）
    // ============================================================

    public Page<PayOrderVO> adminOrders(String status, String bizType, int pageNum, int pageSize) {
        Page<PayOrder> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<PayOrder> wrapper = new LambdaQueryWrapper<PayOrder>().orderByDesc(PayOrder::getCreateTime);
        if (status != null && !status.isEmpty()) wrapper.eq(PayOrder::getStatus, status);
        if (bizType != null && !bizType.isEmpty()) wrapper.eq(PayOrder::getBizType, bizType);
        Page<PayOrder> result = orderMapper.selectPage(page, wrapper);
        Page<PayOrderVO> voPage = new Page<>(pageNum, pageSize, result.getTotal());
        voPage.setRecords(result.getRecords().stream().map(this::toOrderVO).toList());
        return voPage;
    }

    public Page<PayTxnVO> adminTxns(String direction, int pageNum, int pageSize) {
        Page<PayTransaction> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<PayTransaction> wrapper = new LambdaQueryWrapper<PayTransaction>().orderByDesc(PayTransaction::getCreateTime);
        if (direction != null && !direction.isEmpty()) wrapper.eq(PayTransaction::getDirection, direction);
        Page<PayTransaction> result = txnMapper.selectPage(page, wrapper);
        Page<PayTxnVO> voPage = new Page<>(pageNum, pageSize, result.getTotal());
        voPage.setRecords(result.getRecords().stream().map(this::toTxnVO).toList());
        return voPage;
    }

    public List<MerchantAccountVO> adminMerchantAccounts() {
        List<PayMerchantAccount> accounts = merchantAccountMapper.selectList(
                new LambdaQueryWrapper<PayMerchantAccount>().orderByDesc(PayMerchantAccount::getTotalIncome));
        return accounts.stream().map(a -> {
            MerchantAccountVO vo = new MerchantAccountVO();
            vo.setId(a.getId());
            vo.setMerchantId(a.getMerchantId());
            vo.setBalance(a.getBalance());
            vo.setTotalIncome(a.getTotalIncome());
            vo.setStatus(a.getStatus());
            BizMerchant m = merchantMapper.selectById(a.getMerchantId());
            vo.setMerchantName(m != null ? m.getMerchantName() : "商户" + a.getMerchantId());
            return vo;
        }).toList();
    }

    // ============================================================
    //  内部工具
    // ============================================================

    private void afterPaid(PayOrder order) {
        if (BIZ_MERCHANT_CONSUME.equals(order.getBizType())) {
            // 商户入账
            incomeToMerchant(order.getMerchantId(), order.getAmount(), order.getOrderNo(),
                    BIZ_MERCHANT_CONSUME, order.getOrderNo(), order.getSubject());
            // 同步写消费记账（biz_transaction，EXPENSE 归类）
            writeBizTransaction(order);
        }
        // 其他业务（GUARANTEE_FEE）通过事件回调
        eventPublisher.publishEvent(new PaySuccessEvent(order.getOrderNo(), order.getBizType(),
                order.getBizId(), order.getUserId(), order.getAmount(), order.getPayMethod(), order.getMerchantId()));
    }

    /** 消费支付成功后同步写 biz_transaction（归类，供预算页展示） */
    private void writeBizTransaction(PayOrder order) {
        BizTransaction tx = new BizTransaction();
        tx.setUserId(order.getUserId());
        tx.setTransactionNo(generateNo("TX"));
        tx.setTransactionType("EXPENSE");
        tx.setAmount(order.getAmount());
        tx.setTransactionTime(LocalDateTime.now());
        tx.setSource("SIMULATED");
        String[] remarkParts = order.getRemark() == null ? new String[0] : order.getRemark().split("\\u0001");
        if (remarkParts.length >= 3) {
            tx.setCategoryCode(remarkParts[0]);
            tx.setMccCode(remarkParts[1]);
            tx.setDescription(remarkParts[2]);
            tx.setMerchantName(remarkParts[3]);
        }
        bizTransactionMapper.insert(tx);
    }

    private String buildConsumeRemark(String categoryCode, String mccCode, String description, String merchantName) {
        return (categoryCode == null ? "OTHER" : categoryCode) + "\u0001"
                + (mccCode == null ? "" : mccCode) + "\u0001"
                + (description == null ? "" : description) + "\u0001"
                + (merchantName == null ? "" : merchantName);
    }

    private PayOrder buildOrder(Long userId, String bizType, Long bizId, Long merchantId,
                                String subject, BigDecimal amount, String remark) {
        PayOrder order = new PayOrder();
        order.setOrderNo(generateNo("ORD"));
        order.setUserId(userId);
        order.setBizType(bizType);
        order.setBizId(bizId);
        order.setMerchantId(merchantId);
        order.setSubject(subject);
        order.setAmount(amount);
        order.setStatus(ORDER_PENDING_PAY);
        order.setExpireTime(LocalDateTime.now().plusMinutes(15));
        order.setRemark(remark);
        return order;
    }

    private PayWallet findWallet(Long userId) {
        return walletMapper.selectOne(new LambdaQueryWrapper<PayWallet>().eq(PayWallet::getUserId, userId));
    }

    private PayWallet findWalletOrCreate(Long userId) {
        PayWallet wallet = findWallet(userId);
        if (wallet == null) {
            return getWallet(userId) != null ? findWallet(userId) : null;
        }
        return wallet;
    }

    private PayOrder findByOrderNo(String orderNo) {
        PayOrder order = orderMapper.selectOne(new LambdaQueryWrapper<PayOrder>().eq(PayOrder::getOrderNo, orderNo));
        if (order == null) {
            throw new BizException(ErrorCode.BIZ_RULE_NOT_MET, "订单不存在");
        }
        return order;
    }

    private void checkOwner(PayOrder order, Long userId) {
        if (!order.getUserId().equals(userId)) {
            throw new BizException(ErrorCode.FORBIDDEN, "无权访问他人订单");
        }
    }

    private void insertTxn(Long userId, Long merchantId, PayOrder order, String direction,
                           BigDecimal amount, BigDecimal balanceAfter, String method, String remark, String relatedNo) {
        PayTransaction txn = new PayTransaction();
        txn.setTxnNo(generateNo("PAY"));
        txn.setOrderId(order.getId());
        txn.setOrderNo(order.getOrderNo());
        txn.setUserId(userId);
        txn.setMerchantId(merchantId);
        txn.setDirection(direction);
        txn.setAmount(amount);
        txn.setBalanceAfter(balanceAfter);
        txn.setPayMethod(method);
        txn.setStatus("SUCCESS");
        txn.setBizType(order.getBizType());
        txn.setRelatedNo(relatedNo);
        txn.setRemark(remark);
        txnMapper.insert(txn);
    }

    private WalletVO toWalletVO(PayWallet w) {
        WalletVO vo = new WalletVO();
        vo.setWalletId(w.getId());
        vo.setUserId(w.getUserId());
        vo.setBalance(w.getBalance());
        vo.setFrozen(w.getFrozen());
        vo.setHasPassword(w.getPayPassword() != null);
        vo.setRemark("模拟电子钱包，不涉及真实资金；支付密码默认123456");
        return vo;
    }

    private PayOrderVO toOrderVO(PayOrder o) {
        PayOrderVO vo = new PayOrderVO();
        vo.setId(o.getId());
        vo.setOrderNo(o.getOrderNo());
        vo.setUserId(o.getUserId());
        vo.setBizType(o.getBizType());
        vo.setBizTypeName(bizTypeName(o.getBizType()));
        vo.setBizId(o.getBizId());
        vo.setMerchantId(o.getMerchantId());
        vo.setSubject(o.getSubject());
        vo.setAmount(o.getAmount());
        vo.setPayMethod(o.getPayMethod());
        vo.setPayMethodName(methodName(o.getPayMethod()));
        vo.setStatus(o.getStatus());
        vo.setStatusName(statusName(o.getStatus()));
        vo.setPayTime(o.getPayTime());
        vo.setCloseTime(o.getCloseTime());
        vo.setExpireTime(o.getExpireTime());
        vo.setRemark(o.getRemark());
        vo.setCreateTime(o.getCreateTime());
        if (o.getMerchantId() != null) {
            BizMerchant m = merchantMapper.selectById(o.getMerchantId());
            vo.setMerchantName(m != null ? m.getMerchantName() : null);
        }
        return vo;
    }

    private PayTxnVO toTxnVO(PayTransaction t) {
        PayTxnVO vo = new PayTxnVO();
        vo.setId(t.getId());
        vo.setTxnNo(t.getTxnNo());
        vo.setOrderNo(t.getOrderNo());
        vo.setDirection(t.getDirection());
        vo.setAmount(t.getAmount());
        vo.setBalanceAfter(t.getBalanceAfter());
        vo.setPayMethod(t.getPayMethod());
        vo.setBizType(t.getBizType());
        vo.setBizTypeName(bizTypeName(t.getBizType()));
        vo.setRelatedNo(t.getRelatedNo());
        vo.setRemark(t.getRemark());
        vo.setCreateTime(t.getCreateTime());
        return vo;
    }

    public static String bizTypeName(String bizType) {
        return switch (bizType == null ? "" : bizType) {
            case BIZ_GUARANTEE_FEE -> "保函费";
            case BIZ_LOAN_REPAY -> "贷款还款";
            case BIZ_ENTRUST_PAY -> "受托支付";
            case BIZ_MERCHANT_CONSUME -> "模拟消费";
            case BIZ_RECHARGE -> "钱包充值";
            case "LOAN_WITHDRAW" -> "贷款提款";
            case "REFUND" -> "退款";
            default -> bizType;
        };
    }

    private String statusName(String status) {
        return switch (status == null ? "" : status) {
            case ORDER_PENDING_PAY -> "待支付";
            case ORDER_PAID -> "已支付";
            case ORDER_CLOSED -> "已关闭";
            case ORDER_REFUNDED -> "已退款";
            default -> status;
        };
    }

    private String methodName(String method) {
        return switch (method == null ? "" : method) {
            case "WALLET" -> "钱包余额";
            case "CARD" -> "模拟银行卡";
            case "SIM_BANK" -> "工行e支付";
            case "QR_WECHAT" -> "微信扫码支付";
            case "QR_ALIPAY" -> "支付宝扫码支付";
            case "QR_UNIONPAY" -> "云闪付扫码支付";
            case "QR_ICBC" -> "工行扫码支付";
            default -> method;
        };
    }

    private String generateNo(String prefix) {
        return prefix + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS"))
                + String.format("%03d", ThreadLocalRandom.current().nextInt(1000));
    }
}
