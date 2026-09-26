package com.icbc.qingqi.module.user.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 用户实体
 * <p>
 * 对应表：sys_user
 * 角色说明：
 * - USER  普通用户（用户端网页、小程序登录）
 * - ADMIN 管理员（管理端网页登录）
 * <p>
 * 人群类型（userType）：
 * - STUDENT       在校生
 * - GRADUATE      毕业2年内
 * - ENTREPRENEUR  青年创业者
 * - OTHER         其他青年
 */
@Data
@TableName("sys_user")
public class SysUser {

    /** 主键 ID */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 用户名 / 手机号（登录账号） */
    private String username;

    /** 密码（BCrypt 加密） */
    private String password;

    /** 昵称 */
    private String nickname;

    /** 真实姓名 */
    private String realName;

    /** 身份证号（脱敏存储） */
    private String idCard;

    /** 手机号 */
    private String phone;

    /** 邮箱 */
    private String email;

    /** 头像 URL */
    private String avatar;

    /** 角色：USER / ADMIN */
    private String role;

    /**
     * 人群类型
     * STUDENT-在校生 / GRADUATE-毕业2年内 / ENTREPRENEUR-青年创业者 / OTHER-其他青年
     */
    private String userType;

    /** 毕业日期 */
    private LocalDate graduationDate;

    /** 学校 */
    private String school;

    /** 状态：0-禁用，1-正常 */
    private Integer status;

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
