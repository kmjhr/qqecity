package com.icbc.qingqi.module.loan;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 贷款申请（loan_apply）
 */
@Data
@TableName("loan_apply")
public class LoanApply {

    @TableId(type = IdType.AUTO)
    private Long loanApplyId;
    private Long userId;
    /** 贷款类型：A/B */
    private String loanType;
    /** 申请金额 */
    private BigDecimal applyAmount;
    /** 贷款用途（限合法经营用途） */
    private String loanPurpose;
    /** 创业计划书地址 */
    private String bizPlanUrl;
    /** 预审状态：0未预审/1通过/2不通过 */
    private Integer precheckStatus;
    /** 预审额度区间 */
    private String precheckRange;
    /** 审批状态：0待审批/1通过/2拒绝 */
    private Integer approveStatus;
    private BigDecimal approveAmount;
    private BigDecimal interestRate;
    private String approver;
    private LocalDateTime approveTime;
    private LocalDateTime createTime;
}
