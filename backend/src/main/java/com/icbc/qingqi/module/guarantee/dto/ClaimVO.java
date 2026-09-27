package com.icbc.qingqi.module.guarantee.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 索赔 VO（列表 + 详情共用）
 */
@Data
public class ClaimVO {

    /** 索赔 ID */
    private Long id;

    /** 索赔编号 */
    private String claimNo;

    /** 保函 ID */
    private Long guaranteeId;

    /** 保函编号 */
    private String guaranteeNo;

    /** 索赔人 ID（房东） */
    private Long claimantId;

    /** 索赔人姓名 */
    private String claimantName;

    /** 被索赔租客 ID */
    private Long tenantId;

    /** 索赔金额 */
    private BigDecimal claimAmount;

    /** 索赔原因 */
    private String claimReason;

    /** 证据材料（JSON 数组） */
    private String evidenceFiles;

    /** 索赔状态（DB 原值） */
    private String claimStatus;

    /** 索赔状态展示名 */
    private String statusName;

    /** AI 初审结果 */
    private String aiReviewResult;

    /** AI 初审详情 */
    private String aiReviewDetail;

    /** 拒绝原因 */
    private String rejectReason;

    /** 申辩内容 */
    private String defenseContent;

    /** 实际赔付金额 */
    private BigDecimal payoutAmount;

    /** 提交时间 */
    private LocalDateTime submitTime;

    /** 审核时间 */
    private LocalDateTime reviewTime;

    /** 结案时间 */
    private LocalDateTime closeTime;

    /** 创建时间 */
    private LocalDateTime createTime;
}
