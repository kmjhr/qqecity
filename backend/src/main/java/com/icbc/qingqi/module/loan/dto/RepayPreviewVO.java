package com.icbc.qingqi.module.loan.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 还款试算预览（A/B 双轨通用）
 */
@Data
@Schema(description = "还款试算预览")
public class RepayPreviewVO {

    @Schema(description = "额度类型 A_TYPE/B_TYPE")
    private String creditType;

    @Schema(description = "额度类型名称")
    private String creditTypeName;

    @Schema(description = "待还本金（已用额度）")
    private BigDecimal usedLimit;

    @Schema(description = "年化利率（小数，如0.0385）")
    private BigDecimal rate;

    @Schema(description = "年化利率（百分数文案，如3.85%）")
    private String rateText;

    @Schema(description = "计息起始日期（最早提款/最早受托支付日）")
    private LocalDate earliestDate;

    @Schema(description = "已计息天数")
    private int borrowDays;

    @Schema(description = "预估利息（按全额还清口径：本金×利率×天数/365）")
    private BigDecimal interestPreview;

    @Schema(description = "应还合计（本金+预估利息）")
    private BigDecimal totalDue;

    @Schema(description = "计息规则说明")
    private String remark;
}
