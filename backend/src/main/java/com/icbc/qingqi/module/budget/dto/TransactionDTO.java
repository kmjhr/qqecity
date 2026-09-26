package com.icbc.qingqi.module.budget.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 模拟交易 DTO（C-2）
 * <p>
 * 按 MCC 码自动归类并实时扣减预算
 */
@Data
public class TransactionDTO {

    /** 交易类型：INCOME/EXPENSE */
    @NotBlank(message = "交易类型不能为空")
    private String transactionType;

    /** 金额 */
    @NotNull(message = "交易金额不能为空")
    @DecimalMin(value = "0.01", message = "交易金额必须大于0")
    private BigDecimal amount;

    /** 商户名称 */
    private String merchantName;

    /** MCC 码（用于自动归类） */
    private String mccCode;

    /** 交易描述 */
    private String description;
}
