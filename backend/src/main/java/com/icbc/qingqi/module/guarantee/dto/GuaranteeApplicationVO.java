package com.icbc.qingqi.module.guarantee.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 保函申请 VO（列表/详情返回）
 */
@Data
public class GuaranteeApplicationVO {

    private Long id;
    private String applyNo;
    private Long tenantId;
    private Long houseId;
    private Long contractId;
    private Long landlordId;
    private BigDecimal depositAmount;
    private BigDecimal guaranteeRate;
    private BigDecimal guaranteeFee;
    private Integer guaranteePeriodMonths;
    private String applicantName;
    private String applicantPhone;
    private String landlordName;
    private String landlordPhone;
    /** 申请状态 */
    private String applyStatus;
    /** AI 复审结果 */
    private String aiReviewResult;
    private Integer aiReviewScore;
    private String aiReviewDetail;
    private String rejectReason;
    private LocalDateTime submitTime;
    private LocalDateTime landlordConfirmTime;
    private LocalDateTime reviewTime;
    private LocalDateTime createTime;
}
