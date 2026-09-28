package com.icbc.qingqi.module.loan.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 打款记录 VO（用户端）：受托支付成功流水 + 自定义商户复核单统一展示
 */
@Data
public class EntrustRecordVO {

    /** 记录号（EP流水号 / 复核单号） */
    private String recordNo;

    /** 记录类型：ENTRUST_PAY 受托支付打款 / ENTRUST_REVIEW 复核单 */
    private String recordType;

    /** 状态：SUCCESS 打款成功 / PENDING_REVIEW 待银行复核 / REJECTED 已驳回 */
    private String status;

    /** 状态展示名 */
    private String statusName;

    /** 收款商户 ID */
    private Long merchantId;

    /** 商户名称 */
    private String merchantName;

    /** 商户来源：SYSTEM平台通用 / USER_CUSTOM我的自定义 */
    private String merchantSource;

    /** 打款金额 */
    private BigDecimal amount;

    /** 用途说明 */
    private String purpose;

    /** 打款/提交时间 */
    private LocalDateTime recordTime;

    /** 复核意见（驳回原因等） */
    private String reviewRemark;

    /** 资金路径说明 */
    private String fundPath;
}
