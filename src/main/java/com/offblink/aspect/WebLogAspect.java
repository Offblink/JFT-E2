package com.offblink.aspect;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Arrays;

/**
 * 请求日志切面：Controller 每进一次记一条「入参 → 耗时 / 异常」。
 *
 * 为什么是 AOP 而不是在每个方法里写 log：
 *   日志是横切关注点，散在 10 个接口里就是 10 份样板 + 10 个漏写点。
 *   @Around 是唯一能同时拿到「入参 + 返回值 + 异常 + 耗时」的通知类型，其余四种都只覆盖其中一部分。
 *
 * 放在子容器（SpringMvcConfig 扫描 com.offblink.aspect）：Spring AOP 的代理只作用于
 * 本容器内的 Bean，切面必须与 Controller 同容器才能织入。
 */
@Aspect
@Component
public class WebLogAspect {

    private static final Logger log = LoggerFactory.getLogger(WebLogAspect.class);

    @Pointcut("execution(* com.offblink.controller..*(..))")
    public void controllerMethods() {
    }

    @Around("controllerMethods()")
    public Object logAround(ProceedingJoinPoint pjp) throws Throwable {
        String signature = pjp.getSignature().toShortString();
        long start = System.currentTimeMillis();
        if (log.isDebugEnabled()) {
            log.debug("[REQ ] {} 参数：{}", signature, Arrays.toString(pjp.getArgs()));
        }
        try {
            Object result = pjp.proceed();
            log.info("[RESP] {} 耗时 {} ms", signature, System.currentTimeMillis() - start);
            return result;
        } catch (Throwable t) {
            // 异常在这里只记录、不吞：原样抛出，交给 GlobalExceptionHandler 统一成 JSON
            log.error("[ERR ] {} 耗时 {} ms，异常：{}",
                    signature, System.currentTimeMillis() - start, t.toString());
            throw t;
        }
    }
}
