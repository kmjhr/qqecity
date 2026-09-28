package com.icbc.qingqi.module.pay.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** 支付流水视图 */
@Data
public class PayTxnVO {
    private Long id;
    private String txnNo;
    private String orderNo;
    private String direction;
    private BigDecimal amount;
    private BigDecimal balanceAfter;
    private String payMethod;
    private String bizType;
    private String bizTypeName;
    private String relatedNo;
    private String remark;
    private LocalDateTime createTime;
}
