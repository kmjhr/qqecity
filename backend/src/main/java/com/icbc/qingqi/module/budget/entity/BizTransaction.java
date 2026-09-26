package com.icbc.qingqi.module.budget.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 交易记录实体
 * <p>
 * 对应表：biz_transaction
 * 按 MCC 码自动归类并扣减对应预算
 */
@Data
@TableName("biz_transaction")
public class BizTransaction {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    private String transactionNo;

    /** 交易类型：INCOME/EXPENSE */
    private String transactionType;

    private Long categoryId;

    private String categoryCode;

    private BigDecimal amount;

    private String merchantName;

    private String mccCode;

    private LocalDateTime transactionTime;

    private String description;

    /** 来源：SIMULATED/BANK_IMPORT */
    private String source;

    private Long budgetId;

    @TableLogic
    private Integer deleted;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
