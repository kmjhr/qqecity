package com.icbc.qingqi.module.guarantee.dto;

import lombok.Data;

/**
 * 管理端 - 房东信息编辑 DTO
 */
@Data
public class LandlordUpdateDTO {
    private String realName;
    private String idCard;
    private String phone;
    private String bankAccount;
    private String bankName;
}
