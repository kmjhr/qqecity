package com.icbc.qingqi.module.operation.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * 现金流风险预警检测结果 VO
 * <p>
 * 规则：近3月结余率<10% 且 存在>7天应收未收 → CASHFLOW_WARNING
 */
@Data
public class CashflowWarningVO {

    /** 用户ID */
    private Long userId;

    /** 检测周期（近3月）起止 */
    private LocalDate periodStart;
    private LocalDate periodEnd;

    /** 近3月总收入 */
    private BigDecimal totalIncome;

    /** 近3月总支出 */
    private BigDecimal totalExpense;

    /** 净现金流 */
    private BigDecimal netCashFlow;

    /** 结余率（百分比，0-100） */
    private BigDecimal surplusRate;

    /** 是否命中预警 */
    private Boolean warningTriggered;

    /** 命中原因（命中时返回） */
    private List<String> triggerReasons;

    /** 应收未收记录数 */
    private Integer pendingReceivableCount;

    /** 最长应收未收天数 */
    private Integer maxPendingDays;

    /** 备货/淡旺季建议文案（命中时返回） */
    private String advice;

    /** 已生成的预警记录ID（命中时返回，可空） */
    private Long warningId;
}
