package com.icbc.qingqi.module.admin.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 管理端 - 风险预警处置参数
 */
@Data
public class WarningHandleDTO {

    /** 处置备注（必填） */
    @NotBlank(message = "处置备注不能为空")
    private String handleNote;
}
