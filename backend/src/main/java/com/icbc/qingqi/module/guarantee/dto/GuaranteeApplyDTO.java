package com.icbc.qingqi.module.guarantee.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 保函申请 DTO
 */
@Data
public class GuaranteeApplyDTO {

    /** 房屋 ID */
    @NotNull(message = "房屋 ID 不能为空")
    private Long houseId;

    /** 租赁合同 ID */
    @NotNull(message = "租赁合同 ID 不能为空")
    private Long contractId;

    /** 押金金额（保函金额） */
    @NotNull(message = "押金金额不能为空")
    private BigDecimal depositAmount;

    /** 保函期限（月） */
    private Integer guaranteePeriodMonths;

    /** 申请人姓名 */
    @NotBlank(message = "申请人姓名不能为空")
    private String applicantName;

    /** 申请人电话 */
    private String applicantPhone;

    /** 房东姓名 */
    @NotBlank(message = "房东姓名不能为空")
    private String landlordName;

    /** 房东电话 */
    private String landlordPhone;

    /** 租赁开始日期 */
    private LocalDate rentStartDate;

    /** 租赁结束日期 */
    private LocalDate rentEndDate;
}
