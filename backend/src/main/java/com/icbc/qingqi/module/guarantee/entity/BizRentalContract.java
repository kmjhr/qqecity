package com.icbc.qingqi.module.guarantee.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 租赁合同实体
 * <p>
 * 对应表：biz_rental_contract
 * 关联房屋（house_id）、房东（landlord_id）、租客（tenant_id）
 * 合同状态：PENDING-待签署 / SIGNED-已签署 / TERMINATED-已终止
 */
@Data
@TableName("biz_rental_contract")
public class BizRentalContract {

    /** 主键 ID */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 合同编号 */
    private String contractNo;

    /** 房屋 ID */
    private Long houseId;

    /** 房东 ID */
    private Long landlordId;

    /** 租客用户 ID */
    private Long tenantId;

    /** 月租金 */
    private BigDecimal monthlyRent;

    /** 押金金额 */
    private BigDecimal depositAmount;

    /** 租期开始日 */
    private LocalDate rentStartDate;

    /** 租期结束日 */
    private LocalDate rentEndDate;

    /** 付款方式：MONTHLY-月付 / QUARTERLY-季付 */
    private String payMethod;

    /** 合同文件 URL */
    private String contractFile;

    /** 合同状态：PENDING/SIGNED/TERMINATED */
    private String contractStatus;

    /** 签署日期 */
    private LocalDate signDate;

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
