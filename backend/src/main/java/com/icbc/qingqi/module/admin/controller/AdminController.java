package com.icbc.qingqi.module.admin.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.icbc.qingqi.common.Result;
import com.icbc.qingqi.module.admin.dto.LoanReviewDTO;
import com.icbc.qingqi.module.admin.dto.ObservationUserVO;
import com.icbc.qingqi.module.admin.dto.WarningHandleDTO;
import com.icbc.qingqi.module.admin.service.AdminService;
import com.icbc.qingqi.module.guarantee.entity.BizGuaranteeApplication;
import com.icbc.qingqi.module.guarantee.service.GuaranteeService;
import com.icbc.qingqi.module.guarantee.dto.GuaranteeApplicationVO;
import com.icbc.qingqi.module.loan.entity.BizCreditTxn;
import com.icbc.qingqi.module.loan.entity.BizAiReviewLog;
import com.icbc.qingqi.module.loan.entity.BizEntrustPayment;
import com.icbc.qingqi.module.loan.entity.BizEntrustReview;
import com.icbc.qingqi.module.loan.entity.BizLoanApplication;
import com.icbc.qingqi.module.loan.entity.BizMerchant;
import com.icbc.qingqi.module.loan.service.LoanService;
import com.icbc.qingqi.module.risk.entity.BizRiskWarning;
import com.icbc.qingqi.security.UserContext;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 管理端业务审核台（步骤 8·缺口 #22）
 * <p>
 * 路径：/api/v1/admin/**
 * 由 JwtAuthFilter 中 isAdminPath(uri) 自动校验 ADMIN 角色，越权返回 4001。
 * <p>
 * 5 个审核队列：
 * ① AI 复审队列：保函存疑转人工（MANUAL_REVIEW）
 * ② 索赔复核队列：复用 /v1/guarantee/claims/manual-review-queue + /v1/guarantee/claims/{id}/review
 * ③ 贷款审批：A 类提款 / B 类受托支付流水复核
 * ④ 商户白名单审核：复用 /v1/loan/merchants/by-status + /v1/loan/merchants/{id}/audit
 * ⑤ 风险预警总览：全量聚合 biz_risk_warning
 * <p>
 * 所有银行能力均为模拟桩，演示数据标注"模拟"。
 */
@Tag(name = "管理端业务审核台")
@RestController
@RequestMapping("/v1/admin")
public class AdminController {

    private final AdminService adminService;
    private final LoanService loanService;
    private final GuaranteeService guaranteeService;

    public AdminController(AdminService adminService, LoanService loanService, GuaranteeService guaranteeService) {
        this.adminService = adminService;
        this.loanService = loanService;
        this.guaranteeService = guaranteeService;
    }

    // ============================================================
    //  ① AI 复审队列 - 保函存疑转人工
    // ============================================================

