package com.icbc.qingqi.module.admin.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.icbc.qingqi.common.BizException;
import com.icbc.qingqi.common.ErrorCode;
import com.icbc.qingqi.module.admin.dto.LoanReviewDTO;
import com.icbc.qingqi.module.admin.dto.WarningHandleDTO;
import com.icbc.qingqi.module.guarantee.entity.BizGuaranteeApplication;
import com.icbc.qingqi.module.guarantee.mapper.BizGuaranteeApplicationMapper;
import com.icbc.qingqi.module.loan.entity.BizCreditTxn;
import com.icbc.qingqi.module.loan.entity.BizEntrustPayment;
import com.icbc.qingqi.module.loan.entity.BizLoanApplication;
import com.icbc.qingqi.module.loan.mapper.BizCreditTxnMapper;
import com.icbc.qingqi.module.loan.mapper.BizEntrustPaymentMapper;
import com.icbc.qingqi.module.loan.mapper.BizLoanApplicationMapper;
import com.icbc.qingqi.module.message.entity.SysMessage;
import com.icbc.qingqi.module.message.mapper.SysMessageMapper;
import com.icbc.qingqi.module.risk.entity.BizRiskWarning;
import com.icbc.qingqi.module.risk.mapper.BizRiskWarningMapper;
import com.icbc.qingqi.module.user.entity.BizRegistrationReview;
import com.icbc.qingqi.module.user.entity.SysUser;
import com.icbc.qingqi.module.user.mapper.BizRegistrationReviewMapper;
import com.icbc.qingqi.module.user.mapper.SysUserMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * 管理端业务审核台服务（步骤 8·缺口 #22）
 * <p>
 * 5 队列中：
 * ① AI 复审队列：仅全量查询保函 MANUAL_REVIEW；审核操作复用既有
 *    PUT /v1/guarantee/{id}/manual-review（GuaranteeService.manualReviewApplication）
 * ② 索赔复核队列：复用 /v1/guarantee/claims/manual-review-queue + /v1/guarantee/claims/{id}/review
 * ③ 贷款审批：本服务实现全量查询 + 审批操作
 * ④ 商户白名单审核：复用 /v1/loan/merchants/by-status + /v1/loan/merchants/{id}/audit
 * ⑤ 风险预警总览：本服务实现全量查询 + 标记已处理
 * <p>
 * 所有银行能力均为模拟桩，审批操作自动写 sys_message 通知用户（双端状态同步）。
 */
@Slf4j
@Service
public class AdminService {

    private final BizGuaranteeApplicationMapper guaranteeApplicationMapper;
    private final BizLoanApplicationMapper loanApplicationMapper;
    private final BizCreditTxnMapper creditTxnMapper;
    private final BizEntrustPaymentMapper entrustPaymentMapper;
    private final BizRiskWarningMapper riskWarningMapper;
    private final SysMessageMapper messageMapper;
    private final SysUserMapper sysUserMapper;
    private final BizRegistrationReviewMapper registrationReviewMapper;

    // 状态常量
    private static final String LOAN_PENDING_APPROVAL = "PENDING_APPROVAL";
    private static final String LOAN_APPROVED = "APPROVED";
    private static final String LOAN_REJECTED = "REJECTED";
    private static final String LOAN_PRE_CHECK = "PRE_CHECK";

    private static final String DECISION_APPROVED = "APPROVED";
    private static final String DECISION_REJECTED = "REJECTED";
    private static final String DECISION_RETURNED = "RETURNED";

    public AdminService(BizGuaranteeApplicationMapper guaranteeApplicationMapper,
                        BizLoanApplicationMapper loanApplicationMapper,
                        BizCreditTxnMapper creditTxnMapper,
                        BizEntrustPaymentMapper entrustPaymentMapper,
                        BizRiskWarningMapper riskWarningMapper,
                        SysMessageMapper messageMapper,
                        SysUserMapper sysUserMapper,
                        BizRegistrationReviewMapper registrationReviewMapper) {
        this.guaranteeApplicationMapper = guaranteeApplicationMapper;
        this.loanApplicationMapper = loanApplicationMapper;
        this.creditTxnMapper = creditTxnMapper;
        this.entrustPaymentMapper = entrustPaymentMapper;
        this.riskWarningMapper = riskWarningMapper;
        this.messageMapper = messageMapper;
        this.sysUserMapper = sysUserMapper;
        this.registrationReviewMapper = registrationReviewMapper;
    }

