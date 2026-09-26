package com.icbc.qingqi.module.user.dto;

import lombok.Data;

/**
 * 登录成功返回 Token 对
 */
@Data
public class TokenVO {

    /** 访问令牌（2 小时有效） */
    private String accessToken;

    /** 刷新令牌（7 天有效） */
    private String refreshToken;

    /** 令牌类型：Bearer */
    private String tokenType = "Bearer";

    /** 过期时间（秒） */
    private Long expiresIn;

    /** 用户信息 */
    private UserVO user;
}
