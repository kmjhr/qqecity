package com.icbc.qingqi.module.consumption.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 理财产品实体（仅低风险，模拟）
 * <p>
 * 对应表：biz_finance_product
 * 风险等级：R1-低风险 / R2-中低风险（仅此两档）
 * 合规口径：理财非存款、产品有风险、工行仅代销
 */
@Data
@TableName("biz_finance_product")
public class BizFinanceProduct {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String productCode;
    private String productName;

    /** 产品类型：SAVING_GOAL/CASH_MANAGEMENT/SHORT_BOND/FUND_DCA/GOLD_ACCUM */
    private String productType;

    /** 风险等级：R1/R2 */
    private String riskLevel;

    private String expectedReturn;
    private BigDecimal minAmount;
    private String period;
    private String productElements;
    private String riskDisclosure;
    private String applyUrl;

    /** 适配风险等级（逗号分隔）：CONSERVATIVE/STEADY/BALANCED */
    private String targetRiskLevel;

    private String status;
    private Integer sortOrder;

    @TableLogic
    private Integer deleted;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
