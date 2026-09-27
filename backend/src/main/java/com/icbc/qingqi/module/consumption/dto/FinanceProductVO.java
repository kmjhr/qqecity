package com.icbc.qingqi.module.consumption.dto;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 理财产品 VO
 * <p>
 * 合规口径：理财非存款、产品有风险、工行仅代销
 */
@Data
public class FinanceProductVO {

    private Long id;
    private String productCode;
    private String productName;
    private String productType;
    private String productTypeName;
    private String riskLevel;
    private String riskLevelName;
    private String expectedReturn;
    private BigDecimal minAmount;
    private String period;
    private String productElements;
    private String riskDisclosure;
    private String applyUrl;
    private String targetRiskLevel;
    private Integer sortOrder;

    /** 当前用户是否匹配（按风险等级） */
    private Boolean matched;

    /** 合规声明 */
    private String complianceNotice;
}
