package com.icbc.qingqi.module.guarantee.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 电子保函 VO
 */
@Data
public class GuaranteeVO {

    /** 保函 ID */
    private Long id;

    /** 保函编号 */
    private String guaranteeNo;

    /** 关联申请 ID */
    private Long applicationId;

    /** 申请编号 */
    private String applyNo;

    /** 租客 ID */
    private Long tenantId;

    /** 租客姓名 */
    private String tenantName;

    /** 房东 ID */
    private Long landlordId;

    /** 房东姓名 */
    private String landlordName;

    /** 房屋 ID */
    private Long houseId;

    /** 房屋地址 */
    private String houseAddress;

    /** 保函金额 */
    private BigDecimal guaranteeAmount;

    /** 保函费 */
    private BigDecimal guaranteeFee;

    /** 生效日期 */
    private LocalDate effectiveDate;

    /** 到期日期 */
    private LocalDate expireDate;

    /** 保函状态：ACTIVE/EXPIRED/CLAIMED/TERMINATED */
    private String guaranteeStatus;

    /** 保函状态展示名 */
    private String statusName;

    /** 缴费状态：UNPAID/PAID/REFUNDED */
    private String payStatus;

    /** 缴费时间 */
    private LocalDateTime payTime;

    /** 开函时间 */
    private LocalDateTime issueTime;

    /** 房屋租住情况：PRE_RENTAL/RENTING_CLAIMED/ENDED_CLAIMED/RENTING_NORMAL/ENDED_NORMAL/ENDED_CONFIRMED */
    private String houseSituation;

    /** 房屋租住情况展示名：租期前·待入住/租中·被索赔/结束租·被索赔/租中·正常/租后·正常/租后·确认无需索赔 */
    private String houseSituationName;

    /** 租期开始日（关联租赁合同） */
    private LocalDate rentStartDate;

    /** 租期结束日（关联租赁合同） */
    private LocalDate rentEndDate;
}
