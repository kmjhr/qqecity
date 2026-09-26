package com.icbc.qingqi.module.guarantee.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 保函申请实体
 * <p>
 * 对应表：biz_guarantee_application
 * 状态流转：SUBMITTED → LANDLORD_CONFIRM → AI_REVIEW → (MANUAL_REVIEW) → PENDING_PAY → APPROVED
 *                                                          ↘ REJECTED
 */
@Data
@TableName("biz_guarantee_application")
public class BizGuaranteeApplication {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 申请编号 */
    private String applyNo;

    /** 申请人（租客）ID */
    private Long tenantId;

    /** 房屋 ID */
    private Long houseId;

    /** 租赁合同 ID */
    private Long contractId;

    /** 房东 ID */
    private Long landlordId;

    /** 押金金额（保函金额） */
    private BigDecimal depositAmount;

    /** 保函费率 */
    private BigDecimal guaranteeRate;

    /** 保函费 */
    private BigDecimal guaranteeFee;

    /** 保函期限（月） */
    private Integer guaranteePeriodMonths;

    /** 申请人姓名 */
    private String applicantName;

    /** 申请人电话 */
    private String applicantPhone;

    /** 房东姓名 */
    private String landlordName;

    /** 房东电话 */
    private String landlordPhone;

    /** 申请状态：SUBMITTED/LANDLORD_CONFIRM/AI_REVIEW/MANUAL_REVIEW/PENDING_PAY/APPROVED/REJECTED */
    private String applyStatus;

    /** AI 复审结果：PASS/RISK_WARNING/MANUAL_REVIEW */
    private String aiReviewResult;

    /** AI 复审评分 */
    private Integer aiReviewScore;

    /** AI 复审详情（JSON） */
    private String aiReviewDetail;

    /** 拒绝原因 */
    private String rejectReason;

    /** 提交时间 */
    private LocalDateTime submitTime;

    /** 房东确认时间 */
    private LocalDateTime landlordConfirmTime;

    /** 审核时间 */
    private LocalDateTime reviewTime;

    @TableLogic
    private Integer deleted;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
