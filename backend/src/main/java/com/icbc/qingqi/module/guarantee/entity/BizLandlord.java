package com.icbc.qingqi.module.guarantee.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 房东信息实体
 * <p>
 * 对应表：biz_landlord
 */
@Data
@TableName("biz_landlord")
public class BizLandlord {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 对应用户表 ID（房东也是系统用户） */
    private Long userId;

    /** 真实姓名 */
    private String realName;

    /** 身份证号 */
    private String idCard;

    /** 联系电话 */
    private String phone;

    /** 收款银行账号 */
    private String bankAccount;

    /** 开户银行 */
    private String bankName;

    /** 认证状态：PENDING/VERIFIED/REJECTED */
    private String verifyStatus;

    @TableLogic
    private Integer deleted;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
