package com.icbc.qingqi.module.loan.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 授信额度实体
 * <p>
 * 对应表：biz_credit_limit
 * 授信类型：A_TYPE（最高5万循环）/ B_TYPE（小额定向）
 */
@Data
@TableName("biz_credit_limit")
public class BizCreditLimit {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 用户 ID */
    private Long userId;

    /** 授信类型：A_TYPE/B_TYPE */
    private String creditType;

    /** 总额度 */
    private BigDecimal totalLimit;

    /** 已用额度 */
    private BigDecimal usedLimit;

    /** 可用额度 */
    private BigDecimal availableLimit;

    /** 利率（年化） */
    private BigDecimal interestRate;

    /** 状态：ACTIVE/FROZEN/CLOSED */
    private String status;

    /** 生效日期 */
    private LocalDate effectiveDate;

    /** 到期日期 */
    private LocalDate expireDate;

    @TableLogic
    private Integer deleted;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
