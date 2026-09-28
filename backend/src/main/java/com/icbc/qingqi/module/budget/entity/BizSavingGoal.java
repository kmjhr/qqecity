package com.icbc.qingqi.module.budget.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 心愿储蓄实体
 * <p>
 * 对应表：biz_saving_goal
 */
@Data
@TableName("biz_saving_goal")
public class BizSavingGoal {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    private String goalName;

    private BigDecimal targetAmount;

    private BigDecimal currentAmount;

    private BigDecimal progressPercent;

    private LocalDate deadline;

    private String description;

    /** 目标类型：WISH心愿储蓄 / REPAY_GUARD还款保障金 */
    private String goalType;

    /** 关联授信额度ID（还款保障金绑定） */
    private Long linkedCreditLimitId;

    /** 状态：ACTIVE/COMPLETED/CANCELLED */
    private String status;

    @TableLogic
    private Integer deleted;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
