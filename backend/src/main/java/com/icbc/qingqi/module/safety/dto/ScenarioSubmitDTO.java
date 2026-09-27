package com.icbc.qingqi.module.safety.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.Map;

/**
 * 反诈情景教学提交答案 DTO
 * <p>
 * answers：题号(questionNo) → 用户选择的选项 key(A/B/C/D)
 */
@Data
public class ScenarioSubmitDTO {

    @NotNull(message = "answers 不能为空")
    private Map<Integer, String> answers;
}
