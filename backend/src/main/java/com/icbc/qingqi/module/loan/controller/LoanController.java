package com.icbc.qingqi.module.loan.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.icbc.qingqi.common.Result;
import com.icbc.qingqi.module.loan.dto.*;
import com.icbc.qingqi.module.loan.entity.BizLoanApplication;
import com.icbc.qingqi.module.loan.entity.BizMerchant;
import com.icbc.qingqi.module.loan.service.LoanService;
import com.icbc.qingqi.security.UserContext;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 模块2 - 轻创业智能授信
 * <p>
 * 路径：/api/v1/loan/**
 * 覆盖 L-1 ~ L-3：B类免费预审 → A/B双轨额度 → 受托支付
 * <p>
 * 所有银行能力（预审、授信、支付）均为模拟桩，演示数据标注"模拟"。
 */
@Tag(name = "轻创业智能授信")
@RestController
@RequestMapping("/v1/loan")
public class LoanController {

    private final LoanService loanService;

    public LoanController(LoanService loanService) {
        this.loanService = loanService;
    }

    // ============================================================
    //  L-0 产品介绍：A/B 双轨产品规则与风险揭示
    // ============================================================

    @Operation(summary = "青创e贷 A/B 双轨产品规则",
            description = "返回A类（创业信用画像循环贷）与B类（小额定向两步式受托支付贷）完整产品要素、准入规则、资金流向与风险提示，供前端产品介绍页展示。")
    @GetMapping("/product-rules")
    public Result<LoanProductRulesVO> productRules() {
        return Result.success(loanService.getProductRules());
    }

    // ============================================================
    //  L-1 B 类免费预审（不查征信）
    // ============================================================

    @Operation(summary = "L-1 B类免费预审",
            description = "不查征信，按创业计划+人群资质规则给出1—2万元额度区间。")
    @PostMapping("/precheck")
    public Result<LoanPrecheckVO> precheck(@Valid @RequestBody LoanPrecheckDTO dto) {
        Long userId = UserContext.getUserId();
        return Result.success(loanService.precheck(userId, dto));
    }

    // ============================================================
    //  L-2 A/B 双轨额度展示
    // ============================================================

    @Operation(summary = "L-2 查询我的A/B双轨授信额度",
            description = "A类最高5万循环额度（随借随还）、B类小额定向（受托支付）。")
    @GetMapping("/credit")
    public Result<List<CreditLimitVO>> credit() {
        Long userId = UserContext.getUserId();
        return Result.success(loanService.myCreditLimits(userId));
    }

    // ============================================================
    //  L-3 受托支付
    // ============================================================

    @Operation(summary = "L-3 受托支付",
            description = "100%定向打给预置商户，资金不经过借款人个人账户，生成受托支付流水。")
    @PostMapping("/entrust-pay")
    public Result<EntrustPaymentVO> entrustPay(@Valid @RequestBody EntrustPayDTO dto) {
        Long userId = UserContext.getUserId();
        return Result.success(loanService.entrustPay(userId, dto));
    }

    // ============================================================
    //  辅助：商户列表 / 申请列表 / 申请详情
    // ============================================================

    @Operation(summary = "查询受托支付商户列表",
            description = "预置商户，受托支付定向打款目标。")
    @GetMapping("/merchants")
    public Result<List<BizMerchant>> merchants() {
        return Result.success(loanService.listMerchants());
    }

