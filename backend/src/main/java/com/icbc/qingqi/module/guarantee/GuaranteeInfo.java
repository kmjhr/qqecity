package com.icbc.qingqi.module.guarantee;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 保函信息（guarantee_info）
 */
@Data
@TableName("guarantee_info")
public class GuaranteeInfo {

    @TableId(type = IdType.AUTO)
    private Long guaranteeId;
    /** 保函申请ID */
    private Long applyId;
    /** 保函编号 */
    private String guaranteeNo;
    /** 申请人（租客）用户ID */
    private Long applicantId;
    /** 受益人（房东） */
    private String beneficiary;
    /** 保函金额 */
    private BigDecimal guaranteeAmount;
    private LocalDate issueDate;
    private LocalDate expireDate;
    /** 状态：0有效/1已失效/2赔付中/3已赔付 */
    private Integer guaranteeStatus;
    /** 电子保函地址 */
    private String eGuaranteeUrl;
}
