package com.icbc.qingqi.module.loan.dto;

import lombok.Data;

import java.math.BigDecimal;

/**
 * B转A观察期 VO
 */
@Data
public class ObservationVO {

    private Long creditLimitId;
    private String creditType;
    private String observationStatus;
    private String observationStatusName;
    private Integer observationMonths;
    private Integer observationScore;
    private Integer monthsRemaining;
    private Integer promotionThreshold;
    private String currentMonthDetail;
    private BigDecimal totalLimit;
    private BigDecimal availableLimit;
    /** 是否已转A类 */
    private Boolean promoted;
    /** 转A后新额度（如已转A） */
    private BigDecimal promotedLimit;
}
