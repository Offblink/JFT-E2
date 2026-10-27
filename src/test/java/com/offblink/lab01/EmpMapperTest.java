package com.offblink.lab01;

import com.offblink.entity.Emp;
import com.offblink.util.MyBatisUtil;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * EmpMapper 测试（指导书任务 2 基础 CRUD·注解实现 + 任务 3 动态 SQL·XML 实现）
 *
 * mybatis 中的核心类（课件概念）：
 *   SqlSessionFactory —— 数据库连接工厂，全局唯一，MyBatisUtil 懒加载构建一次
 *   SqlSession        —— 数据库会话，类似 connection 对象，非线程安全，用完必须关闭
 *
 *       约定：
 *       查询类断言基于自拟的 4 行种子数据（罗浩然/蒋雨桐/韩梦琪/邱志远，按老师要求不使用指导书文档里的示例数据）
 */
public class EmpMapperTest {
    private SqlSession sqlSession;   // 每个用例独立一个会话
    private EmpMapper empMapper;     // Mapper 代理对象（框架动态生成实现）

    // 以下两个方法与mpTest基本一致，不赘述了
    @BeforeEach  // JUnit5：测试方法执行前执行
    public void init() {
        sqlSession = MyBatisUtil.openSession();
        empMapper = sqlSession.getMapper(EmpMapper.class);
    }

    @AfterEach  // 测试方法执行后执行：关闭会话，没 commit 的写操作随 close 回滚
    public void destroy() {
        if (sqlSession != null) {
            sqlSession.close();
        }
    }

    /** 造一行演练用的员工（每个写库用例的公共入口，字段全给上） */
    private Emp newEmp(String name, String gender, String dept, String post, String salary) {
        Emp e = new Emp();
        e.setEmpName(name);
        e.setGender(gender);
        e.setDept(dept);
        e.setPost(post);
        e.setSalary(new BigDecimal(salary));
        e.setHireDate(new Date());
        e.setStatus(1);
        return e;
    }

    /* ==================== 任务 2：基础 CRUD ==================== */

    @Test
    public void testSelectAll() {
        // 为什么返回的是 List<Emp>？XML 里 resultMap="empMap" 指定了封装类型
        List<Emp> list = empMapper.selectAll();
        for (Emp e : list) {
            System.out.println(e);
        }

        assertEquals(4, list.size());

        // resultMap 生效的证据：emp_name / hire_date 这些对不上名的列都进了属性
        assertNotNull(list.get(0).getEmpName());
        assertNotNull(list.get(0).getHireDate());
    }

    @Test
    // 测试CRUD的生命周期
    public void testCrudLifecycle() {
        Emp emp = newEmp("演练员工", "男", "技术部", "运维工程师", "9000");

        // 增：@Options(useGeneratedKeys, keyProperty) 把自增主键回填进 emp.empId
        int rows = empMapper.insertEmp(emp);
        assertEquals(1, rows);
        assertNotNull(emp.getEmpId());

        // 改：这些getter和setter已经由lombok自动生成
        emp.setPost("高级运维工程师");
        emp.setSalary(new BigDecimal("11000"));
        assertEquals(1, empMapper.updateEmp(emp));

        // 查：确认修改落库
        Emp updated = empMapper.selectEmpById(emp.getEmpId());
        System.out.println("更新后查询：" + updated);
        assertEquals("高级运维工程师", updated.getPost());

        // 删：清场，净残留 0
        assertEquals(1, empMapper.deleteEmpById(emp.getEmpId()));
        assertNull(empMapper.selectEmpById(emp.getEmpId()));
        sqlSession.commit(); // 没了MP，连commit都要自己来😭
    }

    /* ==================== 任务 3：动态 SQL ==================== */

    @Test
    public void testSelectByCondition() {
        System.out.println("========== 测试 <if> + <where> 多条件动态查询 ==========");

        // 新员工包含组合条件：技术部 + 女性
        Emp cond = new Emp();
        cond.setGender("女");
        cond.setDept("技术部");

        List<Emp> hit = empMapper.selectByCondition(cond);
        System.out.println("技术部+女：" + hit);
        assertEquals(1, hit.size());
        assertEquals("蒋雨桐", hit.get(0).getEmpName());

        // 单条件模糊查询：姓名含"梦" → 韩梦琪
        Emp like = new Emp();
        like.setEmpName("梦");
        assertEquals("韩梦琪", empMapper.selectByCondition(like).get(0).getEmpName());

        // 全空条件：where 标签不产生 WHERE 子句 → 全表 4 条
        List<Emp> all = empMapper.selectByCondition(new Emp());
        System.out.println("空条件全表：" + all.size() + " 条");
        assertEquals(4, all.size());
    }

