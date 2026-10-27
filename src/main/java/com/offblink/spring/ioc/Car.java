package com.offblink.spring.ioc;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

/**
 * 演示：@Value 注入到 setter（属性注入）、@Resource 按名称注入对象
 */
@Component("carBySetter")
public class Car {

    private String brand;
    private String model;
    private int price;
    private Wheel wheel;   // 对象属性：注入的不是值，而是一个容器里的 Bean

    // 必不可少的无参构造器
    public Car() {
        System.out.println("  [Car] 无参构造器");
    }

    public Car(String brand, String model, int price) {   // 供 IocConfig 的 @Bean 工厂方法使用
        this.brand = brand;
        this.model = model;
        this.price = price;
    }

    // 对象注入的三种姿势（课件注解版要点）：
    //   1. @Autowired 按类型注入——同类型容器里只能有一个
    //   2. @Autowired + @Qualifier 按「类型 + 名称」注入
    //   3. @Resource 按名称注入——点名要哪个 Bean

    // 对象属性
    @Resource(name = "wheelSingleton")
    public void setWheel(Wheel wheel) {
        this.wheel = wheel;
        System.out.println("  [Car] @Resource 注入 Wheel -> " + wheel.getType());
    }

    @Value("比亚迪")   // 默认值
    public void setBrand(String brand) {
        this.brand = brand;
    }

    @Value("秦PLUS")
    public void setModel(String model) {
        this.model = model;
    }

    @Value("128000")
    public void setPrice(int price) {
        this.price = price;
    }

    public String getBrand() {
        return brand;
    }

    public String getModel() {
        return model;
    }

    public int getPrice() {
        return price;
    }

    public Wheel getWheel() {
        return wheel;
    }

    @Override
    public String toString() {
        return "Car{brand='" + brand + "', model='" + model + "', price=" + price
                + ", wheel=" + wheel + "}";
    }
}
