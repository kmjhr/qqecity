package com.icbc.qingqi.module.bookkeeping.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 记账记录 DTO（B-1）
 */
@Data
public class BookkeepingRecordDTO {

    /** 记录类型：INCOME/EXPENSE */
    @NotBlank(message = "记录类型不能为空")
    private String recordType;

    /** 分类 */
    @NotBlank(message = "分类不能为空")
    private String category;

    /** 金额 */
    @NotNull(message = "金额不能为空")
    @DecimalMin(value = "0.01", message = "金额必须大于0")
    private BigDecimal amount;

    /** 发生日期 */
    @NotNull(message = "发生日期不能为空")
    private LocalDate happenDate;

    /** 描述 */
    private String description;

    /** 交易对方 */
    private String relatedParty;
}
