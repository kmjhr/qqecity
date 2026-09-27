package com.icbc.qingqi.module.safety.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * 逾期风险预判结果 VO（模拟）
 * <p>
 * 缺口 #15 逾期风险预判
 * 基于还款日历（贷款/受托支付/保函费）+ 收支数据规则评分
 * 命中落 biz_risk_warning warning_type=OVERDUE_RISK
 */
@Data
public class OverdueRiskVO {

    /** 用户 ID */
    private Long userId;

    /** 预测周期（天） */
    private Integer aheadDays;

    /** 还款日历（未来 N 天内到期） */
    private List<RepaymentItem> calendar;

    /** 账户当前余额（模拟） */
    private BigDecimal accountBalance;

    /** 预计净现金流（未来 N 天 收入-支出，模拟） */
    private BigDecimal projectedNetCashFlow;

    /** 是否命中预警 */
    private Boolean warningTriggered;

    /** 命中原因清单 */
    private List<String> triggerReasons;

    /** 预警等级：LOW/MEDIUM/HIGH/CRITICAL */
    private String warningLevel;

    /** 风险评分（0-100，越高越危险） */
    private Integer riskScore;

    /** 应对建议 */
    private String advice;

    /** 预警 ID（如已落库） */
    private Long warningId;

    @Data
    public static class RepaymentItem {
        /** 还款类型：LOAN-贷款 / ENTRUST_PAYMENT-受托支付 / GUARANTEE_FEE-保函费 */
        private String type;
        /** 关联业务 ID */
        private Long bizId;
        /** 还款编号 */
        private String bizNo;
        /** 应还本金 */
        private BigDecimal principal;
        /** 应还利息 */
        private BigDecimal interest;
        /** 应还总额 */
        private BigDecimal totalAmount;
        /** 到期日 */
        private LocalDate dueDate;
        /** 距今天数 */
        private Integer daysToDue;
        /** 备注 */
        private String remark;
    }
}
