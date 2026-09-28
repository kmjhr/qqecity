package com.icbc.qingqi.module.loan.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 受托支付流水 VO（L-3）
 */
@Data
public class EntrustPaymentVO {

    /** 流水 ID */
    private Long id;

    /** 支付编号 */
    private String paymentNo;

    /** 借款人 ID */
    private Long userId;

    /** 贷款申请 ID */
    private Long loanApplicationId;

    /** 收款商户 ID */
    private Long merchantId;

    /** 商户名称 */
    private String merchantName;

    /** 支付金额 */
    private BigDecimal amount;

    /** 用途说明 */
    private String purpose;

    /** 支付状态：PENDING / PROCESSING / SUCCESS / FAILED */
    private String paymentStatus;

    /** 支付状态展示名 */
    private String paymentStatusName;

    /** 支付完成时间 */
    private LocalDateTime paymentTime;

    /** 资金路径说明（定向打款，不经过个人账户） */
    private String fundPath;

    /** 是否待银行复核（自定义商户每单复核：true=已提交复核单，未放款） */
    private Boolean pendingReview;

    /** 复核单号（自定义商户提交后生成） */
    private String reviewNo;

    /** 复核状态：PENDING待复核 / APPROVED已通过 / REJECTED已驳回 */
    private String reviewStatus;

    /** 复核状态展示名 */
    private String reviewStatusName;
}
