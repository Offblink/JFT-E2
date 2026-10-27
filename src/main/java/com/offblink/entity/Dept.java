package com.offblink.entity;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.io.Serializable;
import java.util.List;

/**
 * 部门实体（多表联合查询里的"一方"，对应数据库表 ssm_emp.dept）
 *
 * 为什么要新加这张表：实验一教师新增要求「一定要有多表联合查询」，而原来的 employer 只有一个
 * 部门"文本"列（'技术部'），没有第二张表就没有可连的东西 —— 于是补上 dept 表与
 * employer.dept_id 外键列（建表、种子、外键都写在 sql/实验一/ssm_emp.sql 里，可重复执行）。
 *
 * 与 Emp 的区别：本类不是 MyBatis-Plus 实体（没有 @TableName/@TableId），
 * 它只服务关联查询的结果封装，字段与 dept 表一一对应；
 * 另外多一个 {@link #emps} 用来装"一对多"查出来的员工集合（<collection> 的 property 指向它）。
 */
@Data
@NoArgsConstructor
@ToString(exclude = "emps")  // 打印部门时不连带刷出整队员工（名单由用例自己遍历输出，避免刷屏）

// 想要二级缓存？请先实现序列化类
public class Dept implements Serializable {

    // 部门编号（dept 表里是手工指定的 10/20/30，不是自增）
    private Integer deptId;

    private String deptName;

    private String loc;  // 办公地点

    /** 一对多：一个部门下的多个员工（<collection ofType="Emp"> 的落点） */
    private List<Emp> emps;
}
