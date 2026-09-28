package com.icbc.qingqi.module.pay.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 商户收款账户实体（模拟）
 * 对应表：pay_merchant_account
 */
@Data
@TableName("pay_merchant_account")
public class PayMerchantAccount {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long merchantId;

    /** 收款余额（模拟） */
    private BigDecimal balance;

    /** 累计收款 */
    private BigDecimal totalIncome;

    private String status;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