    // ============================================================
    //  ① AI 复审队列 - 保函存疑转人工
    // ============================================================

    public Page<BizGuaranteeApplication> pageGuaranteeManualReviewQueue(int pageNum, int pageSize) {
        Page<BizGuaranteeApplication> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<BizGuaranteeApplication> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(BizGuaranteeApplication::getApplyStatus, "MANUAL_REVIEW");
        wrapper.orderByDesc(BizGuaranteeApplication::getSubmitTime);
        return guaranteeApplicationMapper.selectPage(page, wrapper);
    }

    // ============================================================
    //  ③ 贷款审批
    // ============================================================

    public Page<BizLoanApplication> pageLoanApplications(int pageNum, int pageSize,
                                                          String status, String loanType) {
        Page<BizLoanApplication> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<BizLoanApplication> wrapper = new LambdaQueryWrapper<>();
        if (status != null && !status.isBlank()) {
            wrapper.eq(BizLoanApplication::getApplyStatus, status);
        } else {
            // 默认查待审批
            wrapper.eq(BizLoanApplication::getApplyStatus, LOAN_PENDING_APPROVAL);
        }
        if (loanType != null && !loanType.isBlank()) {
            wrapper.eq(BizLoanApplication::getLoanType, loanType);
        }
        wrapper.orderByDesc(BizLoanApplication::getSubmitTime);
        return loanApplicationMapper.selectPage(page, wrapper);
    }

    @Transactional(rollbackFor = Exception.class)
    public BizLoanApplication reviewLoanApplication(Long applicationId, LoanReviewDTO dto) {
        BizLoanApplication app = loanApplicationMapper.selectById(applicationId);
        if (app == null) {
            throw new BizException(ErrorCode.BIZ_RULE_NOT_MET, "贷款申请不存在");
        }
        if (!LOAN_PENDING_APPROVAL.equals(app.getApplyStatus())) {
            throw new BizException(ErrorCode.BIZ_RULE_NOT_MET,
                    "当前申请状态为" + app.getApplyStatus() + "，不可审批");
        }

        String decision = dto.getDecision();
        String msgTitle;
        String msgContent;

        if (DECISION_APPROVED.equals(decision)) {
            // 审批通过：写审批金额、审批时间
            BigDecimal approveAmount = dto.getApproveAmount() != null
                    ? dto.getApproveAmount() : app.getApplyAmount();
            app.setApplyStatus(LOAN_APPROVED);
            app.setApproveAmount(approveAmount);
            app.setApproveTime(LocalDateTime.now());
            msgTitle = "贷款申请已通过审批【模拟】";
            msgContent = "您的贷款申请（编号 " + app.getApplyNo()
                    + "）已通过 banker 审批，审批金额 ¥" + approveAmount
                    + "。【模拟桩，不产生真实放款】";
        } else if (DECISION_REJECTED.equals(decision)) {
            if (dto.getRejectReason() == null || dto.getRejectReason().isBlank()) {
                throw new BizException(ErrorCode.PARAM_ERROR, "拒绝原因不能为空");
            }
            app.setApplyStatus(LOAN_REJECTED);
            app.setRejectReason(dto.getRejectReason());
            app.setApproveTime(LocalDateTime.now());
            msgTitle = "贷款申请未通过审批【模拟】";
            msgContent = "您的贷款申请（编号 " + app.getApplyNo()
                    + "）banker 审批未通过，原因：" + dto.getRejectReason() + "。";
        } else if (DECISION_RETURNED.equals(decision)) {
            if (dto.getReturnReason() == null || dto.getReturnReason().isBlank()) {
                throw new BizException(ErrorCode.PARAM_ERROR, "退回原因不能为空");
            }
            app.setApplyStatus(LOAN_PRE_CHECK);
            app.setRejectReason("退回补充资料：" + dto.getReturnReason());
            app.setApproveTime(LocalDateTime.now());
            msgTitle = "贷款申请退回补充资料【模拟】";
            msgContent = "您的贷款申请（编号 " + app.getApplyNo()
                    + "）banker 退回，需补充资料：" + dto.getReturnReason() + "。";
        } else {
            throw new BizException(ErrorCode.PARAM_ERROR,
                    "审批结论只能为 APPROVED / REJECTED / RETURNED");
        }

        loanApplicationMapper.updateById(app);
        log.info("[贷款审批] 申请编号={}, banker decision={}, 审批金额={}",
                app.getApplyNo(), decision, app.getApproveAmount());

        // 写站内信通知用户（双端状态同步）
        SysMessage msg = new SysMessage();
        msg.setUserId(app.getUserId());
        msg.setTitle(msgTitle);
        msg.setContent(msgContent);
        msg.setType("BUSINESS");
        msg.setBizType("LOAN");
        msg.setBizId(app.getId());
        msg.setIsRead(0);
        messageMapper.insert(msg);

        return app;
    }

