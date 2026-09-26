package com.icbc.qingqi.module.budget;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 心愿储蓄（goal_saving）
 */
@Data
@TableName("goal_saving")
public class GoalSaving {

    @TableId(type = IdType.AUTO)
    private Long savingId;
    private Long userId;
    private String goalName;
    private BigDecimal goalAmount;
    private BigDecimal currentAmount;
    /** 资金来源：0一键改存/1预算结余/2手动 */
    private Integer sourceType;
    /** 利率档位 */
    private String rateGrade;
    private LocalDate openDate;
    /** 状态：0进行中/1已达成/2已终止 */
    private Integer status;
}
