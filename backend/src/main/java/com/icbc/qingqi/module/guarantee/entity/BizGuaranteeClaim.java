package com.icbc.qingqi.module.guarantee.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 索赔实体
 * <p>
 * 对应表：biz_guarantee_claim
 */
@Data
@TableName("biz_guarantee_claim")
public class BizGuaranteeClaim {

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

    @TableLogic
    private Integer deleted;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
