package com.icbc.qingqi.module.loan.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 受托支付 DTO（L-3）
 * <p>
 * 100% 定向打给预置商户，资金不经过个人账户
 */
@Data
public class EntrustPayDTO {

    /** 贷款申请 ID */
    @NotNull(message = "贷款申请ID不能为空")
    private Long loanApplicationId;

    /** 收款商户 ID（biz_merchant） */
    @NotNull(message = "收款商户不能为空")
    private Long merchantId;

    /** 支付金额 */
    @NotNull(message = "支付金额不能为空")
    @DecimalMin(value = "0.01", message = "支付金额必须大于0")
    private BigDecimal amount;

    /** 用途说明 */
    private String purpose;

    /** 交易凭证 URL */
    private String tradeProof;
}
