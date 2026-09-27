package com.icbc.qingqi.module.policy.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class PolicyVO {

    private Long id;
    private String policyNo;
    private String policyName;
    private String policyType;
    private String policyTypeName;
    private List<String> targetCrowdList;
    private BigDecimal maxAmount;
    private String subsidyRate;
    private String conditions;
    private String applyUrl;
    private String policySource;
    private Integer sortOrder;
    /** 是否匹配当前用户人群 */
    private Boolean matched;
    /** 匹配说明 */
    private String matchReason;
}
