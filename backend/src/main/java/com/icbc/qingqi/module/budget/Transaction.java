package com.icbc.qingqi.module.budget;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 交易记录（transaction）
 */
@Data
@TableName("`transaction`")
public class Transaction {

    @TableId(type = IdType.AUTO)
    private Long transId;
    private Long userId;
    /** 交易账户（加密存储） */
    private String accountNo;
    private BigDecimal transAmount;
    private LocalDateTime transTime;
    private String merchantName;
    /** 商户类别码（MCC） */
    private String mccCode;
    /** 消费分类：餐饮/娱乐/购物/交通/其他 */
    private String category;
    /** 归类方式：0自动/1AI建议待确认/2已确认 */
    private Integer classifyMode;
    /** 归类状态：0未归类/1已归类 */
    private Integer classifyStatus;
    /** 交易方向：0支出/1收入 */
    private Integer transDirection;
}
