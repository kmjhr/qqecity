package com.icbc.qingqi.module.pay.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 支付订单实体（模拟）
 * 对应表：pay_order
 */
@Data
@TableName("pay_order")
public class PayOrder {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String orderNo;

    private Long userId;

    /** 业务类型：GUARANTEE_FEE/LOAN_REPAY/ENTRUST_PAY/MERCHANT_CONSUME/RECHARGE/REFUND */
    private String bizType;

    private Long bizId;

    private Long merchantId;

    private String subject;

    private BigDecimal amount;

    /** 支付方式：WALLET/CARD/SIM_BANK */
    private String payMethod;

    /** 状态：PENDING_PAY/PAID/CLOSED/REFUNDED */
    private String status;

    private LocalDateTime payTime;

    private LocalDateTime closeTime;

    /** 支付超时时间（默认15分钟） */
    private LocalDateTime expireTime;

    private String remark;

    @TableLogic
    private Integer deleted;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
