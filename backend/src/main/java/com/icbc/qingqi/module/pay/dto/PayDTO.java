package com.icbc.qingqi.module.pay.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/** 收银台支付 DTO */
@Data
public class PayDTO {

    /** 支付方式：WALLET钱包余额/CARD模拟银行卡/SIM_BANK工行e支付（模拟） */
    @NotBlank(message = "支付方式不能为空")
    private String payMethod;

    /** 支付密码（模拟6位，默认123456） */
    private String password;
}
