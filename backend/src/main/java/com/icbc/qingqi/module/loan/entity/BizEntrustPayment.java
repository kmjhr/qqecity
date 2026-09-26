package com.icbc.qingqi.module.loan.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 受托支付实体
 * <p>
 * 对应表：biz_entrust_payment
 * 100% 定向打给指定商户，资金不经过个人账户
 */
@Data
@TableName("biz_entrust_payment")
public class BizEntrustPayment {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 支付编号 */
    private String paymentNo;

    /** 借款人 ID */
    private Long userId;

    /** 贷款申请 ID */
    private Long loanApplicationId;

    /** 收款商户 ID */
    private Long merchantId;

    /** 商户名称（冗余） */
    private String merchantName;

    /** 支付金额 */
    private BigDecimal amount;

    /** 用途说明 */
    private String purpose;

    /** 交易凭证 URL */
    private String tradeProof;

    /** 支付状态：PENDING/PROCESSING/SUCCESS/FAILED */
    private String paymentStatus;

    /** 支付完成时间 */
    private LocalDateTime paymentTime;

    /** 失败原因 */
    private String failReason;

    @TableLogic
    private Integer deleted;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
