package com.icbc.qingqi.security;

import com.icbc.qingqi.common.BizException;
import com.icbc.qingqi.common.ErrorCode;

/**
 * 登录上下文（ThreadLocal）
 */
public final class UserContext {

    private static final ThreadLocal<Long> USER_ID = new ThreadLocal<>();
    private static final ThreadLocal<String> USER_NAME = new ThreadLocal<>();

    private UserContext() {
    }

    public static void set(Long userId, String name) {
        USER_ID.set(userId);
        USER_NAME.set(name);
    }

    public static Long getUserId() {
        return USER_ID.get();
    }

    public static String getUserName() {
        return USER_NAME.get();
    }

    /**
     * 获取当前登录用户，未登录时抛出 2001
     */
    public static Long requireUserId() {
        Long id = USER_ID.get();
        if (id == null) {
            throw new BizException(ErrorCode.UNAUTHORIZED);
        }
        return id;
    }

    public static void clear() {
        USER_ID.remove();
        USER_NAME.remove();
    }
}
