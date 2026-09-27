package com.icbc.qingqi.module.loan.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

/**
 * A类循环贷提款 DTO
 */
@Data
public class WithdrawDTO {

    @NotNull(message = "提款金额不能为空")
    @DecimalMin(value = "0.01", message = "提款金额必须大于0")
    private BigDecimal amount;

    /** 提款用途 */
    private String purpose;
}
