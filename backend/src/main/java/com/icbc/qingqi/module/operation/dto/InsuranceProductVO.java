package com.icbc.qingqi.module.operation.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * 保险代销产品 VO
 * <p>
 * 合规口径：所有产品均为模拟演示，工行仅代销、不承保
 */
@Data
public class InsuranceProductVO {

    private Long id;
    private String productCode;
    private String productName;
    private String insuranceType;
    private String insuranceTypeName;
    private String scene;
    private String sceneName;

    private List<String> targetCrowdList;
    private String premiumRate;
    private BigDecimal coverageAmount;
    private String insurer;
    private String productElements;
    private String conditions;
    private String applyUrl;
    private Integer sortOrder;

    /** 当前用户是否匹配（按 user_type 命中 target_crowd） */
    private Boolean matched;

    /** 匹配原因说明 */
    private String matchReason;

    /** 合规声明（前端固定展示） */
    private String complianceNotice;
}
