package com.icbc.qingqi.common;

import lombok.Getter;

/**
 * 业务异常
 * <p>
 * 业务逻辑中抛出此异常，由全局异常处理器统一捕获并返回 Result
 */
@Getter
public class BizException extends RuntimeException {

    private final Integer code;

    public BizException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.code = errorCode.getCode();
    }

    public BizException(ErrorCode errorCode, String message) {
        super(message);
        this.code = errorCode.getCode();
    }

    public BizException(Integer code, String message) {
        super(message);
        this.code = code;
    }
}
