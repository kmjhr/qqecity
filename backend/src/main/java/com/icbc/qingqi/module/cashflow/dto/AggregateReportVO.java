package com.icbc.qingqi.module.cashflow.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * 聚合流水报表
 */
@Data
public class AggregateReportVO {

    /** 总交易数 */
    private Integer totalCount;

    /** 总收入 */
    private BigDecimal totalIncome;

    /** 总退款 */
    private BigDecimal totalRefund;

    /** 净流水 */
    private BigDecimal netAmount;

    /** 渠道汇总 */
    private List<ChannelSummary> channelSummaries;

    /** 流水记录（最近 50 条） */
    private List<AggregateTxnVO> records;

    /** 是否模拟 */
    private Boolean simulated;

    @Data
    public static class ChannelSummary {
        private String channelCode;
        private String channelName;
        private Integer txnCount;
        private BigDecimal income;
        private BigDecimal refund;
    }
}
