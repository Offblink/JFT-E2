package com.offblink.spring.ioc;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

/**
 * IOC 容器配置（全注解，替代 XML 容器配置文件）
 *
 * 两条注册路线：
 *   @ComponentScan —— 扫包收编 @Component 族注解的类（Wheel / Car / Person）
 *   @Bean          —— 工厂方法，把自由构造的对象或集合交给容器管理
 */
@Configuration   // 标识这是一个配置类，等价于一份 XML 容器配置
@ComponentScan("com.offblink.spring.ioc")
public class IocConfig {

    // 原型域：每次 getBean 都 new 一个新实例（对照 wheelSingleton 的单例）
    @Bean(name = "wheelPrototype")
    @Scope("prototype")
    public Wheel wheelPrototype() {
        return new Wheel("固特异", 18);
    }

    // @Bean 与 @Component 并存：同一个 Car 类容器里两个 Bean。
    // 注意这里 new 的值会被 @Value setter 注入覆盖（拿到比亚迪不是保时捷）——坑演示，测试有断言留档
    @Bean(name = "carLifecycle")
    public Car carLifecycle() {
        return new Car("保时捷", "911", 1500000);
    }

    // 集合类 Bean：配合 Person 的 @Autowired + @Qualifier 注入
    @Bean(name = "hobbies")
    public List<String> hobbies() {
        List<String> list = new ArrayList<>();
        list.add("游泳");
        list.add("编程");
        list.add("读书");
        return list;
    }

    @Bean(name = "skills")
    public Set<String> skills() {
        return new HashSet<>(Arrays.asList("Java", "Spring", "MyBatis"));
    }

    @Bean(name = "scores")
    public Map<String, Double> scores() {
        Map<String, Double> map = new HashMap<>();
        map.put("语文", 85.5);
        map.put("数学", 92.0);
        return map;
    }

    @Bean(name = "healthInfo")
    public Properties healthInfo() {
        Properties p = new Properties();
        p.setProperty("height", "175cm");
        p.setProperty("weight", "70kg");
        return p;
    }
}
