package com.icbc.qingqi.module.loan;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 授信额度（credit_limit）
 */
@Data
@TableName("credit_limit")
public class CreditLimit {

    @TableId(type = IdType.AUTO)
    private Long creditId;
    private Long userId;
    /** 授信类型：A循环额度/B定向额度 */
    private String creditType;
    private BigDecimal creditAmount;
    private BigDecimal usedAmount;
    private BigDecimal remainAmount;
    private LocalDate validStart;
    private LocalDate validEnd;
    /** 额度状态：0有效/1冻结/2已失效 */
    private Integer creditStatus;
    private LocalDateTime updateTime;
}
