package com.icbc.qingqi.common;

import lombok.Getter;

/**
 * 错误码，对应设计说明书 8.2 通用错误码表
 */
@Getter
public enum ErrorCode {

    SUCCESS(0, "success"),
    PARAM_ERROR(1001, "参数错误"),
    NOT_FOUND(1002, "数据不存在"),
    UNAUTHORIZED(2001, "未认证或登录态失效"),
    FORBIDDEN(2002, "无权限"),
    BIZ_ERROR(3001, "业务校验不通过"),
    CREDIT_NOT_AUTHORIZED(3002, "征信硬查询未授权"),
    RATE_LIMITED(4001, "接口限流"),
    SYSTEM_ERROR(5000, "系统内部异常");

    private final int code;
    private final String message;

    ErrorCode(int code, String message) {
        this.code = code;
        this.message = message;
    }
}
