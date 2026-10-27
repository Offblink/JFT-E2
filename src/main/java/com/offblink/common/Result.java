package com.offblink.common;

import lombok.Data;

import java.io.Serializable;

/**
 * 统一返回格式：{@code {code, message, data}}。
 *
 * 为什么要统一外壳：前端只写一次「判 code → 取 data / 弹 message」的分支，
 * 不必为每个接口记住不同的返回形状；HTTP 状态码之外再带一层业务码，
 * 网关、前端、日志都能按固定字段断言。
 *
 * code 约定：200 成功；400 参数/业务校验失败；401 未登录；404 资源不存在；500 服务端异常。
 */
@Data
public class Result<T> implements Serializable {

    private static final long serialVersionUID = 1L;

    private Integer code;
    private String message;
    private T data;

    public static <T> Result<T> ok(T data) {
        Result<T> result = new Result<>();
        result.code = 200;
        result.message = "success";
        result.data = data;
        return result;
    }

    public static <T> Result<T> ok() {
        return ok(null);
    }

    public static <T> Result<T> fail(int code, String message) {
        Result<T> result = new Result<>();
        result.code = code;
        result.message = message;
        return result;
    }

    public static <T> Result<T> fail(String message) {
        return fail(500, message);
    }
}
