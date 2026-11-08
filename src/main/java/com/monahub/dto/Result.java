package com.monahub.dto;

/**
 * 统一返回格式（实验二任务 4.3）：code / message / data。
 *
 * <p>code 与 HTTP 状态码保持一致：200 成功 / 400 参数或业务失败 /
 * 401 未登录 / 404 不存在 / 409 重复 / 500 兜底。</p>
 */
public class Result<T> {

    private int code;
    private String message;
    private T data;

    private Result() {
    }

    // ok方法
    public static <T> Result<T> ok(T data) {
        Result<T> r = new Result<>();
        r.code = 200;
        r.message = "success";
        r.data = data;
        return r;
    }

    public static <T> Result<T> fail(int code, String message) {
        Result<T> r = new Result<>();
        r.code = code;
        r.message = message;
        return r;
    }

    public static <T> Result<T> fail(String message) {
        return fail(500, message);
    }

    public int getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }

    public T getData() {
        return data;
    }
}
