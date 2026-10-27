package com.offblink.spring.aop;

import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;

/**
 * AOP 配置：扫包收编切面与目标服务，开启 @AspectJ 注解切面支持
 */
@Configuration
@ComponentScan("com.offblink.spring.aop")
@EnableAspectJAutoProxy   // 动态代理的开关：目标 Bean 被包上代理，切面逻辑才有落脚点
public class AopConfig {
}
