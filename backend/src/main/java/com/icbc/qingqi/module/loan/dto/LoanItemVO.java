// LoanItemVO.java — 单笔借款明细（按笔计息）
package com.icbc.qingqi.module.loan.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 单笔借款明细（按笔计息）：每笔提款/受托支付 = 一笔独立借款
 */
@Data
@Schema(description = "单笔借款明细")
public class LoanItemVO {

    @Schema(description = "借款编号（A类=提款流水号CW，B类=受托支付号EP）")
    private String loanNo;

    @Schema(description = "借款日期")
    private LocalDate loanDate;

    @Schema(description = "借款本金")
    private BigDecimal principal;

    @Schema(description = "已还本金")
    private BigDecimal repaidPrincipal;

    @Schema(description = "剩余本金")
    private BigDecimal remainingPrincipal;

    @Schema(description = "已计息天数（自借款日至今日，最短1天）")
    private int borrowDays;

    @Schema(description = "年化利率（小数）")
    private BigDecimal rate;

    @Schema(description = "日利率文案（如 0.0105%/日）")
    private String dailyRateText;

    @Schema(description = "应还利息（剩余本金 × 日利率 × 天数）")
    private BigDecimal interestPreview;

    @Schema(description = "本笔应还合计（剩余本金+利息）")
    private BigDecimal totalDue;
}
