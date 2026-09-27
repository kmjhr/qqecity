package com.icbc.qingqi.module.cashflow.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 聚合流水单条记录（模拟）
 */
@Data
public class AggregateTxnVO {

    /** 交易编号（模拟） */
    private String txnNo;

    /** 渠道：ICBC_QR/WECHAT/ALIPAY/TAOBAO */
    private String channelCode;

    /** 渠道名称 */
    private String channelName;

    /** 对方账户/买家昵称（脱敏） */
    private String counterparty;

    /** 交易类型：INCOME/REFUND */
    private String txnType;

    /** 金额 */
    private BigDecimal amount;

    /** 交易时间 */
    private LocalDateTime txnTime;

    /** 商品/订单描述 */
    private String description;

    /** 来源标注（始终"已授权聚合"） */
    private String sourceTag;

    /** 是否模拟 */
    private Boolean simulated;
}
