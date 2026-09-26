package com.icbc.qingqi.module.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 管理员创建用户 DTO
 */
@Data
public class UserCreateDTO {

    @NotBlank(message = "用户名不能为空")
    @Size(min = 3, max = 20, message = "用户名长度 3-20 位")
    private String username;

    @NotBlank(message = "密码不能为空")
    @Size(min = 6, max = 20, message = "密码长度 6-20 位")
    private String password;

    private String nickname;
    private String phone;
    private String email;

    /** 人群类型 */
    private String userType;

    /** 角色：USER / ADMIN */
    private String role;

    /** 状态：0-禁用，1-正常 */
    private Integer status;
}
