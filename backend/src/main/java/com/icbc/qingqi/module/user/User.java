package com.icbc.qingqi.module.user;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 青年用户（youth_user）
 */
@Data
@TableName("youth_user")
public class User {

    @TableId(type = IdType.AUTO)
    private Long userId;
    /** 姓名 */
    private String name;
    /** 证件类型 */
    private String idType;
    /** 证件号码（加密/脱敏存储） */
    private String idNumber;
    /** 手机号 */
    private String phone;
    /** 人群类型：在校大学生/应届毕业生/Z世代青年 */
    private String userType;
    /** 所在院校 */
    private String school;
    /** 学历层次 */
    private String eduLevel;
    /** 毕业年份 */
    private String gradYear;
    /** 登录密码散列 */
    private String passwordHash;
    /** 账户状态：0正常/1冻结 */
    private Integer status;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