    @Operation(summary = "① AI 复审队列 - 保函存疑转人工",
            description = "全量分页查询 MANUAL_REVIEW 状态的保函申请；banker 审核操作复用 PUT /v1/guarantee/{id}/manual-review?decision=APPROVED|REJECTED&rejectReason=...")
    @GetMapping("/guarantee/manual-review-queue")
    public Result<Page<BizGuaranteeApplication>> guaranteeManualReviewQueue(
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "10") int pageSize) {
        return Result.success(adminService.pageGuaranteeManualReviewQueue(pageNum, pageSize));
    }

    // ============================================================
    //  ③ 贷款审批
    // ============================================================

    @Operation(summary = "③-1 贷款申请审批队列（全量分页）",
            description = "按状态/类型筛选贷款申请；默认查 PENDING_APPROVAL。")
    @GetMapping("/loan/applications")
    public Result<Page<BizLoanApplication>> loanApplications(
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "10") int pageSize,
            @Parameter(description = "申请状态：PRE_CHECK/PENDING_APPROVAL/APPROVED/REJECTED/CANCELLED")
            @RequestParam(required = false) String status,
            @Parameter(description = "贷款类型：A_TYPE/B_TYPE")
            @RequestParam(required = false) String loanType) {
        return Result.success(adminService.pageLoanApplications(pageNum, pageSize, status, loanType));
    }

    @Operation(summary = "③-2 审批贷款申请",
            description = "banker 对 PENDING_APPROVAL 申请裁决：APPROVED 通过（写 approve_amount=apply_amount）/ REJECTED 拒绝 / RETURNED 退回补充资料；自动写站内信通知用户。")
    @PutMapping("/loan/applications/{id}/review")
    public Result<BizLoanApplication> reviewLoanApplication(
            @PathVariable Long id,
            @Valid @RequestBody LoanReviewDTO dto) {
        return Result.success(adminService.reviewLoanApplication(id, dto));
    }

    @Operation(summary = "③-3 A 类提款流水（全量分页）",
            description = "全量分页查询 A 类循环贷提款/还款流水（biz_credit_txn），用于审批台展示。")
    @GetMapping("/loan/credit-txns")
    public Result<Page<BizCreditTxn>> creditTxns(
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "10") int pageSize,
            @Parameter(description = "交易类型：WITHDRAW/REPAY")
            @RequestParam(required = false) String txnType) {
        return Result.success(adminService.pageCreditTxns(pageNum, pageSize, txnType));
    }

    @Operation(summary = "③-3.5 B转A观察期用户列表",
            description = "B 类授信且进入观察期（OBSERVING/PROMOTED/EXITED）的用户，含观察月数/评分/转A进度，贷款数据与用户画像关联展示。")
    @GetMapping("/loan/observation-users")
    public Result<Page<ObservationUserVO>> observationUsers(
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "10") int pageSize,
            @Parameter(description = "用户名/姓名关键词")
            @RequestParam(required = false) String keyword) {
        return Result.success(adminService.pageObservationUsers(pageNum, pageSize, keyword));
    }

    @Operation(summary = "③-5 AI 审核记录（借款前AI审查日志）",
            description = "分页查询借款前 AI 审查日志（提款/打款时的模拟AI审查），支持按用户/结果/授信类型筛选。")
    @GetMapping("/loan/ai-review-logs")
    public Result<Page<BizAiReviewLog>> aiReviewLogs(
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "10") int pageSize,
            @Parameter(description = "用户ID")
            @RequestParam(required = false) Long userId,
            @Parameter(description = "审查结果：PASS/REJECT")
            @RequestParam(required = false) String result,
            @Parameter(description = "授信类型：A_TYPE/B_TYPE")
            @RequestParam(required = false) String creditType) {
        return Result.success(adminService.pageAiReviewLogs(pageNum, pageSize, userId, result, creditType));
    }

    @Operation(summary = "③-4 B 类受托支付流水（全量分页）",
            description = "全量分页查询受托支付流水（biz_entrust_payment），用于审批台展示。")
    @GetMapping("/loan/entrust-payments")
    public Result<Page<BizEntrustPayment>> entrustPayments(
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "10") int pageSize,
            @Parameter(description = "支付状态：PENDING/PROCESSING/SUCCESS/FAILED")
            @RequestParam(required = false) String paymentStatus) {
        return Result.success(adminService.pageEntrustPayments(pageNum, pageSize, paymentStatus));
    }

    // ============================================================
    //  ④ 商户白名单审核（自定义商户审查）
    // ============================================================

    @Operation(summary = "④ 商户白名单队列（按状态）",
            description = "verifyStatus: PENDING待审（默认全量含灰名单）/ VERIFIED-白名单 / REJECTED-已拒绝。含用户自定义商户（来源USER_CUSTOM）与预置商户（SYSTEM）。")
    @GetMapping("/merchants")
    public Result<List<BizMerchant>> merchants(
            @Parameter(description = "认证状态") @RequestParam(required = false) String verifyStatus) {
        return Result.success(loanService.listMerchantsByVerifyStatus(verifyStatus));
    }

    @Operation(summary = "④-2 banker 审核商户白名单（含自定义商户）",
            description = "对 PENDING 商户裁决：VERIFIED-加入白名单（用户端立即可选）/ REJECTED-拒绝（必须填 reason 驳回原因，用户端可见）。")
    @PutMapping("/merchants/{id}/audit")
    public Result<BizMerchant> auditMerchant(
            @Parameter(description = "商户ID") @PathVariable Long id,
            @Parameter(description = "审核结论：VERIFIED/REJECTED") @RequestParam String verifyStatus,
            @Parameter(description = "审核意见/驳回原因") @RequestParam(required = false) String reason) {
        return Result.success(loanService.auditMerchant(id, verifyStatus, reason, UserContext.getUserId()));
    }

    // ============================================================
    //  ④-3 受托支付复核（自定义商户每单复核）
    // ============================================================

    @Operation(summary = "④-3 受托支付复核单队列（自定义商户每单复核）",
            description = "用户自定义商户（USER_CUSTOM）每次受托支付提交后生成复核单（PENDING），banker 复核通过后才执行放款；status: PENDING/APPROVED/REJECTED。")
    @GetMapping("/entrust-reviews")
    public Result<List<BizEntrustReview>> entrustReviews(
            @Parameter(description = "复核状态：PENDING/APPROVED/REJECTED") @RequestParam(required = false) String status) {
        return Result.success(loanService.listEntrustReviews(status));
    }

    @Operation(summary = "④-3-2 banker 复核受托支付（自定义商户每单复核）",
            description = "approve=true 复核通过并执行放款（扣额度+EP流水+商户收款入账）；approve=false 驳回，不放款、额度不动。")
    @PutMapping("/entrust-reviews/{id}/audit")
    public Result<BizEntrustReview> auditEntrustReview(
            @Parameter(description = "复核单ID") @PathVariable Long id,
            @Parameter(description = "复核结论：true通过并放款/false驳回") @RequestParam boolean approve,
            @Parameter(description = "复核意见/驳回原因") @RequestParam(required = false) String reason) {
        return Result.success(loanService.auditEntrustReview(id, approve, reason, UserContext.getUserId()));
    }

    // ============================================================
    //  ⑤ 风险预警总览
    // ============================================================

    @Operation(summary = "⑤ 风险预警总览（全量分页聚合）",
            description = "全量分页查询 biz_risk_warning；支持按 warningType（OVERDUE_RISK/HIGH_FREQ_BORROW/CREDIT_ABNORMAL/BUDGET_OVER/CASHFLOW_WARNING）+ warningLevel 过滤。")
    @GetMapping("/risk-warnings")
    public Result<Page<BizRiskWarning>> riskWarnings(
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "10") int pageSize,
            @Parameter(description = "预警类型")
            @RequestParam(required = false) String warningType,
            @Parameter(description = "预警等级：LOW/MEDIUM/HIGH/CRITICAL")
            @RequestParam(required = false) String warningLevel,
            @Parameter(description = "是否已处理：0-未处理 / 1-已处理")
            @RequestParam(required = false) Integer isHandled) {
        return Result.success(adminService.pageRiskWarnings(pageNum, pageSize, warningType, warningLevel, isHandled));
    }

    @Operation(summary = "⑤-2 标记预警已处理",
            description = "banker 标记预警为已处理并填处置备注；自动写站内信通知用户。")
    @PutMapping("/risk-warnings/{id}/handle")
    public Result<BizRiskWarning> handleRiskWarning(
            @PathVariable Long id,
            @Valid @RequestBody WarningHandleDTO dto) {
        return Result.success(adminService.handleRiskWarning(id, dto));
    }

    // ============================================================
    //  ⑥ 保函管理（全量申请 + 代房东确认）
    // ============================================================

    @Operation(summary = "⑥ 保函申请全量分页（含待房东确认 SUBMITTED）",
            description = "全量分页查询保函申请，按状态筛选（默认全部）：SUBMITTED待房东确认/LANDLORD_CONFIRM待缴费前/PENDING_PAY/AI_REVIEW/MANUAL_REVIEW/ISSUED/EXPIRED 等。")
    @GetMapping("/guarantee/applications")
    public Result<Page<BizGuaranteeApplication>> guaranteeApplications(
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "10") int pageSize,
            @Parameter(description = "申请状态（为空查全部）")
            @RequestParam(required = false) String status) {
        return Result.success(adminService.pageGuaranteeApplications(pageNum, pageSize, status));
    }

    @Operation(summary = "⑥-2 代房东确认（银行/运营代操作）",
            description = "对 SUBMITTED（待确认）状态的保函申请代房东确认并电子签署（记录 ADMIN_AGENT_CONFIRM），随后自动触发 AI 合同复审（G-3）。")
    @PutMapping("/guarantee/{id}/landlord-confirm")
    public Result<GuaranteeApplicationVO> adminLandlordConfirm(
            @PathVariable Long id,
            @Parameter(description = "代签内容（可选，默认 ADMIN_AGENT_CONFIRM）")
            @RequestParam(required = false) String signContent) {
        return Result.success(guaranteeService.adminLandlordConfirm(UserContext.getUserId(), id, signContent));
    }

    // ============================================================
    //  ⑦ 注册审核记录（白名单 AI 审核留痕）
    // ============================================================

    @Operation(summary = "⑦ 注册审核记录分页（白名单 AI 审核留痕）",
            description = "全量分页查询 biz_registration_review；支持关键词（用户名/姓名/手机号/审核编号）与结论（APPROVED/REJECTED）筛选。")
    @GetMapping("/registration-reviews")
    public Result<Page<com.icbc.qingqi.module.user.entity.BizRegistrationReview>> registrationReviews(
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "10") int pageSize,
            @Parameter(description = "关键词：用户名/姓名/手机号/审核编号")
            @RequestParam(required = false) String keyword,
            @Parameter(description = "审核结论：APPROVED/REJECTED")
            @RequestParam(required = false) String result) {
        return Result.success(adminService.pageRegistrationReviews(pageNum, pageSize, keyword, result));
    }

    // ============================================================
    //  ⑧ 仪表盘统计（管理端数据看板）
    // ============================================================

    @Operation(summary = "⑧ 仪表盘统计",
            description = "返回管理端数据看板核心指标：用户总数/今日新增、保函申请总数/待房东确认/人工复审待办、贷款待审批、预警未处理、注册审核记录数。")
    @GetMapping("/dashboard/stats")
    public Result<Map<String, Object>> dashboardStats() {
        return Result.success(adminService.dashboardStats());
    }

    // ============================================================
    //  简化入参校验工具
    // ============================================================
    // (内部辅助方法如需要可在此扩展)
}
