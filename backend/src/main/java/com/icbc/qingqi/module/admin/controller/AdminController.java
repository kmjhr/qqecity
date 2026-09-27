package com.icbc.qingqi.module.admin.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.icbc.qingqi.common.Result;
import com.icbc.qingqi.module.admin.dto.LoanReviewDTO;
import com.icbc.qingqi.module.admin.dto.WarningHandleDTO;
import com.icbc.qingqi.module.admin.service.AdminService;
import com.icbc.qingqi.module.guarantee.entity.BizGuaranteeApplication;
import com.icbc.qingqi.module.loan.entity.BizCreditTxn;
import com.icbc.qingqi.module.loan.entity.BizEntrustPayment;
import com.icbc.qingqi.module.loan.entity.BizLoanApplication;
import com.icbc.qingqi.module.risk.entity.BizRiskWarning;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

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

    public AdminController(AdminService adminService) {
        this.adminService = adminService;
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
    //  简化入参校验工具
    // ============================================================
    // (内部辅助方法如需要可在此扩展)
}
