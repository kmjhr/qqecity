package com.icbc.qingqi.module.budget.dto;

import lombok.Data;

/**
 * 结余转储蓄 DTO（C-4）
 */
@Data
public class TransferSavingDTO {

    /** 目标心愿储蓄 ID（可选，不传则新建） */
    private Long goalId;

    /** 新建目标名称（goalId 为空时必填） */
    private String goalName;

    /** 目标金额（新建时必填） */
    private java.math.BigDecimal targetAmount;
}
