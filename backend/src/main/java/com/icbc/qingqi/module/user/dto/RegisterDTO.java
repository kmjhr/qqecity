package com.icbc.qingqi.module.user.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 用户注册 DTO
 * <p>
 * 新增 userType 字段，用于标识人群类型
 */
@Data
public class RegisterDTO {

    /** 用户名 / 手机号 */
    @NotBlank(message = "用户名不能为空")
    @Size(min = 3, max = 20, message = "用户名长度 3-20 位")
    private String username;

    /** 密码 */
    @NotBlank(message = "密码不能为空")
    @Size(min = 6, max = 20, message = "密码长度 6-20 位")
    private String password;

    /** 昵称 */
    private String nickname;

    /** 手机号 */
    @Pattern(regexp = "^1[3-9]\\d{9}$", message = "手机号格式不正确")
    private String phone;

    /** 邮箱 */
    @Email(message = "邮箱格式不正确")
    private String email;

    /**
     * 人群类型
     * STUDENT-在校生 / GRADUATE-毕业2年内 / ENTREPRENEUR-青年创业者 / OTHER-其他青年
     */
    private String userType;
}
