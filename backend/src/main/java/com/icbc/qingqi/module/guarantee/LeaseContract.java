package com.icbc.qingqi.module.guarantee;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 租赁合同（lease_contract）
 */
@Data
@TableName("lease_contract")
public class LeaseContract {

    @TableId(type = IdType.AUTO)
    private Long contractId;
    /** 租客用户ID */
    private Long userId;
    private String landlordName;
    private String landlordPhone;
    private String landlordIdNo;
    private String houseAddress;
    /** 月租金 */
    private BigDecimal monthlyRent;
    /** 押金金额 */
    private BigDecimal depositAmount;
    private LocalDate leaseStart;
    private LocalDate leaseEnd;
    private String contractFileUrl;
    private String handoverPhotoUrl;
    /** 复审状态：0待审/1自动通过/2转人工 */
    private Integer reviewStatus;
    /** AI复审意见 */
    private String reviewResult;
    private LocalDateTime reviewTime;
    private LocalDateTime createTime;
}
