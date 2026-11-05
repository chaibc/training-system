package com.hanmo.calligraphy.common;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.stream.Collectors;

/**
 * 全局异常处理器。
 *
 * <p>把各层抛出的异常统一转换为 ApiResponse 失败体与对应的 HTTP 状态码，
 * 避免异常细节直接暴露给前端；未知异常会记录 error 日志。</p>
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * 处理业务异常：401/403/404/405/409/423 分别映射为同名 HTTP 状态码，其余按 400 返回。
     *
     * @param e 业务异常
     * @return 携带业务错误码的失败响应
     */
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiResponse<Void>> handleBusiness(BusinessException e) {
        HttpStatus status = switch (e.getCode()) {
            case 401 -> HttpStatus.UNAUTHORIZED;
            case 403 -> HttpStatus.FORBIDDEN;
            case 404 -> HttpStatus.NOT_FOUND;
            case 405 -> HttpStatus.METHOD_NOT_ALLOWED;
            case 409 -> HttpStatus.CONFLICT;
            case 423 -> HttpStatus.LOCKED;
            // 业务层用 500 表示「服务端契约被破坏」（如实体缺少 id 字段），HTTP 也用 500，避免被当成参数错误
            case 500 -> HttpStatus.INTERNAL_SERVER_ERROR;
            default -> HttpStatus.BAD_REQUEST;
        };
        return ResponseEntity.status(status).body(ApiResponse.fail(e.getCode(), e.getMessage()));
    }

    /**
     * 处理请求体字段校验失败：把所有字段错误消息用「；」拼接后按 400 返回。
     *
     * @param e 参数校验异常
     * @return 400 失败响应
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleValidation(MethodArgumentNotValidException e) {
        String message = e.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining("；"));
        return ResponseEntity.badRequest().body(ApiResponse.fail(400, message));
    }

    /**
     * 处理方法级权限不足（@PreAuthorize 拒绝），返回 403。
     *
     * @param e 权限异常
     * @return 403 失败响应
     */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiResponse<Void>> handleAccessDenied(AccessDeniedException e) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(ApiResponse.fail(403, "没有权限执行该操作"));
    }

    /**
     * 处理 Content-Type 不被支持（例如把 JSON 接口当 multipart/form-data 调用），返回 415。
     *
     * @param e 媒体类型异常
     * @return 415 失败响应
     */
    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<ApiResponse<Void>> handleMediaType(HttpMediaTypeNotSupportedException e) {
        return ResponseEntity.status(HttpStatus.UNSUPPORTED_MEDIA_TYPE)
                .body(ApiResponse.fail(415, "请求格式不被支持，请使用 application/json"));
    }

    /**
     * 处理 HTTP 方法不被支持（例如对着只接受 POST 的接口发 GET），返回 405 而不是 500。
     *
     * @param e 方法不支持异常
     * @return 405 失败响应
     */
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ApiResponse<Void>> handleMethodNotSupported(HttpRequestMethodNotSupportedException e) {
        return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED)
                .body(ApiResponse.fail(405, "请求方法不被支持：" + e.getMethod()));
    }

    /**
     * 处理静态资源 / 路径不存在，返回 404。
     *
     * @param e 资源不存在异常
     * @return 404 失败响应
     */
    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleNotFound(NoResourceFoundException e) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ApiResponse.fail(404, "资源不存在"));
    }

    /**
     * 兜底处理未预期异常：记录 error 日志，对外只返回 500 与通用提示。
     *
     * @param e 未捕获异常
     * @return 500 失败响应
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleUnknown(Exception e) {
        log.error("Unhandled exception", e);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.fail(500, "服务器内部错误"));
    }
}
