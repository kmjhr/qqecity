package com.icbc.qingqi.module.guarantee.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 房东电子签约 DTO
 * <p>
 * G-2 房东确认环节携带电子签名：
 * - Canvas 手写签名为 base64 编码图片
 * - 点击确认为 "CLICK_CONFIRM"
 */
@Data
public class LandlordSignDTO {

    /**
     * 电子签名内容
     * - Canvas 手写：data:image/png;base64,xxxx
     * - 点击确认：CLICK_CONFIRM
     */
    @NotBlank(message = "签名内容不能为空")
    private String signContent;
}
