package com.icbc.qingqi.module.guarantee.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

/**
 * banker 人工复核索赔 DTO
 * <p>
 * banker01 对 MANUAL_REVIEW 状态的索赔进行人工复核，决定赔付或拒绝
 */
@Data
public class ClaimReviewDTO {

    /** 复核结论：APPROVED-赔付 / REJECTED-拒绝 */
    @NotBlank(message = "复核结论不能为空")
    private String decision;

    /** 实际赔付金额（APPROVED 时必填） */
    private BigDecimal payoutAmount;

    /** 拒绝原因（REJECTED 时必填） */
    private String rejectReason;
}
