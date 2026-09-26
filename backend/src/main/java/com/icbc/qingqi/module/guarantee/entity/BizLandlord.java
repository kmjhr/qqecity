package com.icbc.qingqi.module.guarantee.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 房东信息实体
 * <p>
 * 对应表：biz_landlord
 * 房东也是系统用户（user_id 关联 sys_user），拥有独立认证信息
 * 认证状态：PENDING-待认证 / VERIFIED-已认证 / REJECTED-已驳回
 */
@Data
@TableName("biz_landlord")
public class BizLandlord {

    /** 主键 ID */
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
