package com.offblink.common;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.NoHandlerFoundException;

/**
 * 全局异常处理：所有异常只有一个出口，返回统一的 JSON。
 *
 * 三个价值：
 *   1. 不把堆栈抛给前端（安全）；2. 失败也是 {code,message,data} 形状，前端无需特判；
 *   3. Controller 里不再写 try-catch —— 业务层 throw BusinessException 即可。
 *
 * 返回 ResponseEntity 而非裸 Result：HTTP 状态码与业务码保持一致（400/401/404/500），
 * 让 Gateway、监控、ApiFox 断言都能按状态码分层。
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /** 业务异常：消息是写给用户看的，原样返回 */
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<Result<Void>> handleBusiness(BusinessException e) {
        log.warn("[BIZ ] {}", e.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Result.fail(HttpStatus.BAD_REQUEST.value(), e.getMessage()));
    }

    /** @RequestBody 上的 @Valid 校验失败：取第一条字段错误返回，逐条罗列对用户没有意义 */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Result<Void>> handleValidation(MethodArgumentNotValidException e) {
        return badRequest(firstFieldError(e.getBindingResult().getFieldError()));
    }

    /** 表单/查询参数绑定失败（与上一条同类，MethodArgumentNotValidException 是它的子类场景） */
    @ExceptionHandler(BindException.class)
    public ResponseEntity<Result<Void>> handleBind(BindException e) {
        return badRequest(firstFieldError(e.getBindingResult().getFieldError()));
    }

    /** JSON 无法解析（语法错误、类型不匹配） */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Result<Void>> handleUnreadable(HttpMessageNotReadableException e) {
        log.warn("[BAD ] 请求体解析失败：{}", e.getMessage());
        return badRequest("请求体格式不正确，请检查 JSON 与字段类型");
    }

    /** 必填的查询参数缺失 */
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<Result<Void>> handleMissingParam(MissingServletRequestParameterException e) {
        return badRequest("缺少必填参数：" + e.getParameterName());
    }

    /** 路径变量类型不对（如 /emps/abc 里的 abc 不是数字） */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<Result<Void>> handleTypeMismatch(MethodArgumentTypeMismatchException e) {
        return badRequest("参数 " + e.getName() + " 的类型不正确");
    }

    /** 请求方法不支持（如对 /emps 发 PATCH） */
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<Result<Void>> handleMethodNotSupported(HttpRequestMethodNotSupportedException e) {
        return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED)
                .body(Result.fail(HttpStatus.METHOD_NOT_ALLOWED.value(), "不支持的请求方法：" + e.getMethod()));
    }

    /** 路径不存在（依赖 DispatcherServlet 的 throwExceptionIfNoHandlerFound=true） */
    @ExceptionHandler(NoHandlerFoundException.class)
    public ResponseEntity<Result<Void>> handleNotFound(NoHandlerFoundException e) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(Result.fail(HttpStatus.NOT_FOUND.value(),
                        "接口不存在：" + e.getHttpMethod() + " " + e.getRequestURL()));
    }

    /** 兜底：未预料的异常只记日志，对外统一文案，绝不泄露堆栈与 SQL */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Result<Void>> handleOther(Exception e) {
        log.error("[FAIL] 未处理的异常", e);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Result.fail(HttpStatus.INTERNAL_SERVER_ERROR.value(), "系统繁忙，请稍后重试"));
    }

    private ResponseEntity<Result<Void>> badRequest(String message) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Result.fail(HttpStatus.BAD_REQUEST.value(), message));
    }

    private String firstFieldError(FieldError fieldError) {
        if (fieldError == null) {
            return "参数校验失败";
        }
        return fieldError.getDefaultMessage();
    }
}
