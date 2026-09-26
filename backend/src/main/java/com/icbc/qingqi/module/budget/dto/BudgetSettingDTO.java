package com.icbc.qingqi.module.budget.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 预算设置 DTO（C-1）
 */
@Data
public class BudgetSettingDTO {

    /** 预算分类 ID */
    @NotNull(message = "预算分类不能为空")
    private Long categoryId;

    /** 预算金额 */
    @NotNull(message = "预算金额不能为空")
    @DecimalMin(value = "0.01", message = "预算金额必须大于0")
    private BigDecimal budgetAmount;

    /** 预算周期（可选，默认当月） */
    private String budgetPeriod;
}
