package com.icbc.qingqi.module.loan.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 授信额度 VO（L-2 A/B 双轨展示）
 */
@Data
public class CreditLimitVO {

    /** 额度 ID */
    private Long id;

    /** 授信类型：A_TYPE / B_TYPE */
    private String creditType;

    /** 授信类型展示名：A类5万循环 / B类小额定向 */
    private String creditTypeName;

    /** 总额度 */
    private BigDecimal totalLimit;

    /** 已用额度 */
    private BigDecimal usedLimit;

    /** 可用额度 */
    private BigDecimal availableLimit;

    /** 利率（年化） */
    private BigDecimal interestRate;

    /** 状态：ACTIVE / FROZEN / CLOSED */
    private String status;

    /** 状态展示名 */
    private String statusName;

    /** 生效日期 */
    private LocalDate effectiveDate;

    /** 到期日期 */
    private LocalDate expireDate;

    /** 备注（A类随借随还 / B类定向受托支付） */
    private String remark;
}
