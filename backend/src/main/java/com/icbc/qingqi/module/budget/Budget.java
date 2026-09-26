package com.icbc.qingqi.module.budget;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 消费预算（budget）
 */
@Data
@TableName("budget")
public class Budget {

    @TableId(type = IdType.AUTO)
    private Long budgetId;
    private Long userId;
    /** 预算月份 YYYY-MM */
    private String budgetMonth;
    /** 消费分类 */
    private String category;
    private BigDecimal budgetAmount;
    private BigDecimal usedAmount;
    /** 剩余比例（%） */
    private BigDecimal remainRatio;
    /** 提醒等级：0正常/1温和/2紧张/3超支 */
    private Integer remindLevel;
    /** 结余结转金额 */
    private BigDecimal carryoverAmount;
    /** 状态：0进行中/1已结束 */
    private Integer status;
}
