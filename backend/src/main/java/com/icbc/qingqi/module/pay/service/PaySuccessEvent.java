package com.icbc.qingqi.module.pay.service;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 支付成功事件（Spring 事件）
 * 供业务模块监听：保函缴费开函（GUARANTEE_FEE）等
 */
@Data
@AllArgsConstructor
public class PaySuccessEvent {
    private String orderNo;
    private String bizType;
    private Long bizId;
    private Long userId;
    private BigDecimal amount;
    private String payMethod;
    private Long merchantId;
}
