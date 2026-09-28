package com.icbc.qingqi.module.loan.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 商户信息实体
 * <p>
 * 对应表：biz_merchant
 * 受托支付定向打款目标，预置数据，不经过个人账户
 */
@Data
@TableName("biz_merchant")
public class BizMerchant {

    /** 主键 ID */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 商户名称 */
    private String merchantName;

    /** 商户类型：MATERIAL-物料 / STALL-摊位 / PROMOTION-推广 / OTHER-其他 */
    private String merchantType;

    /** 联系人 */
    private String contactName;

    /** 联系电话 */
    private String contactPhone;

    /** 地址 */
    private String address;

    /** 营业执照号 */
    private String businessLicense;

    /** 收款账号 */
    private String bankAccount;

    /** 开户银行 */
    private String bankName;

    /** 认证状态：VERIFIED白名单 / PENDING待审 / REJECTED已拒绝 */
    private String verifyStatus;

    /** 商户来源：SYSTEM预置 / USER_CUSTOM用户自定义 */
    private String merchantSource;

    /** 申请人用户ID（用户自定义商户） */
    private Long applicantUserId;

    /** 申请说明 */
    private String applyRemark;

    /** 审核意见/驳回原因 */
    private String reviewRemark;

    /** 审核人ID（banker） */
    private Long reviewerId;

    /** 审核时间 */
    private LocalDateTime reviewTime;

    /** 状态：0-禁用，1-正常 */
    private Integer status;

    /** 逻辑删除 */
    @TableLogic
    private Integer deleted;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
