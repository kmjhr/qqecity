package com.icbc.qingqi.common;

import lombok.Getter;

/**
 * 全局错误码枚举
 * <p>
 * 对齐《项目目录结构.md》文档定义：
 * - 0       成功
 * - 1001    参数错误
 * - 1002    未登录或登录已过期
 * - 2001    用户不存在
 * - 2002    用户已存在
 * - 3001    业务规则不满足
 * - 3002    模拟银行接口调用失败
 * - 4001    越权访问
 * - 5000    系统繁忙
 */
@Getter
public enum ErrorCode {

    // ---------- 成功 ----------
    SUCCESS(0, "success"),

    // ---------- 1xxx 通用客户端错误 ----------
    /** 参数错误 */
    PARAM_ERROR(1001, "参数错误"),
    /** 未登录或登录已过期 */
    UNAUTHORIZED(1002, "未登录或登录已过期"),

    // ---------- 2xxx 用户相关错误 ----------
    /** 用户不存在 */
    USER_NOT_FOUND(2001, "用户不存在"),
    /** 用户已存在 */
    USER_ALREADY_EXISTS(2002, "用户已存在"),

    // ---------- 3xxx 业务规则错误 ----------
    /** 业务规则不满足 */
    BIZ_RULE_NOT_MET(3001, "业务规则不满足"),
    /** 模拟银行接口调用失败 */
    BANK_API_FAILED(3002, "模拟银行接口调用失败"),

    // ---------- 4xxx 权限错误 ----------
    /** 越权访问 */
    FORBIDDEN(4001, "越权访问"),

    // ---------- 5xxx 服务端错误 ----------
    /** 系统繁忙 */
    SYSTEM_ERROR(5000, "系统繁忙，请稍后重试");

    private final Integer code;
    private final String message;

    ErrorCode(Integer code, String message) {
        this.code = code;
        this.message = message;
    }
}
