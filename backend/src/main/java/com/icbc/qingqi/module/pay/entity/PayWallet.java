package com.icbc.qingqi.module.pay.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 用户钱包实体（模拟）
 * 对应表：pay_wallet
 */
@Data
@TableName("pay_wallet")
public class PayWallet {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    /** 可用余额（模拟） */
    private BigDecimal balance;

    /** 冻结金额（支付中） */
    private BigDecimal frozen;

    /** 支付密码（模拟，默认123456） */
    private String payPassword;

    /** 状态：ACTIVE/FROZEN/CLOSED */
    private String status;

    @TableLogic
    private Integer deleted;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
