package com.icbc.qingqi.module.budget.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 预算设置实体
 * <p>
 * 对应表：biz_budget_setting
 * 唯一键 (user_id, category_id, budget_period)
 */
@Data
@TableName("biz_budget_setting")
public class BizBudgetSetting {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    private Long categoryId;

    private String categoryCode;

    /** 预算周期：2024-01 等 */
    private String budgetPeriod;

    private BigDecimal budgetAmount;

    private BigDecimal usedAmount;

    private BigDecimal remainingAmount;

    private BigDecimal usagePercent;

    /** 50% 提醒已发送 */
    @TableField("remind_50_sent")
    private Integer remind50Sent;

    /** 20% 提醒已发送 */
    @TableField("remind_20_sent")
    private Integer remind20Sent;

    /** 超支提醒已发送 */
    private Integer remindOverSent;

    @TableLogic
    private Integer deleted;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
