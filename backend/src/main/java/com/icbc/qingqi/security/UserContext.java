package com.icbc.qingqi.security;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 当前登录用户上下文
 * <p>
 * 通过 ThreadLocal 存储，在一次请求内任意位置可获取当前登录用户信息
 * 由 JwtAuthFilter 在请求进入时设置，响应返回前清除
 */
public class UserContext {

    private static final ThreadLocal<CurrentUser> CURRENT_USER = new ThreadLocal<>();

    public static void set(CurrentUser user) {
        CURRENT_USER.set(user);
    }

    public static CurrentUser get() {
        return CURRENT_USER.get();
    }

    public static Long getUserId() {
        CurrentUser user = CURRENT_USER.get();
        return user != null ? user.getUserId() : null;
    }

    public static String getUsername() {
        CurrentUser user = CURRENT_USER.get();
        return user != null ? user.getUsername() : null;
    }

    public static String getRole() {
        CurrentUser user = CURRENT_USER.get();
        return user != null ? user.getRole() : null;
    }

    public static boolean isAdmin() {
        CurrentUser user = CURRENT_USER.get();
        return user != null && "ADMIN".equals(user.getRole());
    }

    public static void clear() {
        CURRENT_USER.remove();
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CurrentUser {
        /** 用户 ID */
        private Long userId;
        /** 用户名 / 手机号 */
        private String username;
        /** 角色：USER / ADMIN */
        private String role;
    }
}
