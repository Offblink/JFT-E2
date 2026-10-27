package com.offblink.spring;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.offblink.spring.aop.AopConfig;
import com.offblink.spring.aop.UserService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.aop.support.AopUtils;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import java.util.List;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * AOP 测试（课程 8 号材料 Aop 注解版）：代理成立、五种通知触发、@Around 改参
 *
 * 断言方式：给 LogAspect 的 Logback 日志器挂 ListAppender ——
 * 切面本来就打日志，日志内容即断言依据，不用为测试改一行业务代码
 */
public class AopAnnotationTest {

    private AnnotationConfigApplicationContext ctx;
    private UserService userService;
    private Logger aspectLogger;                      // LogAspect 的 Logback 日志器
    private ListAppender<ILoggingEvent> appender;     // 挂上去的内存 Appender：截获的事件都存在 list 里

    @BeforeEach
    public void init() {
        ctx = new AnnotationConfigApplicationContext(AopConfig.class);
        userService = ctx.getBean("userService", UserService.class);

        // 容器里拿到的必须是代理对象，否则切面根本没挂上（@EnableAspectJAutoProxy 的效果）
        assertTrue(AopUtils.isAopProxy(userService));

        aspectLogger = (Logger) LoggerFactory.getLogger("com.offblink.spring.aop.LogAspect");
        appender = new ListAppender<>();
        appender.start();
        aspectLogger.addAppender(appender);
    }

    @AfterEach
    public void destroy() {
        aspectLogger.detachAppender(appender);        // 拆掉，别影响其它测试类的日志
        if (ctx != null) {
            ctx.close();
        }
    }

    /** 截获的切面日志正文 */
    private List<String> messages() {
        return appender.list.stream()
                .map(ILoggingEvent::getFormattedMessage)
                .collect(Collectors.toList());
    }

    @Test
    public void testAdviceChainOnSave() {
        userService.saveUser("张三");

        List<String> msgs = messages();
        // 正常方法走满一圈：环绕进入/退出 + 前置 + 返回 + 最终
        // λ函数，输出包含关键词说明执行成功
        assertTrue(msgs.stream().anyMatch(m -> m.contains("@Around") && m.contains("环绕进入")));
        assertTrue(msgs.stream().anyMatch(m -> m.contains("@Before")));
        assertTrue(msgs.stream().anyMatch(m -> m.contains("@AfterReturning")));
        assertTrue(msgs.stream().anyMatch(m -> m.contains("@After ")));
        assertTrue(msgs.stream().anyMatch(m -> m.contains("环绕退出")));
    }

    @Test
    // @Around 的改参能力：findUser 实际收到的是被改写后的 "[已加密]张三"，返回值随之变化
    public void testAroundModifiesArgs() {
        assertEquals("用户[[已加密]张三]", userService.findUser("张三"));
    }

    @Test
    // 异常路径：@AfterThrowing 与 @Around 的异常分支都触发，异常原样抛给调用方
    public void testAfterThrowing() {
        RuntimeException ex = assertThrows(RuntimeException.class, userService::riskyOperation);
        assertTrue(ex.getMessage().contains("数据库连接失败"));

        List<String> msgs = messages();
        assertTrue(msgs.stream().anyMatch(m -> m.contains("@AfterThrowing")));
        assertTrue(msgs.stream().anyMatch(m -> m.contains("环绕异常")));
    }

    @Test
    public void testDeleteReturnsNormally() {
        assertEquals(1, userService.deleteUser("李四"));
    }
}
