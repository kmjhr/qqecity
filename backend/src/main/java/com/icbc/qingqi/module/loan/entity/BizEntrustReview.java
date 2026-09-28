package com.icbc.qingqi.module.loan.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 受托支付复核单实体
 * <p>
 * 对应表：biz_entrust_review
 * 用户自定义商户（USER_CUSTOM）每次受托支付前须银行复核：PENDING → APPROVED 放款 / REJECTED 驳回
 */
@Data
@TableName("biz_entrust_review")
public class BizEntrustReview {

    /** 主键 ID */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 复核单号（ERR+时间戳） */
    private String reviewNo;

    /** 借款人 ID */
    private Long userId;

    /** 贷款申请 ID */
    private Long loanApplicationId;

    /** 收款商户 ID（用户自定义商户） */
    private Long merchantId;

    /** 商户名称（快照） */
    private String merchantName;

    /** 打款金额 */
    private BigDecimal amount;

    /** 用途说明 */
    private String purpose;

    /** 交易凭证说明 */
    private String tradeProof;

    /** 状态：PENDING待复核 / APPROVED已通过 / REJECTED已驳回 */
    private String status;

    /** 复核人 ID（banker） */
    private Long reviewerId;

    /** 复核意见/驳回原因 */
    private String reviewRemark;

    /** 复核时间 */
    private LocalDateTime reviewTime;

    /** 放款流水 ID（复核通过后生成的受托支付流水） */
    private Long paymentId;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
