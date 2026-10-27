package com.offblink.web;

import com.offblink.entity.Emp;
import com.offblink.spring.SpringConfig;
import com.offblink.spring.dao.EmpService;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * 声明式事务的实证（任务 6.5）：INSERT 已经执行、事务尚未提交时抛异常，整笔必须撤销。
 *
 * 断言两件事，才能证明「真的回滚」而不是「根本没插」：
 *   1. emp.empId 已回填 —— INSERT 确实发到了数据库（自增值都被消耗了）；
 *   2. 按这个主键再查 —— 查不到，说明事务把 INSERT 抹掉了。
 */
class TxRollbackTest {

    private static AnnotationConfigApplicationContext ctx;
    private static EmpService empService;

    @BeforeAll
    static void init() {
        ctx = new AnnotationConfigApplicationContext(SpringConfig.class);
        empService = ctx.getBean(EmpService.class);
    }

    @AfterAll
    static void close() {
        if (ctx != null) {
            ctx.close();
        }
    }

    @Test
    void insertIsRolledBackWhenExceptionThrown() {
        Emp emp = new Emp();
        emp.setEmpName("回滚验证-" + System.nanoTime());
        emp.setGender("男");
        emp.setDept("技术部");
        emp.setPost("临时岗");
        emp.setSalary(new BigDecimal("100.00"));
        emp.setStatus(1);

        assertThrows(IllegalStateException.class, () -> empService.saveWithRollback(emp));
        assertNotNull(emp.getEmpId(), "INSERT 已执行，自增主键应已回填");
        assertNull(empService.findById(emp.getEmpId()), "事务回滚后该行不应存在");
    }
}
