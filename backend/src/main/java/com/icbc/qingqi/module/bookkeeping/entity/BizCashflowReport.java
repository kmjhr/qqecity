package com.icbc.qingqi.module.bookkeeping.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 现金流报表实体
 * <p>
 * 对应表：biz_cashflow_report
 * 唯一键 (user_id, report_period)
 * 预警等级：NORMAL/WARNING/CRITICAL
 */
@Data
@TableName("biz_cashflow_report")
public class BizCashflowReport {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    /** 报表周期：2024-01 等 */
    private String reportPeriod;

    private BigDecimal totalIncome;

    private BigDecimal totalExpense;

    private BigDecimal netCashFlow;

    private BigDecimal profitAmount;

    private BigDecimal profitMargin;

    /** 预警等级：NORMAL/WARNING/CRITICAL */
    private String warningLevel;

    private String warningContent;

    /** 详细报表数据（JSON） */
    private String reportData;

    private LocalDateTime generateTime;

    @TableLogic
    private Integer deleted;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
