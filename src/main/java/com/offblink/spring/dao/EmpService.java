package com.offblink.spring.dao;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.offblink.dto.EmpQueryDTO;
import com.offblink.entity.Emp;

import java.math.BigDecimal;
import java.util.List;

/**
 * 员工业务接口：Spring 声明式事务的演示载体（查询只读、单条提交、批量与超限回滚）
 */
public interface EmpService {

    List<Emp> findAll();

    Emp findById(Integer empId);

    /** 正常提交：插入成功后自增主键已回填进 emp.empId */
    void registerEmployee(Emp emp);

    /** 全有或全无：列表里出现姓名含 bad 的员工 → 整批回滚，一条不落 */
    void batchInsertWithTransaction(List<Emp> emps);

    /** 薪资划转：扣款与入账两笔 UPDATE 同生共死；金额超 5000 抛异常 → 两笔一起回滚 */
    void transferSalary(Integer fromId, Integer toId, BigDecimal amount);

    /* ==================== 实验二新增：Web 层需要的业务能力 ==================== */

    /**
     * 分页 + 多条件查询：name 模糊、gender/dept 精确，条件为空则该条件不参与。
     * page/size 的合法区间由实现里夹紧（page ≥ 1，1 ≤ size ≤ 100）。
     */
    Page<Emp> pageQuery(EmpQueryDTO query);

    /** 按主键更新（只改传入的非空字段），返回是否命中行 */
    boolean updateEmployee(Emp emp);

    /** 单条删除，返回是否命中行 */
    boolean removeEmployee(Integer empId);

    /** 批量删除，返回实际删除行数 */
    int removeEmployees(List<Integer> ids);

    /**
     * 事务回滚演示：插入一条后故意抛运行时异常。
     * 不加 @Transactional 时库里会留下这行；加了就必须整笔撤销 —— 用例见 TxRollbackTest。
     * 不对外暴露接口，只作为事务机制的实证。
     */
    void saveWithRollback(Emp emp);
}
