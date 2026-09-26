package com.icbc.qingqi.module.guarantee.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 保函申请实体
 * <p>
 * 对应表：biz_guarantee_application
 * 整条安居保函链路的入口，关联租客、房屋、租赁合同、房东
 * <p>
 * 申请状态流转（apply_status）：
 * SUBMITTED-申请中 → LANDLORD_CONFIRM-待房东确认 → AI_REVIEW-复审中 →
 * MANUAL_REVIEW-人工复审 → PENDING_PAY-待缴费 → APPROVED-已开立 / REJECTED-已拒绝
 * <p>
 * AI 复审结果（ai_review_result）：
 * PASS-通过 / RISK_WARNING-风险提示 / MANUAL_REVIEW-转人工
 */
@Data
@TableName("biz_guarantee_application")
public class BizGuaranteeApplication {

    /** 主键 ID */
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

    /** 逻辑删除：0-存在，1-删除 */
    @TableLogic
    private Integer deleted;

    /** 创建时间 */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /** 更新时间 */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
