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
}