    public Page<BizCreditTxn> pageCreditTxns(int pageNum, int pageSize, String txnType) {
        Page<BizCreditTxn> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<BizCreditTxn> wrapper = new LambdaQueryWrapper<>();
        if (txnType != null && !txnType.isBlank()) {
            wrapper.eq(BizCreditTxn::getTxnType, txnType);
        }
        wrapper.orderByDesc(BizCreditTxn::getTxnTime);
        return creditTxnMapper.selectPage(page, wrapper);
    }

    public Page<BizEntrustPayment> pageEntrustPayments(int pageNum, int pageSize, String paymentStatus) {
        Page<BizEntrustPayment> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<BizEntrustPayment> wrapper = new LambdaQueryWrapper<>();
        if (paymentStatus != null && !paymentStatus.isBlank()) {
            wrapper.eq(BizEntrustPayment::getPaymentStatus, paymentStatus);
        }
        wrapper.orderByDesc(BizEntrustPayment::getCreateTime);
        return entrustPaymentMapper.selectPage(page, wrapper);
    }

    // ============================================================
    //  ⑤ 风险预警总览
    // ============================================================

    public Page<BizRiskWarning> pageRiskWarnings(int pageNum, int pageSize,
                                                  String warningType, String warningLevel,
                                                  Integer isHandled) {
        Page<BizRiskWarning> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<BizRiskWarning> wrapper = new LambdaQueryWrapper<>();
        if (warningType != null && !warningType.isBlank()) {
            wrapper.eq(BizRiskWarning::getWarningType, warningType);
        }
        if (warningLevel != null && !warningLevel.isBlank()) {
            wrapper.eq(BizRiskWarning::getWarningLevel, warningLevel);
        }
        if (isHandled != null) {
            wrapper.eq(BizRiskWarning::getIsHandled, isHandled);
        }
        wrapper.orderByDesc(BizRiskWarning::getWarningTime);
        return riskWarningMapper.selectPage(page, wrapper);
    }

    @Transactional(rollbackFor = Exception.class)
    public BizRiskWarning handleRiskWarning(Long id, WarningHandleDTO dto) {
        BizRiskWarning warning = riskWarningMapper.selectById(id);
        if (warning == null) {
            throw new BizException(ErrorCode.BIZ_RULE_NOT_MET, "预警记录不存在");
        }
        warning.setIsHandled(1);
        warning.setHandleNote(dto.getHandleNote());
        warning.setHandleTime(LocalDateTime.now());
        riskWarningMapper.updateById(warning);
        log.info("[预警处置] 预警ID={}, 类型={}, 处置备注={}",
                id, warning.getWarningType(), dto.getHandleNote());

        // 写站内信通知用户（双端状态同步）
        SysMessage msg = new SysMessage();
        msg.setUserId(warning.getUserId());
        msg.setTitle("风险预警已处置【模拟】");
        msg.setContent("您的「" + warning.getWarningTitle()
                + "」预警已被 banker 处置，处置说明：" + dto.getHandleNote() + "。");
        msg.setType("SAFETY");
        msg.setBizType("RISK_WARNING");
        msg.setBizId(warning.getId());
        msg.setIsRead(0);
        messageMapper.insert(msg);

        return warning;
    }

