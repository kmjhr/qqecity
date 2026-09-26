package com.icbc.qingqi.module.user.dto;

import lombok.Data;

/**
 * 用户信息 VO（返回给前端）
 * <p>
 * 不包含密码等敏感信息
 */
@Data
public class UserVO {

    private Long id;
    private String username;
    private String nickname;
    private String phone;
    private String email;
    private String avatar;
    private String role;

    /** 人群类型 */
    private String userType;

    private Integer status;
    private String createTime;
}
