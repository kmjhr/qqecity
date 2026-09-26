package com.icbc.qingqi.module.loan;

import com.icbc.qingqi.common.Result;
import com.icbc.qingqi.security.UserContext;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * 轻创业智能授信模块接口（青创e贷 A/B）
 */
@RestController
@RequestMapping("/api/v1/loan")
public class LoanController {

    private final LoanService loanService;

    public LoanController(LoanService loanService) {
        this.loanService = loanService;
    }

    @Data
    public static class ApplyRequest {
        @NotBlank(message = "贷款类型不能为空")
        private String loanType;
        @NotNull(message = "申请金额不能为空")
        private BigDecimal applyAmount;
        @NotBlank(message = "贷款用途不能为空")
        private String loanPurpose;
        private String bizPlanUrl;
    }

    @Data
    public static class PayRequest {
        @NotBlank(message = "收款商户不能为空")
        private String merchantName;
        @NotNull(message = "支付金额不能为空")
        private BigDecimal amount;
    }

    /** 申请 + B类免费预审（不查征信） */
    @PostMapping("/apply")
    public Result<Long> apply(@Valid @RequestBody ApplyRequest req) {
        Long id = loanService.apply(UserContext.requireUserId(), req.getLoanType(),
                req.getApplyAmount(), req.getLoanPurpose(), req.getBizPlanUrl());
        return Result.ok(id);
    }

    /** 模拟审批 */
    @PostMapping("/{applyId}/approve")
    public Result<LoanApply> approve(@PathVariable Long applyId) {
        return Result.ok(loanService.approve(applyId));
    }

    /** 100%受托支付 */
    @PostMapping("/{applyId}/entrusted-pay")
    public Result<EntrustedPayment> entrustedPay(@PathVariable Long applyId,
                                                 @Valid @RequestBody PayRequest req) {
        return Result.ok(loanService.entrustedPay(applyId, req.getMerchantName(), req.getAmount()));
    }

    @GetMapping("/list")
    public Result<List<LoanApply>> list() {
        return Result.ok(loanService.listByUser(UserContext.requireUserId()));
    }

    @GetMapping("/{applyId}")
    public Result<Map<String, Object>> detail(@PathVariable Long applyId) {
        return Result.ok(loanService.detail(applyId));
    }
}
