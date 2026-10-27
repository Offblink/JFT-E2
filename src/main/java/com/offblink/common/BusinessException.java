package com.offblink.common;

/**
 * 业务异常：可直接展示给用户的「预期内失败」（参数不合规、目标不存在、重复提交…）。
 *
 * 与 RuntimeException 的区别：业务异常是流程的一部分，消息原样返回给前端；
 * 其余 RuntimeException 属于「程序没考虑到的情况」，消息一律换成兜底文案，避免泄露实现细节。
 */
public class BusinessException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public BusinessException(String message) {
        super(message);
    }

    public BusinessException(String message, Throwable cause) {
        super(message, cause);
    }
}