    // ============================================================
    //  ⑥ 保函管理（全量申请列表，含待房东确认）
    // ============================================================

    public Page<BizGuaranteeApplication> pageGuaranteeApplications(int pageNum, int pageSize, String status) {
        Page<BizGuaranteeApplication> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<BizGuaranteeApplication> wrapper = new LambdaQueryWrapper<>();
        if (status != null && !status.isBlank()) {
            wrapper.eq(BizGuaranteeApplication::getApplyStatus, status);
        }
        wrapper.orderByDesc(BizGuaranteeApplication::getSubmitTime);
        return guaranteeApplicationMapper.selectPage(page, wrapper);
    }

    // ============================================================
    //  ⑦ 注册审核记录（白名单 AI 审核留痕）
    // ============================================================

    public Page<BizRegistrationReview> pageRegistrationReviews(int pageNum, int pageSize,
                                                                String keyword, String result) {
        Page<BizRegistrationReview> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<BizRegistrationReview> wrapper = new LambdaQueryWrapper<>();
        if (keyword != null && !keyword.isBlank()) {
            wrapper.and(w -> w.like(BizRegistrationReview::getUsername, keyword)
                    .or().like(BizRegistrationReview::getRealName, keyword)
                    .or().like(BizRegistrationReview::getPhone, keyword)
                    .or().like(BizRegistrationReview::getReviewNo, keyword));
        }
        if (result != null && !result.isBlank()) {
            wrapper.eq(BizRegistrationReview::getResult, result);
        }
        wrapper.orderByDesc(BizRegistrationReview::getCreateTime);
        return registrationReviewMapper.selectPage(page, wrapper);
    }

    // ============================================================
    //  ⑧ 仪表盘统计（管理端数据看板）
    // ============================================================

    public Map<String, Object> dashboardStats() {
        Map<String, Object> stats = new HashMap<>();
        // 用户维度
        stats.put("userCount", sysUserMapper.selectCount(null));
        stats.put("userToday", sysUserMapper.selectCount(new LambdaQueryWrapper<SysUser>()
                .ge(SysUser::getCreateTime, LocalDate.now().atStartOfDay())));
        // 保函维度
        stats.put("guaranteeTotal", guaranteeApplicationMapper.selectCount(null));
        stats.put("guaranteePendingConfirm", guaranteeApplicationMapper.selectCount(
                new LambdaQueryWrapper<BizGuaranteeApplication>()
                        .eq(BizGuaranteeApplication::getApplyStatus, "SUBMITTED")));
        stats.put("guaranteeManualReview", guaranteeApplicationMapper.selectCount(
                new LambdaQueryWrapper<BizGuaranteeApplication>()
                        .eq(BizGuaranteeApplication::getApplyStatus, "MANUAL_REVIEW")));
        // 贷款维度
        stats.put("loanPending", loanApplicationMapper.selectCount(
                new LambdaQueryWrapper<BizLoanApplication>()
                        .eq(BizLoanApplication::getApplyStatus, LOAN_PENDING_APPROVAL)));
        // 风控维度
        stats.put("riskUnhandled", riskWarningMapper.selectCount(
                new LambdaQueryWrapper<BizRiskWarning>()
                        .eq(BizRiskWarning::getIsHandled, 0)));
        // 注册审核维度
        stats.put("registrationReviews", registrationReviewMapper.selectCount(null));
        return stats;
    }
}
