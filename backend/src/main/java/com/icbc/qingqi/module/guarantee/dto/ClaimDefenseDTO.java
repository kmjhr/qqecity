package com.icbc.qingqi.module.guarantee.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 租客申辩 DTO
 * <p>
 * 申辩期内租客提交反证内容，提交后索赔转入人工复核
 */
@Data
public class ClaimDefenseDTO {

    /** 申辩内容 */
    @NotBlank(message = "申辩内容不能为空")
    private String defenseContent;

    /** 申辩佐证材料（JSON 数组，可选） */
    private String defenseFiles;
}