    @Test
    // 与上一条用例同组条件，验证 trim 与 where 等效
    public void testSelectByTrim() {

        // 新员工（和上一个方法一致）
        Emp cond = new Emp();
        cond.setGender("女");
        cond.setDept("技术部");

        // 只是用了新方法查询
        List<Emp> hit = empMapper.selectByTrim(cond);
        assertEquals(1, hit.size());
        assertEquals("蒋雨桐", hit.get(0).getEmpName());

        // 空条件同样退化为全表
        assertEquals(4, empMapper.selectByTrim(new Emp()).size());
    }

    @Test
    // 测试<set>动态更新非null字段
    public void testUpdateDynamic() {
        Emp emp = newEmp("动态更新演练", "女", "行政部", "行政专员", "7000");
        empMapper.insertEmp(emp);

        // 只带 empId + salary + post 三个字段：其余字段保持原值（set 跳过 null）
        Emp patch = new Emp();
        patch.setEmpId(emp.getEmpId());
        patch.setSalary(new BigDecimal("8800"));
        patch.setPost("主管");

        assertEquals(1, empMapper.updateDynamic(patch));

        Emp loaded = empMapper.selectEmpById(emp.getEmpId());
        System.out.println("动态更新后：" + loaded);

        // DECIMAL(10,2) 读回来带两位小数，数值比较用 compareTo（equals 连 scale 一起比）
        assertEquals(0, loaded.getSalary().compareTo(new BigDecimal("8800")));
        assertEquals("主管", loaded.getPost());

        // 没传的字段没被动过
        assertEquals("动态更新演练", loaded.getEmpName());
        assertEquals("行政部", loaded.getDept());

        // 清场
        assertEquals(1, empMapper.deleteEmpById(emp.getEmpId()));
        sqlSession.commit();
    }

    @Test
    public void testInsertBatch() {
        // 我们现在new三名员工，并一次性批量插入
        Emp e1 = newEmp("批量甲", "男", "批量演练部", "岗位一", "6000");
        Emp e2 = newEmp("批量乙", "女", "批量演练部", "岗位二", "7000");
        Emp e3 = newEmp("批量丙", "男", "批量演练部", "岗位三", "8000");

        // 一次 INSERT 语句插三组 VALUES，只往返数据库一次
        int rows = empMapper.insertBatch(java.util.Arrays.asList(e1, e2, e3));
        System.out.println("批量插入影响行数：" + rows);
        assertEquals(3, rows);

        // 回查验证：按部门标记查出刚插的 3 行
        Emp cond = new Emp();
        cond.setDept("批量演练部");
        List<Emp> loaded = empMapper.selectByCondition(cond);
        loaded.forEach(System.out::println); // 每个输出
        assertEquals(3, loaded.size());

        // 清场：借批量删除把 3 行一起收走
        List<Integer> ids = new java.util.ArrayList<>();
        for (Emp e : loaded) {
            ids.add(e.getEmpId());
        }

        assertEquals(3, empMapper.deleteBatch(ids));
        assertEquals(0, empMapper.selectByCondition(cond).size());
        sqlSession.commit();
    }

    @Test
    public void testDeleteBatch() {
        Emp e1 = newEmp("批量删除甲", "男", "删除演练部", "岗位一", "6000");
        Emp e2 = newEmp("批量删除乙", "女", "删除演练部", "岗位二", "7000");

        empMapper.insertEmp(e1);
        empMapper.insertEmp(e2);

        List<Integer> ids = java.util.Arrays.asList(e1.getEmpId(), e2.getEmpId());
        int rows = empMapper.deleteBatch(ids);
        System.out.println("批量删除影响行数：" + rows);
        assertEquals(2, rows);

        // 逐个回查确认删干净
        assertNull(empMapper.selectEmpById(e1.getEmpId()));
        assertNull(empMapper.selectEmpById(e2.getEmpId()));
        sqlSession.commit();
    }

    @Test
    // 这个方法统一测试choose，when和otherwise
    public void testSelectByChoice() {
        // 分支 1：给了姓名 → 精确按姓名查（choose 互斥，后面的分支不再拼）
        List<Emp> byName = empMapper.selectByChoice("罗浩然", null);
        assertEquals(1, byName.size());
        assertEquals("罗浩然", byName.get(0).getEmpName());

        // 分支 2：没给姓名、给了部门 → 按部门查（运营部只有韩梦琪一人）
        List<Emp> byDept = empMapper.selectByChoice(null, "运营部");
        assertEquals(1, byDept.size());
        assertEquals("韩梦琪", byDept.get(0).getEmpName());

        // 分支 3：都没给 → otherwise 兜底查全部
        List<Emp> all = empMapper.selectByChoice(null, null);
        System.out.println("otherwise ：" + all.size() + " 条");
        assertEquals(4, all.size());
    }
}
