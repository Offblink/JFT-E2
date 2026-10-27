package com.offblink.spring.ioc;

import lombok.Data;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

/**
 * 轮胎组件：演示 @Component 注册 + 构造器 @Value 注入 + @Primary 默认选择 + @Scope 作用域
 *
 * 值注入放在构造器参数上而不是字段上：字段/@setter 上的 @Value 会被注入后处理器施加到
 * 「容器里的每一个 Wheel」，IocConfig 里 new Wheel("固特异", 18) 的值会被覆盖（对照 Car 的坑）
 */
@Component("wheelSingleton")  // 组件注解，id 显式指定（不写默认类名首字母小写）
@Primary                      // 同类型多个 Bean 时按类型注入的默认选择
@Scope("singleton")           // 默认作用域：整个容器共一个实例
@Data
public class Wheel {

    private final String type;   // final：只能走构造器赋值，杜绝事后覆盖
    private final int size;

    // 构造器参数上的 @Value 只在「容器实例化」（@Component 这条路）时生效；
    // IocConfig 里直接 new Wheel(...) 不经过 Spring，传什么就是什么
    public Wheel(@Value("米其林") String type, @Value("17") int size) {
        this.type = type;
        this.size = size;
    }
}
