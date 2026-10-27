package com.offblink.entity;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 技能实体（多对多的另一侧，对应数据库表 ssm_emp.skill）
 *
 * 关系藏在中间表 ssm_emp.employer_skill（联合主键 emp_id + skill_id）里：
 * 一个员工会多项技能、一项技能被多个员工掌握，关系型数据库没有外键能直接表达，
 * 只能靠中间表 —— 查"某员工会什么"就是 employer → employer_skill → skill 两次 JOIN。
 *
 * 同样不是 MyBatis-Plus 实体：只是 <collection ofType="Skill"> 要的元素类型。
 */
@Data
@NoArgsConstructor
public class Skill implements Serializable {

    private Integer id;

    private String name;

    private String description;
}
