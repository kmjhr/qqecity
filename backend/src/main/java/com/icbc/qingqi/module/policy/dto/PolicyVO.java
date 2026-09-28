package com.icbc.qingqi.module.policy.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
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
    /** 申报入口（真实官方申报链接，前端可跳转） */
    private String applyUrl;
    /** 政策来源 */
    private String policySource;
    /** 政策生效日期 */
    private LocalDate validFrom;
    /** 政策失效日期（NULL=长期有效） */
    private LocalDate validTo;
    /** 匹配关键词（逗号分隔） */
    private String keywordTags;
    /** 地区（全国/省/直辖市） */
    private String region;
    /** 政策概要（卡片速览） */
    private String policySummary;
    /** 有效期状态：VALID-有效 / EXPIRED-已过期 */
    private String validityStatus;
    /** 有效期状态名称 */
    private String validityStatusName;
    /** 是否匹配当前用户（关键词命中） */
    private Boolean matched;
    /** 命中关键词明细 */
    private List<String> hitKeywords;
    /** 匹配说明 */
    private String matchReason;
    private Integer sortOrder;
}
