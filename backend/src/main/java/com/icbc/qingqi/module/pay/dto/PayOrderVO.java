package com.icbc.qingqi.module.pay.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** 支付订单视图 */
@Data
public class PayOrderVO {
    private Long id;
    private String orderNo;
    private Long userId;
    private String bizType;
    private String bizTypeName;
    private Long bizId;
    private Long merchantId;
    private String merchantName;
    private String subject;
    private BigDecimal amount;
    private String payMethod;
    private String payMethodName;
    private String status;
    private String statusName;
    private LocalDateTime payTime;
    private LocalDateTime closeTime;
    private LocalDateTime expireTime;
    private String remark;
    private LocalDateTime createTime;
}
