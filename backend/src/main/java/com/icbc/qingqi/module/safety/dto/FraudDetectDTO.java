package com.icbc.qingqi.module.safety.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 骗局甄别 DTO（S-2）
 */
@Data
public class FraudDetectDTO {

    /** 待检测的话术文本 */
    @NotBlank(message = "话术文本不能为空")
    private String inputText;
}
