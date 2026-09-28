package com.icbc.qingqi.module.pay.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

/** 模拟充值 DTO */
@Data
public class RechargeDTO {

    @NotNull(message = "充值金额不能为空")
    @DecimalMin(value = "0.01", message = "充值金额必须大于0")
    private BigDecimal amount;

    /** 充值方式：CARD银行卡/SIM_BANK工行e支付（模拟） */
    private String method;
}
