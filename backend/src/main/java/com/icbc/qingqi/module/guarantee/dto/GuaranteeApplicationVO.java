package com.icbc.qingqi.module.guarantee.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 保函申请 VO（列表 + 详情共用）
 */
@Data
public class GuaranteeApplicationVO {

    /** 申请 ID */
    private Long id;

    /** 申请编号 */
    private String applyNo;

    /** 申请人（租客）ID */
    private Long tenantId;

    /** 申请人姓名 */
    private String applicantName;

    /** 申请人电话 */
    private String applicantPhone;

    /** 房东 ID */
    private Long landlordId;

    /** 房东姓名 */
    private String landlordName;

    /** 房东电话 */
    private String landlordPhone;

    /** 房屋 ID */
    private Long houseId;

    /** 房屋标题 */
    private String houseTitle;

    /** 房屋详细地址 */
    private String houseAddress;

    /** 租赁合同 ID */
    private Long contractId;

    /** 合同编号 */
    private String contractNo;

    /** 月租金 */
    private BigDecimal monthlyRent;

    /** 押金金额（保函金额） */
    private BigDecimal depositAmount;

    /** 保函费率 */
    private BigDecimal guaranteeRate;

    /** 保函费 */
    private BigDecimal guaranteeFee;

    /** 保函期限（月） */
    private Integer guaranteePeriodMonths;

    /** 租期开始日 */
    private LocalDate rentStartDate;

    /** 租期结束日 */
    private LocalDate rentEndDate;

    /** 申请状态（DB 原值） */
    private String applyStatus;

    /** 申请状态展示名（5 态：申请中/待确认/待缴费/已开立/已失效） */
    private String statusName;

    /** AI 复审结果：PASS/RISK_WARNING/MANUAL_REVIEW */
    private String aiReviewResult;

    /** AI 复审评分 */
    private Integer aiReviewScore;

    /** AI 复审详情（JSON 文本，含"模拟"标注） */
    private String aiReviewDetail;

    /** 拒绝原因 */
    private String rejectReason;

    /** 提交时间 */
    private LocalDateTime submitTime;

    /** 房东确认时间 */
    private LocalDateTime landlordConfirmTime;

    /** 电子签名内容（Canvas base64 或 CLICK_CONFIRM） */
    private String signContent;

    /** 电子签署时间 */
    private LocalDateTime signTime;

    /** 审核时间 */
    private LocalDateTime reviewTime;

    /** 关联电子保函 ID（已开立时有值） */
    private Long guaranteeId;

    /** 保函编号（已开立时有值） */
    private String guaranteeNo;
}
