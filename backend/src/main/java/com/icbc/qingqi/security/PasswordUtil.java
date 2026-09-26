package com.icbc.qingqi.security;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * 密码工具类
 * <p>
 * 统一封装 BCrypt 密码加密与校验，避免在业务代码中散落 new BCryptPasswordEncoder()
 * 只引入 Spring Security Crypto 的加密器，不引入完整 Spring Security
 */
@Component
public class PasswordUtil {

    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

    /**
     * 加密明文密码
     *
     * @param rawPassword 明文密码
     * @return BCrypt 哈希后的密码
     */
    public String encode(String rawPassword) {
        return encoder.encode(rawPassword);
    }

    /**
     * 校验明文密码是否与哈希密码匹配
     *
     * @param rawPassword     明文密码
     * @param encodedPassword 哈希密码
     * @return true 匹配，false 不匹配
     */
    public boolean matches(String rawPassword, String encodedPassword) {
        return encoder.matches(rawPassword, encodedPassword);
    }
}
