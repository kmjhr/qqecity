package com.icbc.qingqi.module.bookkeeping.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 记账记录实体
 * <p>
 * 对应表：biz_bookkeeping_record
 * 记录类型：INCOME/EXPENSE；来源：AUTO/MANUAL/IMPORT
 */
@Data
@TableName("biz_bookkeeping_record")
public class BizBookkeepingRecord {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    /** 记录类型：INCOME/EXPENSE */
    private String recordType;

    private String category;

    private BigDecimal amount;

    private LocalDate happenDate;

    private String description;

    /** 来源：AUTO/MANUAL/IMPORT */
    private String source;

    /** 是否已确认（模糊交易0=待确认） */
    private Integer isConfirmed;

    private String relatedParty;

    @TableLogic
    private Integer deleted;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
