package com.hanmo.calligraphy.common;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 统一 API 响应结构。
 *
 * <p>所有 Controller 的返回值都包装为 {code, message, data}：code=0 表示成功，
 * 非 0 为业务错误码，由 GlobalExceptionHandler 映射为相应的 HTTP 状态码。</p>
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ApiResponse<T> {

    // 业务状态码：0 成功；非 0 为错误码
    private int code;
    // 提示信息（成功时为 "ok"）
    private String message;
    // 业务数据；失败时为 null
    private T data;

    /**
     * 构造带数据的成功响应。
     *
     * @param data 业务数据
     * @return code=0、message="ok" 的响应
     */
    public static <T> ApiResponse<T> ok(T data) {
        return new ApiResponse<>(0, "ok", data);
    }

    /**
     * 构造无数据的成功响应。
     *
     * @return code=0、message="ok"、data=null 的响应
     */
    public static ApiResponse<Void> ok() {
        return new ApiResponse<>(0, "ok", null);
    }

    /**
     * 构造失败响应。
     *
     * @param code    业务错误码
     * @param message 提示信息
     * @return data 为 null 的失败响应
     */
    public static <T> ApiResponse<T> fail(int code, String message) {
        return new ApiResponse<>(code, message, null);
    }
}
