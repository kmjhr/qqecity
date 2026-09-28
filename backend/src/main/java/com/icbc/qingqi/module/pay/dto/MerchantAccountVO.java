package com.icbc.qingqi.module.pay.dto;

import lombok.Data;

import java.math.BigDecimal;

/** 商户收款账户视图 */
@Data
public class MerchantAccountVO {
    private Long id;
    private Long merchantId;
    private String merchantName;
    private BigDecimal balance;
    private BigDecimal totalIncome;
    private String status;
}
