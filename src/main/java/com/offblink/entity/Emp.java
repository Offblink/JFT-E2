package com.offblink.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.DecimalMin;
import javax.validation.constraints.Digits;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

/**
 * 员工实体类（指导书任务 2.2 实体类 / 任务 4.2 实体类注解映射）
 *
 * MyBatis-Plus 核心注解演示：
 *   @TableName  — 指定表名（类名与表名不一致时使用）
 *   @TableId    — 指定主键字段与主键策略（AUTO 自增，插入后回填）
 *   @TableField — 指定字段映射（属性名与列名对不上时使用；驼峰能对上的可省略）
 *
 * 对应数据库表：ssm_emp.employer
 * 表名带 schema 前缀的原因：连接串（db.properties）指向的就是 ssm_emp，这里再显式限定一次库名 ——
 * 语句自带落点，将来换数据源或临时连别的库时不会静默查到错表。
 *
 * 注意：指导书原稿在 status 上标了 @TableLogic（逻辑删除）——status 是业务字段
 *       （1 在职 0 离职），照抄会把离职员工在 MP 查询里悄悄过滤掉，故不加；
 *       逻辑删除的正确做法是独立的 deleted 字段，本实验表里没有。
 *
 * 样板代码：原来手写的 getter/setter/toString（批注原话"一大波setter和getter即将来袭……"、
 * "必不可少的toString语句"）改由 Lombok 的 @Data 一行生成，测试里的 getter 调用不变、断言零改动。
 */
@TableName("ssm_emp.employer")  //指定表名 类与表不一致时必须声明
@Data                            //生成 getter/setter/toString/equals/hashCode（课件第 6 章 MP 示例同款）
@NoArgsConstructor               //无参构造：MyBatis/MP 靠反射无参实例化实体
public class Emp {

    // 自增主键
    @TableId(value = "emp_id", type = IdType.AUTO)  //主键：列名 emp_id，自增策略
    private Integer empId;

    // 显式映射列名与属性
    @TableField("emp_name")  //列名 emp_name 与属性 empName 不一致，显式指定
    @NotBlank(message = "员工姓名不能为空")
    @Size(max = 50, message = "姓名长度不能超过 50")
    private String empName;

    // 像这种一致的，就不需要特别声明了
    @Size(max = 1, message = "性别只能是 1 个字符（男/女）")
    private String gender;   //列名与属性一致（驼峰映射接管），无需注解
    @Size(max = 50, message = "部门长度不能超过 50")
    private String dept;
    @Size(max = 50, message = "岗位长度不能超过 50")
    private String post;

    // 校验注解与业务语义一致：salary 列是 DECIMAL(10,2)，整数最多 8 位、小数 2 位
    @NotNull(message = "薪资不能为空")
    @DecimalMin(value = "0", message = "薪资不能为负数")
    @Digits(integer = 8, fraction = 2, message = "薪资格式不正确（最多 8 位整数、2 位小数）")
    private BigDecimal salary;

    @TableField("hire_date")  //列名 hire_date 与属性 hireDate 不一致，显式指定
    private Date hireDate;

    private Integer status;   //状态：1 在职 0 离职（业务字段，勿加 @TableLogic）

    /* ==================== 多表联合查询用的三个字段（实验一教师新增要求） ==================== */

    // 所属部门编号：真列（dept 表的外键 dept_id），驼峰自动映射接管，无需注解
    private Integer deptId;

    /**
     * 关联对象：多表查询时把部门的列装进这个对象（<association> 的落点）。
     *
     * @TableField(exist = false) 必须加：Emp 同时是 MyBatis-Plus 实体
     * （EmpMapper extends BaseMapper&lt;Emp&gt;），不声明"不是表字段"的话，
     * MP 会把 deptInfo 当列拼进 SQL → Unknown column 'deptInfo'。
     */
    @TableField(exist = false)
    private Dept deptInfo;

    /** 关联集合：员工掌握的技能（多对多，<collection> 的落点），同样不是表字段 */
    @TableField(exist = false)
    private List<Skill> skills;
}
