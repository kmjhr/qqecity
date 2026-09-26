package com.icbc.qingqi.module.loan.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 预审结果 VO（L-1）
 */
@Data
public class LoanPrecheckVO {

    /** 申请 ID */
    private Long id;

    /** 申请编号 */
    private String applyNo;

    /** 贷款类型（预审默认 B_TYPE） */
    private String loanType;

    /** 人群资质 */
    private String crowdType;

    /** 预审结果：ELIGIBLE / NOT_ELIGIBLE / NEED_MORE_INFO */
    private String preCheckResult;

    /** 预审结果展示名 */
    private String preCheckResultName;

    /** 预审额度下限 */
    private BigDecimal preCheckMinAmount;

    /** 预审额度上限 */
    private BigDecimal preCheckMaxAmount;

    /** 预审详情（含"模拟-不查征信"标注） */
    private String preCheckDetail;

    /** 申请状态 */
    private String applyStatus;

    /** 提交时间 */
    private LocalDateTime submitTime;
}
