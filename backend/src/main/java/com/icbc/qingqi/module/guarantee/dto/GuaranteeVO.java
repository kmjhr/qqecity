package com.icbc.qingqi.module.guarantee.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 保函 VO（保函详情返回）
 */
@Data
public class GuaranteeVO {

    private Long id;
    private String guaranteeNo;
    private Long applicationId;
    private Long tenantId;
    private Long landlordId;
    private Long houseId;
    private BigDecimal guaranteeAmount;
    private BigDecimal guaranteeFee;
    private LocalDate effectiveDate;
    private LocalDate expireDate;
    /** 保函状态：ACTIVE/EXPIRED/CLAIMED/TERMINATED */
    private String guaranteeStatus;
    /** 缴费状态：UNPAID/PAID/REFUNDED */
    private String payStatus;
    private LocalDateTime payTime;
    private LocalDateTime issueTime;
    private LocalDateTime createTime;
}
