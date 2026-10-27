package com.offblink.spring.ioc;

import lombok.Data;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

/**
 * 演示：字段 @Value、集合注入（List/Set/Map/Properties）、生命周期回调
 */
@Component("personRich")
@Data
public class Person {

    @Value("张三")
    private String name;

    @Value("25")
    private int age;

    // 四种集合类型：List 有序可重复 / Set 去重 / Map 键值对 / Properties 本质也是 Map
    private List<String> hobbies;
    private Set<String> skills;
    private Map<String, Double> scores;
    private Properties healthInfo;

    // 集合注入：容器里同类型的集合 Bean 用 @Qualifier 点名（IocConfig 里 @Bean 定义的四个集合）
    @Autowired
    @Qualifier("hobbies")
    public void setHobbies(List<String> hobbies) {
        this.hobbies = hobbies;
    }

    @Autowired
    @Qualifier("skills")
    public void setSkills(Set<String> skills) {
        this.skills = skills;
    }

    @Autowired
    @Qualifier("scores")
    public void setScores(Map<String, Double> scores) {
        this.scores = scores;
    }

    @Autowired
    @Qualifier("healthInfo")
    public void setHealthInfo(Properties healthInfo) {
        this.healthInfo = healthInfo;
    }

    @PostConstruct   // 初始化回调：Bean 全部装配完成后执行（仅 singleton 在创建时触发）
    public void init() {
        System.out.println("  [Person] ★ @PostConstruct 初始化 -> " + name);
    }

    @PreDestroy      // 销毁回调：容器 close 时执行
    public void destroy() {
        System.out.println("  [Person] ★ @PreDestroy 销毁 -> " + name);
    }
}
