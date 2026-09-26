package com.icbc.qingqi.module.guarantee;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 保函申请（guarantee_apply）
 */
@Data
@TableName("guarantee_apply")
public class GuaranteeApply {

    @TableId(type = IdType.AUTO)
    private Long applyId;
    /** 租赁合同ID */
    private Long contractId;
    /** 申请人用户ID */
    private Long userId;
    /** 保函金额 */
    private BigDecimal guaranteeAmount;
    /** 保函期限（月） */
    private Integer guaranteeTermMonths;
    /** 保函费率（年化） */
    private BigDecimal feeRate;
    /** 保函费 */
    private BigDecimal feeAmount;
    /** 申请状态：0待房东确认/1待缴费/2已开函/3已失效/4赔付中 */
    private Integer applyStatus;
    /** 缴费状态：0未缴/1已缴 */
    private Integer payStatus;
    private LocalDateTime createTime;
}
