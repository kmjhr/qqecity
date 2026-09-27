package com.icbc.qingqi.module.consumption.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 征信报告实体（模拟）
 * <p>
 * 对应表：biz_credit_report
 * 数据来源：SIMULATED（演示模拟，不产生硬查询）
 */
@Data
@TableName("biz_credit_report")
public class BizCreditReport {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    private String reportNo;

    /** 报告类型：SIMPLE/DETAIL */
    private String reportType;

    private Integer creditScore;

    /** 信用等级：EXCELLENT/GOOD/FAIR/POOR */
    private String creditLevel;

    private Integer totalLoanCount;

    private Integer overdueCount;

    private BigDecimal totalCreditLimit;

    private BigDecimal usedCreditLimit;

    private Integer queryCount;

    private String reportSummary;

    private String reportDetail;

    private LocalDateTime queryTime;

    /** 数据来源：SIMULATED/REAL */
    private String source;

    @TableLogic
    private Integer deleted;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
