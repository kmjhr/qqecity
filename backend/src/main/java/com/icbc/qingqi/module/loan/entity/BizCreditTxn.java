package com.icbc.qingqi.module.loan.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 循环贷交易流水实体
 * <p>
 * 对应表：biz_credit_txn
 * A类循环贷提款/还款流水，按日计息
 */
@Data
@TableName("biz_credit_txn")
public class BizCreditTxn {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 交易编号 */
    private String txnNo;

    /** 用户 ID */
    private Long userId;

    /** 授信额度 ID */
    private Long creditLimitId;

    /** 交易类型：WITHDRAW-提款 / REPAY-还款 */
    private String txnType;

    /** 本金金额 */
    private BigDecimal principalAmount;

    /** 利息金额（还款时计算） */
    private BigDecimal interestAmount;

    /** 计息天数（还款时计算） */
    private Integer borrowDays;

    /** 交易后可用额度 */
    private BigDecimal balanceAfter;

    /**
     * 还款目标借款编号（仅指定结清某笔的 REPAY 流水记录；FIFO 还款为 null）
     */
    private String targetLoanNo;

    /** 备注 */
    private String remark;

    /** 交易时间 */
    private LocalDateTime txnTime;

    @TableLogic
    private Integer deleted;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
