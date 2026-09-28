package com.icbc.qingqi.module.pay.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

/** 模拟消费下单 DTO（淘宝式） */
@Data
public class ConsumeDTO {

    @NotNull(message = "商户不能为空")
    private Long merchantId;

    @NotNull(message = "消费金额不能为空")
    @DecimalMin(value = "0.01", message = "消费金额必须大于0")
    private BigDecimal amount;

    @NotBlank(message = "订单标题不能为空")
    private String subject;

    /** 消费分类编码（FOOD/SHOPPING/TRANSPORT/ENTERTAINMENT/HOUSING/OTHER，用于记账归类） */
    private String categoryCode;

    /** MCC 码（可选） */
    private String mccCode;

    private String description;
}
