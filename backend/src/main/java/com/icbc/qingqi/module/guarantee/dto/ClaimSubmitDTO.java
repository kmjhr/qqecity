package com.icbc.qingqi.module.guarantee.dto;

import jakarta.validation.constraints.*;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 房东发起索赔 DTO
 * <p>
 * G-6 索赔闭环入口：房东提交索赔金额、原因、证据材料
 */
@Data
public class ClaimSubmitDTO {

    /** 保函 ID */
    @NotNull(message = "保函 ID 不能为空")
    private Long guaranteeId;

    /** 索赔金额 */
    @NotNull(message = "索赔金额不能为空")
    @DecimalMin(value = "0.01", message = "索赔金额必须大于 0")
    private BigDecimal claimAmount;

    /** 索赔原因 */
    @NotBlank(message = "索赔原因不能为空")
    private String claimReason;

    /**
     * 证据材料（JSON 数组字符串）
     * 示例：["欠租记录","损坏照片","物品清单","沟通记录"]
     */
    private String evidenceFiles;
}
