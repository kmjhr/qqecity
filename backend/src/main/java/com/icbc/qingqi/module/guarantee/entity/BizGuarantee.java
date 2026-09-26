package com.icbc.qingqi.module.guarantee.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 保函主表实体
 * <p>
 * 对应表：biz_guarantee
 * 申请通过（缴费）后生成正式电子保函，与申请一对一
 * <p>
 * 保函状态（guarantee_status）：ACTIVE-有效 / EXPIRED-到期 / CLAIMED-已索赔 / TERMINATED-已终止
 * 缴费状态（pay_status）：UNPAID-未缴 / PAID-已缴 / REFUNDED-已退
 */
@Data
@TableName("biz_guarantee")
public class BizGuarantee {

    /** 主键 ID */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 保函编号 */
    private String guaranteeNo;

    /** 关联申请 ID */
    private Long applicationId;

    /** 租客 ID */
    private Long tenantId;

    /** 房东 ID */
    private Long landlordId;

    /** 房屋 ID */
    private Long houseId;

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

    /** 缴费状态：UNPAID/PAID/REFUNDED */
    private String payStatus;

    /** 缴费时间 */
    private LocalDateTime payTime;

    /** 开函时间 */
    private LocalDateTime issueTime;

    /** 逻辑删除：0-存在，1-删除 */
    @TableLogic
    private Integer deleted;

    /** 创建时间 */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /** 更新时间 */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
