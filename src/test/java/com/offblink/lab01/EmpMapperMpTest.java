package com.offblink.lab01;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.offblink.entity.Emp;
import com.offblink.util.MyBatisUtil;
import org.apache.ibatis.session.SqlSession;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * EmpMapper MyBatis-Plus 测试（指导书任务 4）
 *
 * 覆盖：实体类注解映射 + BaseMapper 零 SQL 通用 CRUD +
 *       条件构造器（QueryWrapper / LambdaQueryWrapper）多条件查询 + 分页插件
 *
 * 注意：本类走 MyBatisUtil.getMpFactory()——分页插件必须程序化注册，
 *       指导书 4.5 的 XML 写法构造不出 InnerInterceptor（分页会静默不生效）
 */
public class EmpMapperMpTest {

    private SqlSession sqlSession;   // 每个用例独立一个会话
    private EmpMapper empMapper;

    @BeforeEach
    public void init() {
        // 自动提交：MP 用例以零 SQL 的增删改为主，省掉每个用例手写 commit
        sqlSession = MyBatisUtil.getMpFactory().openSession(true);

        // 使用getMapper的方法获取当前mapper
        empMapper = sqlSession.getMapper(EmpMapper.class);
    }

    @AfterEach
    // 在每个会话结束后关闭会话
    public void destroy() {
        if (sqlSession != null) {
            sqlSession.close();
        }
    }

    @Test
    // 直接调用BaseMapper的方法实现0Sql增删改
    public void testBaseMapperCrud() {
        // 增：insert 是 BaseMapper 注入的，XML 里没写它。

        // 这里，我们实例化一名员工
        Emp emp = new Emp();
        emp.setEmpName("MP演练员工");
        emp.setGender("男");
        emp.setDept("技术部");
        emp.setPost("后端工程师");
        emp.setSalary(new BigDecimal("9500"));
        emp.setHireDate(new java.util.Date());
        emp.setStatus(1);

        // 影响行数
        int rows = empMapper.insert(emp);

        // 这是Junit5的断言语句，当然Java自1.4开始也支持，当然不如人家做得好——这也是为什么我们要引入Junit
        assertEquals(1, rows);
        assertNotNull(emp.getEmpId());

        // 查：selectById 按 @TableId 指定的 emp_id 取行，驼峰/注解映射把列装回属性
        Emp loaded = empMapper.selectById(emp.getEmpId());
        assertEquals("MP演练员工", loaded.getEmpName());

        // DECIMAL(10,2) 读回来带两位小数（9500.00），数值比较用 compareTo——
        // BigDecimal.equals 连 scale 一起比，"9500" 与 "9500.00" 会被判不等

        // 假设我们注释掉原来的断言，而写作原始的断言
        assertEquals(0, loaded.getSalary().compareTo(new BigDecimal("9500")));
        //assertEquals(new BigDecimal("9500"), loaded.getSalary());

        // 改：updateById 只更新赋了值的字段
        loaded.setSalary(new BigDecimal("12000"));
        assertEquals(1, empMapper.updateById(loaded));
        assertEquals(0, empMapper.selectById(emp.getEmpId()).getSalary().compareTo(new BigDecimal("12000")));

        // 删：清场
        assertEquals(1, empMapper.deleteById(emp.getEmpId()));
        assertNull(empMapper.selectById(emp.getEmpId()));
    }

    @Test
    // 使用条件构造器实现多条件查询
    public void testQueryWrapper() {
        // 字符串列名写法：ge 薪资下限 + eq 在职 + 按薪资降序
        // 第一个参数是"条件开关"：false 时该条件整段不拼接（动态拼接的常用手法）
        QueryWrapper<Emp> wrapper = new QueryWrapper<>();

        // 这里介绍几个关键字母：g大于，l小于，e等于
        // 条件构造器就是这样实现多条件查询的
        wrapper.ge("salary", 8000)
                .eq("status", 1)
                .eq(false, "dept", "不存在的部门")   // 开关为 false，不生效
                .orderByDesc("salary");

        List<Emp> list = empMapper.selectList(wrapper);

        // 极简写法，遍历输出
        list.forEach(System.out::println);

        // 在职且薪资 >= 8000：罗浩然 15500 > 蒋雨桐 11200 > 韩梦琪 8600（邱志远离职被 status 挡掉）
        assertEquals(3, list.size());
        assertEquals("罗浩然", list.get(0).getEmpName());
        assertEquals("韩梦琪", list.get(2).getEmpName());
    }

    @Test
    // λ表达式风格的条件构造器
    public void testLambdaQueryWrapper() {

        LambdaQueryWrapper<Emp> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Emp::getDept, "技术部")          // 部门 = 技术部
                .like(Emp::getEmpName, "蒋")          // 姓名模糊含"蒋"
                .eq(Emp::getStatus, 1)               // 只看在职
                .orderByDesc(Emp::getSalary);        // 薪资降序

        List<Emp> list = empMapper.selectList(wrapper);
        list.forEach(System.out::println);

        // 技术部 + 姓名含蒋 + 在职 → 蒋雨桐（同部门罗浩然不含"蒋"被 like 挡掉）
        assertEquals(1, list.size());
        assertEquals("蒋雨桐", list.get(0).getEmpName());

        // 对比：同样条件用 QueryWrapper 字符串列名也能写，Lambda 的优势是编译期就能发现列名写错
        // 就像是#和$，喜欢哪个看个人咯
        QueryWrapper<Emp> plain = new QueryWrapper<>();
        plain.eq("dept", "技术部").like("emp_name", "蒋").eq("status", 1);
        assertEquals(list.size(), empMapper.selectList(plain).size());
    }

    @Test
    // 测试分页插件
    public void testPagination() {
        // 第 1 页、每页 2 条：种子 4 行 → total=4、pages=2、本页 2 条
        Page<Emp> page = empMapper.selectPage(new Page<>(1, 2), null);

        // 关键的records变量，用于查看各种本页数据
        List<Emp> records = page.getRecords();

        records.forEach(System.out::println);
        System.out.println("总记录数 = " + page.getTotal() + "，总页数 = " + page.getPages());

        assertEquals(4, page.getTotal());
        assertEquals(2, page.getPages());

        // 分页生效的标志：records 只装本页数据而不是全量（插件没注册时这里会是 4 条）
        assertEquals(2, records.size());
        assertTrue(page.getTotal() > records.size());

        // 第 2 页：剩下 2 条
        Page<Emp> page2 = empMapper.selectPage(new Page<>(2, 2), null);
        assertEquals(2, page2.getRecords().size());
    }
}
