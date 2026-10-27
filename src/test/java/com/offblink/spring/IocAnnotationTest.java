package com.offblink.spring;

import com.offblink.lab01.EmpMapper;
import com.offblink.spring.aop.LogAspect;
import com.offblink.spring.dao.EmpService;
import com.offblink.spring.ioc.Car;
import com.offblink.spring.ioc.IocConfig;
import com.offblink.spring.ioc.Person;
import com.offblink.spring.ioc.Wheel;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * IOC 容器测试（课程 8 号材料 Ioc 注解版）：Bean 注册、注入方式、作用域、集合注入、生命周期、@Import 聚合
 *
 * 容器不走 XML：直接 new AnnotationConfigApplicationContext(配置类.class)，
 * close() 触发 @PreDestroy —— 这也是为什么每个用例独立开一个容器
 */
public class IocAnnotationTest {

    private AnnotationConfigApplicationContext ctx;

    @BeforeEach
    public void init() {
        ctx = new AnnotationConfigApplicationContext(IocConfig.class);
    }

    @AfterEach
    public void destroy() {
        if (ctx != null) {
            ctx.close();   // 触发 @PreDestroy（控制台能看到 [Person] ★ @PreDestroy 销毁 -> 张三）
        }
    }

    @Test
    public void testValueAndResourceInjection() {
        Car car = ctx.getBean("carBySetter", Car.class); // ioc使用代理创建bean

        // @Value 注入到 setter：三个值都进来了
        assertEquals("比亚迪", car.getBrand());
        assertEquals("秦PLUS", car.getModel());
        assertEquals(128000, car.getPrice());

        // @Resource(name = "wheelSingleton") 按名称注入对象
        assertEquals("米其林", car.getWheel().getType());
    }

    @Test
    public void testPrimaryAndSingleton() {
        Wheel w1 = ctx.getBean("wheelSingleton", Wheel.class);
        Wheel w2 = ctx.getBean(Wheel.class);   // 按类型取：@Primary 选中 wheelSingleton
        assertSame(w1, w2);                    // 单例域：两种拿法同一个实例
    }

    @Test
    public void testPrototypeScope() {
        Wheel p1 = ctx.getBean("wheelPrototype", Wheel.class);
        Wheel p2 = ctx.getBean("wheelPrototype", Wheel.class);
        assertNotSame(p1, p2);                 // 原型域：每次 getBean 都是新实例
        assertEquals("固特异", p1.getType());
    }

    @Test
    public void testCollectionInjection() {
        Person person = ctx.getBean("personRich", Person.class);

        // 字段 @Value
        assertEquals("张三", person.getName());
        assertEquals(25, person.getAge());

        // 四种集合：@Autowired + @Qualifier 从 IocConfig 的 @Bean 逐个点名注入
        assertEquals(3, person.getHobbies().size());
        assertTrue(person.getSkills().contains("Spring"));
        assertEquals(85.5, person.getScores().get("语文"));
        assertEquals("175cm", person.getHealthInfo().getProperty("height"));
    }

    @Test
    // 坑演示（排查记录素材）：@Bean 工厂方法里 new Car("保时捷", ...) 的值会被 @Value 的 setter 注入覆盖——
    // 注入后处理器对容器里所有 Bean 生效，不限 @Component 注册的那一个，拿到的是比亚迪/秦PLUS。
    // 规避方式见 Wheel：@Value 放构造器参数上，直接 new 的实例不走 Spring、传什么就是什么
    public void testBeanFactoryMethodValueOverridden() {
        Car lifecycle = ctx.getBean("carLifecycle", Car.class);
        assertEquals("比亚迪", lifecycle.getBrand());   // 不是构造参数传的保时捷

        // 实例仍然是两个：@Bean 工厂方法与 @Component 各管各的
        assertNotSame(lifecycle, ctx.getBean("carBySetter", Car.class));
    }

    @Test
    // 根配置 @Import 聚合三块：一个容器里 IOC 的 Bean、AOP 的切面、DAO 的服务与 Mapper 共存
    public void testSpringConfigImportsAllBlocks() {
        try (AnnotationConfigApplicationContext all = new AnnotationConfigApplicationContext(SpringConfig.class)) {
            assertNotNull(all.getBean(EmpService.class));      // DAO 块
            assertNotNull(all.getBean("userService"));          // AOP 块的目标服务
            assertNotNull(all.getBean(LogAspect.class));        // AOP 块的切面本身也是 Bean
            assertNotNull(all.getBean(EmpMapper.class));        // @MapperScan 生成的 Mapper 代理
            assertNotNull(all.getBean("personRich"));           // IOC 块
        }
    }
}
