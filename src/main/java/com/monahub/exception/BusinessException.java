package com.monahub.exception;

/**
 * 业务异常：带 HTTP 状态码，由 {@link GlobalExceptionHandler} 统一转成友好错误页。
 *
 * <p>文案面向用户（"该文件已被冻结"），不包含堆栈与内部路径。</p>
 */
public class BusinessException extends RuntimeException {

    private final int status;

    public BusinessException(int status, String message) {
        super(message);
        this.status = status;
    }

    public int getStatus() {
        return status;
    }
}
