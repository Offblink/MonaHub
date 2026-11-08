package com.monahub.exception;

import com.monahub.controller.ApiController;
import com.monahub.dto.Result;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

/**
 * JSON API 的统一异常出口：全部翻译成 {@link Result}，HTTP 状态码与 code 一致。
 *
 * <p>用 assignableTypes 限定只作用于 {@link ApiController}——
 * JSP 页面流程的异常仍走 {@link GlobalExceptionHandler} 渲染 error.jsp，互不抢出口。
 * HIGHEST_PRECEDENCE 保证同类异常下本类优先。</p>
 */
@Order(Ordered.HIGHEST_PRECEDENCE)
@RestControllerAdvice(assignableTypes = ApiController.class)
public class RestGlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(RestGlobalExceptionHandler.class);

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<Result<Void>> handleBusiness(BusinessException e) {
        return ResponseEntity.status(e.getStatus())
                .body(Result.fail(e.getStatus(), e.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Result<Void>> handleValidation(MethodArgumentNotValidException e) {
        var error = e.getBindingResult().getFieldError();
        String message = error == null ? "参数校验失败" : error.getDefaultMessage();
        return ResponseEntity.badRequest().body(Result.fail(400, message));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Result<Void>> handleUnreadable(HttpMessageNotReadableException e) {
        return ResponseEntity.badRequest().body(Result.fail(400, "请求体格式错误"));
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<Result<Void>> handleTypeMismatch(MethodArgumentTypeMismatchException e) {
        return ResponseEntity.badRequest().body(Result.fail(400, "路径参数类型错误"));
    }

    /** 兜底：堆栈只进日志，对外统一文案，不暴露内部结构。 */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Result<Void>> handleOther(Exception e) {
        log.error("未知异常", e);
        return ResponseEntity.status(500).body(Result.fail(500, "系统繁忙，请稍后重试"));
    }
}
