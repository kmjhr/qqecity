package com.icbc.qingqi.module.loan.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 循环贷交易流水 VO
 */
@Data
public class CreditTxnVO {

    private Long id;
    private String txnNo;
    private Long creditLimitId;
    private String txnType;
    private String txnTypeName;
    private String creditTypeName;
    private String txnDesc;
    private BigDecimal principalAmount;
    private BigDecimal interestAmount;
    private Integer borrowDays;
    private BigDecimal balanceAfter;
    private String remark;
    private LocalDateTime txnTime;
}
