package com.icbc.qingqi.module.loan.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 贷款申请实体
 * <p>
 * 对应表：biz_loan_application
 * 贷款类型：A_TYPE（A类最高5万循环）/ B_TYPE（B类小额定向）
 */
@Data
@TableName("biz_loan_application")
public class BizLoanApplication {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 申请编号 */
    private String applyNo;

    /** 申请人 ID */
    private Long userId;

    /** 贷款类型：A_TYPE/B_TYPE */
    private String loanType;

    /** 申请金额 */
    private BigDecimal applyAmount;

    /** 贷款用途 */
    private String purpose;

    /** 创业计划描述 */
    private String businessPlan;

    /** 人群资质（冗余） */
    private String crowdType;

    /** 预审结果：ELIGIBLE/NOT_ELIGIBLE/NEED_MORE_INFO */
    private String preCheckResult;

    /** 预审额度下限 */
    private BigDecimal preCheckMinAmount;

    /** 预审额度上限 */
    private BigDecimal preCheckMaxAmount;

    /** 预审详情（JSON） */
    private String preCheckDetail;

    /** 申请状态：PRE_CHECK/PENDING_APPROVAL/APPROVED/REJECTED/CANCELLED */
    private String applyStatus;

    /** 审批金额 */
    private BigDecimal approveAmount;

    /** 拒绝原因 */
    private String rejectReason;

    /** 提交时间 */
    private LocalDateTime submitTime;

    /** 审批时间 */
    private LocalDateTime approveTime;

    @TableLogic
    private Integer deleted;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
