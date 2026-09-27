package com.icbc.qingqi.module.safety.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 征信报告智能解读 VO（模拟）
 * <p>
 * 缺口 #2 征信报告智能解读
 * 对应 biz_credit_report，结构化输出：信用分/等级/逾期/查询/负债率 + 逐项评价与改进建议
 */
@Data
public class CreditReportInterpretVO {

    /** 报告 ID */
    private Long reportId;

    /** 报告编号 */
    private String reportNo;

    /** 报告类型：SIMPLE/DETAIL */
    private String reportType;

    /** 信用分（350-850） */
    private Integer creditScore;

    /** 信用等级：EXCELLENT/GOOD/FAIR/POOR */
    private String creditLevel;

    /** 贷款笔数 */
    private Integer totalLoanCount;

    /** 逾期笔数 */
    private Integer overdueCount;

    /** 总授信额度 */
    private BigDecimal totalCreditLimit;

    /** 已用额度 */
    private BigDecimal usedCreditLimit;

    /** 额度使用率（百分比） */
    private BigDecimal utilizationRate;

    /** 查询次数 */
    private Integer queryCount;

    /** 查询时间 */
    private LocalDateTime queryTime;

    /** 数据来源：SIMULATED */
    private String source;

    /** 逐项评价（信用分/逾期/查询/负债率/账户数） */
    private List<ItemAssessment> assessments;

    /** 总体评价：GOOD/FAIR/POOR */
    private String overallLevel;

    /** 总体评价文案 */
    private String overallSummary;

    /** 改进建议清单 */
    private List<String> suggestions;

    /** 模拟标注 */
    private String simulationNotice;

    @Data
    public static class ItemAssessment {
        /** 评估项：CREDIT_SCORE / OVERDUE / QUERY / UTILIZATION / LOAN_COUNT */
        private String itemKey;
        /** 评估项名称 */
        private String itemLabel;
        /** 当前值（字符串展示） */
        private String currentValue;
        /** 评价等级：GOOD / FAIR / POOR */
        String level;
        /** 评价文案 */
        private String evaluation;
        /** 针对该项的改进建议 */
        private String suggestion;
    }
}
