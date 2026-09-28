package com.icbc.qingqi.module.pay.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 支付流水实体（模拟）
 * 对应表：pay_transaction
 */
@Data
@TableName("pay_transaction")
public class PayTransaction {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String txnNo;

    private Long orderId;

    private String orderNo;

    /** 用户ID（商户侧可为空） */
    private Long userId;

    /** 商户ID（用户侧可为空） */
    private Long merchantId;

    /** 方向：IN收入/OUT支出 */
    private String direction;

    private BigDecimal amount;

    /** 交易后余额（对账展示） */
    private BigDecimal balanceAfter;

    private String payMethod;

    private String status;

    private String bizType;

    /** 关联业务单号（保函编号/还款流水号/受托支付号） */
    private String relatedNo;

    private String remark;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
