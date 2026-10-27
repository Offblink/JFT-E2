package com.offblink.lab01;

import com.offblink.entity.Dept;
import com.offblink.entity.Emp;
import com.offblink.entity.Skill;

import java.util.List;

public interface EmpJoinMapper {

    /* ==================== ① 多对一：员工带一个部门（JOIN，1 条 SQL） ==================== */

    /** 查一个员工，顺带把他的部门装进 Emp.deptInfo（一条 LEFT JOIN 搞定） */
    Emp selectEmpWithDept(Integer empId);

    /** 查全部员工，每人各带自己的部门（仍然只有 1 条 SQL —— 这正是 JOIN 相对嵌套 select 的价值） */
    List<Emp> selectAllWithDept();

    /* ==================== ② 一对多：部门带一批员工（JOIN，1 条 SQL） ==================== */

    /** 查全部部门，每个部门带自己的员工集合（<id> 把 JOIN 出来的 N 行归并成"1 个部门 + N 个员工"） */
    List<Dept> selectAllDeptWithEmps();

    /* ==================== ③ 多对多：员工带技能集合（穿中间表，1 条 SQL） ==================== */

    /** 查全部员工 + 各自会的技能（employer → employer_skill → skill 两次 LEFT JOIN） */
    List<Emp> selectAllWithSkills();

    /* ==================== N+1 反例：嵌套 select（1 + N 条 SQL，只作对照） ==================== */

    /** 多对一的嵌套 select 版：主查询查员工，再逐个员工去查他的部门 → 1 + N 条 SQL */
    List<Emp> selectAllWithDeptNested();

    /** 一对多的嵌套 select 版：主查询查部门，再逐个部门去查它的员工 → 1 + N 条 SQL */
    List<Dept> selectAllDeptWithEmpsNested();

    /** 多对多的嵌套 select 版：主查询查员工，再逐个员工去查他的技能 → 1 + N 条 SQL */
    List<Emp> selectAllWithSkillsNested();

    /* ==================== 嵌套 select 的靶子（也被用例直接调用，单独验证） ==================== */

    /** 按编号查部门（association select 引用的子查询） */
    Dept selectDeptById(Integer deptId);

    /** 按部门编号查员工列表（collection select 引用的子查询） */
    List<Emp> selectEmpsByDeptId(Integer deptId);

    /** 按员工编号查技能列表（collection select 引用的子查询，自己再做两次 JOIN 穿中间表） */
    List<Skill> selectSkillsByEmpId(Integer empId);
}
