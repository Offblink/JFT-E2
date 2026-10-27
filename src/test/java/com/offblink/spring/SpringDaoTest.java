package com.offblink.spring;

import com.offblink.entity.Emp;
import com.offblink.lab01.EmpMapper;
import com.offblink.spring.dao.DaoConfig;
import com.offblink.spring.dao.EmpService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Spring DAO 测试（课程 8 号材料 dao 注解版）：@MapperScan 注入 + @Transactional 提交/回滚实证
 *
 * 用例纪律与 EmpMapperTest 一致：自造数据、断言、清场，净残留 0。
 * 回滚类用例的关键：第一笔 UPDATE 明明执行了，异常后必须查不到痕迹 —— 这就是声明式事务的实证
 */
public class SpringDaoTest {

    private AnnotationConfigApplicationContext ctx;
    private EmpService empService;
    private EmpMapper empMapper;   // 清场用（同一个容器里 @MapperScan 生成的代理）

    @BeforeEach
    public void init() {
        ctx = new AnnotationConfigApplicationContext(DaoConfig.class);
        empService = ctx.getBean(EmpService.class);
        empMapper = ctx.getBean(EmpMapper.class);
    }

    @AfterEach
    public void destroy() {
        if (ctx != null) {
            ctx.close();
        }
    }

    /** 造一行演练员工（字段全给上） */
    private Emp newEmp(String name, String dept, String salary) {
        Emp e = new Emp();
        e.setEmpName(name);
        e.setGender("男");
        e.setDept(dept);
        e.setPost("演练岗");
        e.setSalary(new BigDecimal(salary));
        e.setHireDate(new Date());
        e.setStatus(1);
        return e;
    }

    @Test
    public void testFindAllReadOnly() {
        // 只读事务 + 种子 4 行
        List<Emp> all = empService.findAll();
        all.forEach(System.out::println); // 全部输出

        // 为了验证事务执行结果的正确性
        assertEquals(4, all.size());
        assertNotNull(all.get(0).getEmpName());
    }

    @Test
    public void testRegisterEmployee() {
        Emp emp = newEmp("Spring入职", "Spring演练部", "9000");
        empService.registerEmployee(emp);          // 事务内提交，不用手写 commit

        assertNotNull(emp.getEmpId());             // 主键回填在 Spring 侧照常工作
        Emp loaded = empService.findById(emp.getEmpId());
        assertEquals("Spring入职", loaded.getEmpName());

        // 清场
        empMapper.deleteEmpById(emp.getEmpId());
    }

    @Test
    // 正常路径：两笔 UPDATE 都提交，余额一减一加
    public void testTransferSalaryCommit() {
        Emp a = newEmp("划转甲", "划转演练部", "1000");
        Emp b = newEmp("划转乙", "划转演练部", "1000");
        empMapper.insertEmp(a);                    // 事务外的 Mapper 调用：逐条自动提交
        empMapper.insertEmp(b);

        empService.transferSalary(a.getEmpId(), b.getEmpId(), new BigDecimal("500"));

        assertEquals(0, empService.findById(a.getEmpId()).getSalary().compareTo(new BigDecimal("500")));
        assertEquals(0, empService.findById(b.getEmpId()).getSalary().compareTo(new BigDecimal("1500")));

        // 清场
        empMapper.deleteEmpById(a.getEmpId());
        empMapper.deleteEmpById(b.getEmpId());
    }

    @Test
    // 回滚实证：第一笔扣款已执行完毕，超限异常触发后必须被整体撤销
    public void testTransferSalaryRollback() {
        Emp a = newEmp("回滚甲", "回滚演练部", "1000");
        Emp b = newEmp("回滚乙", "回滚演练部", "1000");
        empMapper.insertEmp(a);
        empMapper.insertEmp(b);

        // 8000 超过 5000 上限：方法内先扣了款、后抛异常
        assertThrows(RuntimeException.class,
                () -> empService.transferSalary(a.getEmpId(), b.getEmpId(), new BigDecimal("8000")));

        // 两笔都没落库：金额原封不动
        assertEquals(0, empService.findById(a.getEmpId()).getSalary().compareTo(new BigDecimal("1000")));
        assertEquals(0, empService.findById(b.getEmpId()).getSalary().compareTo(new BigDecimal("1000")));

        // 清场
        empMapper.deleteEmpById(a.getEmpId());
        empMapper.deleteEmpById(b.getEmpId());
    }

    @Test
    // 批量插入全有或全无：夹带 bad 命名的员工，前面已插的也一并回滚
    public void testBatchInsertRollback() {
        List<Emp> emps = Arrays.asList(
                newEmp("批演甲", "批演部", "6000"),
                newEmp("批演乙", "批演部", "7000"),
                newEmp("bad员工", "批演部", "8000"));

        assertThrows(RuntimeException.class, () -> empService.batchInsertWithTransaction(emps));

        // 回查批演部：0 条——没提交的中间结果在事务外不存在
        Emp cond = new Emp();
        cond.setDept("批演部");
        assertEquals(0, empMapper.selectByCondition(cond).size());
    }
}
