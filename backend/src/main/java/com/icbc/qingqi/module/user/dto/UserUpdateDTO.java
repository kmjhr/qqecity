package com.icbc.qingqi.module.user.dto;

import lombok.Data;

/**
 * 用户信息修改 DTO
 * <p>
 * 普通用户修改个人信息 / 管理员修改用户信息 通用
 */
@Data
public class UserUpdateDTO {

    private String nickname;
    private String phone;
    private String email;
    private String avatar;

    /** 人群类型 */
    private String userType;

    /** 角色（仅管理员可修改） */
    private String role;

    /** 状态（仅管理员可修改） */
    private Integer status;
}
