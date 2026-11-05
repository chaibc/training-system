package com.hanmo.calligraphy.common;

import lombok.Getter;

/**
 * 业务异常。
 *
 * <p>携带业务错误码，由 GlobalExceptionHandler 统一转换为 HTTP 状态码与 ApiResponse；
 * 未指定错误码时默认为 400。</p>
 */
@Getter
public class BusinessException extends RuntimeException {

    // 业务错误码（默认 400；也可用 401/403/404/409/423/500 等）
    private final int code;

    /**
     * 以默认错误码 400 构造业务异常。
     *
     * @param message 提示信息
     */
    public BusinessException(String message) {
        this(400, message);
    }

    /**
     * 指定错误码构造业务异常。
     *
     * @param code    业务错误码（401/403/404/409/423 映射为同名状态码，其余按 400 返回）
     * @param message 提示信息
     */
    public BusinessException(int code, String message) {
        super(message);
        this.code = code;
    }
}
