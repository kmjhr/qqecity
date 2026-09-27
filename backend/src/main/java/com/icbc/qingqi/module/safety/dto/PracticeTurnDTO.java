package com.icbc.qingqi.module.safety.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 用户发言请求（L3，模拟）
 */
@Data
public class PracticeTurnDTO {

    /** 用户自由发言内容 */
    @NotBlank(message = "发言内容不能为空")
    @Size(max = 500, message = "发言内容不能超过500字")
    private String content;
}
