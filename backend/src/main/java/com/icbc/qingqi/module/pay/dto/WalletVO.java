package com.icbc.qingqi.module.pay.dto;

import lombok.Data;

import java.math.BigDecimal;

/** 钱包视图 */
@Data
public class WalletVO {
    private Long walletId;
    private Long userId;
    private BigDecimal balance;
    private BigDecimal frozen;
    private boolean hasPassword;
    /** 演示提示 */
    private String remark;
}
