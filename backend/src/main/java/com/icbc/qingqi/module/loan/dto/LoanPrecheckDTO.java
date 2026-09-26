package com.icbc.qingqi.module.loan.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.math.BigDecimal;

/**
 * B 类免费预审 DTO（L-1）
 * <p>
 * 不查征信，按创业计划 + 人群资质规则给出 1—2 万元额度区间
 */
@Data
public class LoanPrecheckDTO {

    /** 人群资质：STUDENT/ENTREPRENEUR/VETERAN/DISABLED/FARMER/UNEMPLOYED/OTHER */
    @NotBlank(message = "人群资质不能为空")
    private String crowdType;

    /** 创业计划描述 */
    @NotBlank(message = "创业计划不能为空")
    private String businessPlan;

    /** 贷款用途 */
    private String purpose;

    /** 期望申请金额（可选） */
    private BigDecimal applyAmount;
}
