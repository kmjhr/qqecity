package com.icbc.qingqi.common;

import lombok.Getter;

/**
 * 全局错误码枚举
 * <p>
 * 对齐《后端接口规范》8.2 节定义：
 * - 0       成功
 * - 1001    参数错误
 * - 1002    业务异常（业务规则不满足，如密码错误、状态非法）
 * - 2001    未登录 / Token 无效（无 Token、Token 伪造、Token 过期）
 * - 2002    无权限（越权访问他人数据、角色权限不足）
 * - 3001    资源不存在
 * - 3002    资源冲突（唯一键冲突、重复提交，如用户已存在）
 * - 4001    第三方服务异常（模拟桩失败、外部依赖不可用）
 * - 5000    系统内部错误
 */
@Getter
public enum ErrorCode {

    // ---------- 成功 ----------
    SUCCESS(0, "success"),

    // ---------- 1xxx 通用客户端错误 ----------
    /** 参数错误 */
    PARAM_ERROR(1001, "参数错误"),
    /** 业务异常（业务规则不满足） */
    BIZ_ERROR(1002, "业务异常"),

    // ---------- 2xxx 鉴权与权限错误 ----------
    /** 未登录 / Token 无效 */
    UNAUTHORIZED(2001, "未登录或登录已过期"),
    /** 无权限 */
    FORBIDDEN(2002, "无权限"),

    // ---------- 3xxx 资源错误 ----------
    /** 资源不存在 */
    NOT_FOUND(3001, "资源不存在"),
    /** 资源冲突 */
    CONFLICT(3002, "资源冲突"),

    // ---------- 4xxx 第三方服务错误 ----------
    /** 第三方服务异常 */
    THIRD_PARTY_ERROR(4001, "第三方服务异常"),

    // ---------- 5xxx 服务端错误 ----------
    /** 系统内部错误 */
    SYSTEM_ERROR(5000, "系统繁忙，请稍后重试");

    private final Integer code;
    private final String message;

    ErrorCode(Integer code, String message) {
        this.code = code;
        this.message = message;
    }
}
