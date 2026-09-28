package com.icbc.qingqi.module.loan.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

/**
 * A类循环贷还款 DTO
 */
@Data
public class RepayDTO {

    @NotNull(message = "还款金额不能为空")
    @DecimalMin(value = "0.01", message = "还款金额必须大于0")
    private BigDecimal amount;

    @io.swagger.v3.oas.annotations.media.Schema(description = "指定结清的借款编号（A类=CW流水号/B类=EP受托支付号，不传则按先进先出冲抵）")
    private String loanNo;

    @io.swagger.v3.oas.annotations.media.Schema(description = "金额口径：PRINCIPAL=本金（默认，利息按笔自动结算）/ TOTAL=本息合计（输入含息金额，自动拆分本金+利息）")
    private String amountType;
}
