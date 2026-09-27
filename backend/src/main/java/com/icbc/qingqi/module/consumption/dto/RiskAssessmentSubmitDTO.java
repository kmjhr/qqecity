package com.icbc.qingqi.module.consumption.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.Map;

/**
 * 风险测评答案提交 DTO
 * <p>
 * answers key=题号(1-10), value=选项标识(A/B/C/D)
 */
@Data
public class RiskAssessmentSubmitDTO {

    @NotNull(message = "答案不能为空")
    @NotEmpty(message = "答案不能为空")
    private Map<Integer, String> answers;
}
