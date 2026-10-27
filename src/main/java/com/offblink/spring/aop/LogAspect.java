package com.offblink.spring.aop;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.After;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.AfterThrowing;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.annotation.Pointcut;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Arrays;

/**
 * 日志切面：五种通知全上，@Around 额外演示「改参数 + 计时 + 异常透传」
 *
 * 只有 @Component 它是个普通 Bean；@Aspect 才让它成为切面，
 * @EnableAspectJAutoProxy（AopConfig）负责把切面织进目标 Bean 的代理里。
 */
@Aspect
@Component
public class LogAspect {

    // 本类日志对象
    private static final Logger log = LoggerFactory.getLogger(LogAspect.class);

    // 定义切点
    @Pointcut("execution(* com.offblink.spring.aop.UserService.*(..))")
    public void servicePointcut() {
    }

    @Before("servicePointcut()")
    public void beforeLog(JoinPoint jp) {
        // {}占位符是slf4j语法，与C类似，右边参数填入即可
        log.info("⏰ @Before 前置通知 {} 参数: {}", jp.getSignature().getName(), Arrays.toString(jp.getArgs()));
    }

    // 被拦截方法的return（返回值），作为本方法的result形参输入
    @AfterReturning(pointcut = "servicePointcut()", returning = "result")
    public void afterReturningLog(JoinPoint jp, Object result) {
        log.info("✅ @AfterReturning 返回通知 {} 参数: {} -> 返回: {}",
                jp.getSignature().getName(), Arrays.toString(jp.getArgs()), result);
    }

    @AfterThrowing(pointcut = "servicePointcut()", throwing = "ex")
    public void afterThrowingLog(JoinPoint jp, Throwable ex) {
        log.info("❌ @AfterThrowing 异常通知 {} 参数: {} 异常: {}",
                jp.getSignature().getName(), Arrays.toString(jp.getArgs()), ex.getMessage());
    }

    @After("servicePointcut()")
    public void afterLog(JoinPoint jp) {
        log.info("🔔 @After 最终通知 {} 参数: {}", jp.getSignature().getName(), Arrays.toString(jp.getArgs()));
    }

    // 环绕通知：独占目标方法的前后控制权 —— 能改参、能计时、能吞异常（这里选择透传）
    @Around("servicePointcut()")
    public Object aroundLog(ProceedingJoinPoint pjp) throws Throwable {
        String method = pjp.getSignature().getName();
        log.info("🌯 @Around 环绕进入 {} 参数: {}", method, Arrays.toString(pjp.getArgs()));

        // 改参数后 proceed：目标方法实际收到的是改写后的参数（findUser 的用例能看到效果）
        Object[] args = pjp.getArgs().clone();
        if (args.length > 0 && args[0] instanceof String) {
            args[0] = "[已加密]" + args[0]; // 修改了args（参数）
            log.info("🌯 @Around 改写参数后: {}", Arrays.toString(args));
        }

        long start = System.currentTimeMillis();
        try {
            Object ret = pjp.proceed(args); // 让pjp（正在执行的jp）执行修改后的参数
            log.info("🌯 @Around 环绕退出 {} 耗时: {}ms 返回: {}", method, System.currentTimeMillis() - start, ret);
            return ret; // 返回新的return
        } catch (Throwable t) {
            log.info("🌯 @Around 环绕异常 {} 耗时: {}ms", method, System.currentTimeMillis() - start);
            throw t;   // 异常原样抛出，交给 @AfterThrowing 和调用方
        }
    }
}