    @Operation(summary = "分页查询我的贷款申请")
    @GetMapping("/applications")
    public Result<Page<BizLoanApplication>> applications(
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "10") int pageSize,
            @RequestParam(required = false) String status) {
        Long userId = UserContext.getUserId();
        return Result.success(loanService.pageApplications(userId, pageNum, pageSize, status));
    }

    @Operation(summary = "查询贷款申请详情")
    @GetMapping("/applications/{id}")
    public Result<BizLoanApplication> applicationDetail(
            @Parameter(description = "申请ID") @PathVariable Long id) {
        Long userId = UserContext.getUserId();
        return Result.success(loanService.getApplication(userId, id));
    }

    // ============================================================
    //  L-补1 A类循环贷随借随还（缺口 #10）
    // ============================================================

    @Operation(summary = "A类循环贷提款",
            description = "从A类5万循环额度中分次提款，按日计息（年化3.85%模拟）。")
    @PostMapping("/withdraw")
    public Result<CreditTxnVO> withdraw(@Valid @RequestBody WithdrawDTO dto) {
        Long userId = UserContext.getUserId();
        return Result.success(loanService.withdraw(userId, dto));
    }

    @Operation(summary = "A类循环贷还款",
            description = "归还后额度自动恢复，按实际用款天数和利率计息（年化3.85%模拟）。")
    @PostMapping("/repay")
    public Result<CreditTxnVO> repay(@Valid @RequestBody RepayDTO dto) {
        Long userId = UserContext.getUserId();
        return Result.success(loanService.repay(userId, dto));
    }

    @Operation(summary = "还款试算预览（A/B双轨）",
            description = "返回待还本金、计息起始日、已计息天数、预估利息与应还合计，供还款弹窗展示。creditType=A_TYPE|B_TYPE")
    @GetMapping("/repay-preview")
    public Result<RepayPreviewVO> repayPreview(
            @Parameter(description = "额度类型 A_TYPE/B_TYPE") @RequestParam(defaultValue = "A_TYPE") String creditType) {
        Long userId = UserContext.getUserId();
        return Result.success(loanService.repayPreview(userId, creditType));
    }

    @Operation(summary = "B类受托支付还款",
            description = "归还B类定向贷款本金+利息（年化4.35%模拟，自最早受托支付日起按日计息），额度自动恢复。")
    @PostMapping("/entrust-repay")
    public Result<CreditTxnVO> entrustRepay(@Valid @RequestBody RepayDTO dto) {
        Long userId = UserContext.getUserId();
        return Result.success(loanService.entrustRepay(userId, dto));
    }

    @Operation(summary = "查询循环贷流水", description = "提款/还款流水列表")
    @GetMapping("/credit-txns")
    public Result<List<CreditTxnVO>> creditTxns() {
        Long userId = UserContext.getUserId();
        return Result.success(loanService.listCreditTxns(userId));
    }

    // ============================================================
    //  L-补2 B转A观察期（缺口 #11）
    // ============================================================

    @Operation(summary = "查询B转A观察期状态",
            description = "查询B类授信的观察期进度、评分、剩余月数。")
    @GetMapping("/observation")
    public Result<ObservationVO> observationStatus() {
        Long userId = UserContext.getUserId();
        return Result.success(loanService.getObservationStatus(userId));
    }

    @Operation(summary = "模拟月份推进（加速观察）",
            description = "每次调用推进1个月，根据受托支付+记账+还款数据计算月度评分。6个月后达标（≥60分）转A类+提额，不达标维持B类。")
    @PostMapping("/observation/advance")
    public Result<ObservationVO> advanceObservation() {
        Long userId = UserContext.getUserId();
        return Result.success(loanService.advanceObservation(userId));
    }

    // ============================================================
    //  L-补3 商户白名单管理（缺口 #6）
    // ============================================================

    @Operation(summary = "按认证状态筛选商户列表",
            description = "verifyStatus: VERIFIED-白名单 / PENDING-灰名单 / REJECTED-已拒绝，不传则返回全部。")
    @GetMapping("/merchants/by-status")
    public Result<List<BizMerchant>> merchantsByStatus(
            @RequestParam(required = false) String verifyStatus) {
        return Result.success(loanService.listMerchantsByVerifyStatus(verifyStatus));
    }

    @Operation(summary = "banker审核商户白名单",
            description = "banker01对商户进行白名单审核：VERIFIED-加入白名单 / REJECTED-拒绝。")
    @PutMapping("/merchants/{id}/audit")
    public Result<BizMerchant> auditMerchant(
            @PathVariable Long id,
            @RequestParam String verifyStatus) {
        return Result.success(loanService.auditMerchant(id, verifyStatus));
    }
}
