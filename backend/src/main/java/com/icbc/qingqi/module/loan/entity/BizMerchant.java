package com.icbc.qingqi.module.loan.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 商户信息实体
 * <p>
 * 对应表：biz_merchant
 * 受托支付定向打款目标
 */
@Data
@TableName("biz_merchant")
public class BizMerchant {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String merchantName;

    /** 商户类型：MATERIAL/STALL/PROMOTION/OTHER */
    private String merchantType;

    private String contactName;
    private String contactPhone;
    private String address;
    private String businessLicense;
    private String bankAccount;
    private String bankName;

    /** 认证状态 */
    private String verifyStatus;

    /** 状态：0 禁用 / 1 正常 */
    private Integer status;

    @TableLogic
    private Integer deleted;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
