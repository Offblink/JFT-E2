package com.offblink.spring;

import com.offblink.spring.aop.AopConfig;
import com.offblink.spring.dao.DaoConfig;
import com.offblink.spring.ioc.IocConfig;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

/**
 * 根配置：@Import 汇聚 IOC / AOP / DAO 三块 —— 一个容器全启动
 *
 * 用法：new AnnotationConfigApplicationContext(SpringConfig.class)
 * 全注解路线没有 web.xml 里的 ContextLoaderListener，实验二接 Web 层时它就是 contextConfigLocation 的值
 */
@Configuration
@Import({IocConfig.class, AopConfig.class, DaoConfig.class})
public class SpringConfig {
}
