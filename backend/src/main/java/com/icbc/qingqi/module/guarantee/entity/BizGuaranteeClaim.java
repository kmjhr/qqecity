package com.icbc.qingqi.module.guarantee.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 保函索赔实体
 * <p>
 * 对应表：biz_guarantee_claim
 * 房东对已开立保函发起违约索赔，经 AI 初审 → 申辩期 → 人工复核 → 赔付/拒绝
 * <p>
 * 索赔状态流转（claim_status）：
 * SUBMITTED-已提交 → AI_REVIEW-AI初审 →
 * APPROVED-低风险速赔 / DEFENSE_PERIOD-申辩期 → MANUAL_REVIEW-人工复核 →
 * APPROVED-赔付 / REJECTED-拒绝 → CLOSED-已结案
 */
@Data
@TableName("biz_guarantee_claim")
public class BizGuaranteeClaim {

    /** 主键 ID */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 索赔编号 */
    private String claimNo;

    /** 保函 ID */
    private Long guaranteeId;

    /** 保函编号（冗余） */
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

    /** 索赔状态：SUBMITTED/AI_REVIEW/MANUAL_REVIEW/APPROVED/REJECTED/DEFENSE_PERIOD/CLOSED */
    private String claimStatus;

    /** AI 初审结果：PASS/RISK_WARNING/MANUAL_REVIEW */
    private String aiReviewResult;

    /** AI 初审详情（JSON） */
    private String aiReviewDetail;

    /** 拒绝原因 */
    private String rejectReason;

    /** 申辩内容（租客提交） */
    private String defenseContent;

    /** 申辩佐证材料（JSON 数组，租客提交） */
    private String defenseFiles;

    /** 实际赔付金额 */
    private BigDecimal payoutAmount;

    /** 提交时间 */
    private LocalDateTime submitTime;

    /** 审核时间 */
    private LocalDateTime reviewTime;

    /** 结案时间 */
    private LocalDateTime closeTime;

    /** 逻辑删除：0-存在，1-删除 */
    @TableLogic
    private Integer deleted;

    /** 创建时间 */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /** 更新时间 */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
