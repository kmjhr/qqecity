package com.icbc.qingqi.module.guarantee.dto;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 管理端 - 房东管理 VO
 */
@Data
public class LandlordVO {
    private Long id;
    /** 关联用户 ID */
    private Long userId;
    /** 关联登录账号 */
    private String username;
    private String realName;
    private String idCard;
    private String phone;
    private String bankAccount;
    private String bankName;
    /** 认证状态：PENDING/VERIFIED/REJECTED */
    private String verifyStatus;
    /** 关联房屋数 */
    private Integer houseCount;
    /** 关联用户状态：1启用 0停用 */
    private Integer status;
    private LocalDateTime createTime;
}
