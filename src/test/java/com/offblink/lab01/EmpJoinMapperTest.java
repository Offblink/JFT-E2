package com.offblink.lab01;

import com.baomidou.mybatisplus.core.MybatisSqlSessionFactoryBuilder;
import com.offblink.entity.Dept;
import com.offblink.entity.Emp;
import com.offblink.entity.Skill;
import org.apache.ibatis.executor.statement.StatementHandler;
import org.apache.ibatis.io.Resources;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.plugin.Interceptor;
import org.apache.ibatis.plugin.Intercepts;
import org.apache.ibatis.plugin.Invocation;
import org.apache.ibatis.plugin.Signature;
import org.apache.ibatis.reflection.MetaObject;
import org.apache.ibatis.reflection.SystemMetaObject;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.sql.Connection;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class EmpJoinMapperTest {

    /**
     * SQL 计数插件：拦在 StatementHandler.prepare 上，只数"真的发给数据库的语句"。
     * 插件链是每次开会话（newExecutor）时才套到执行器上的，所以 build 之后再 addInterceptor 也生效。
     */
    @Intercepts(@Signature(type = StatementHandler.class, method = "prepare",
            args = {Connection.class, Integer.class}))
    public static class SqlCounter implements Interceptor {

        private final List<String> statementIds = new ArrayList<>();

        @Override
        public Object intercept(Invocation invocation) throws Throwable {
            // 从被代理对象上拿到本次要执行的语句 id（RoutingStatementHandler.delegate.mappedStatement）
            MetaObject meta = SystemMetaObject.forObject(invocation.getTarget());
            MappedStatement ms = (MappedStatement) meta.getValue("delegate.mappedStatement");
            statementIds.add(ms.getId());
            return invocation.proceed();
        }

        void reset() {
            statementIds.clear();
        }

        int count() {
            return statementIds.size();
        }

        List<String> statementIds() {
            return new ArrayList<>(statementIds);
        }
    }

    private static final SqlCounter counter = new SqlCounter();

    private static SqlSessionFactory factory;   // 本类专用：带计数插件的工厂（不动 MyBatisUtil 的全局单例）

    private SqlSession sqlSession;
    private EmpJoinMapper joinMapper;

    @BeforeAll
    public static void initFactory() throws Exception {
        // 与 MyBatisUtil.getFactory() 同一个配置文件，只是额外挂一个计数插件做取证
        try (InputStream in = Resources.getResourceAsStream("mybatis-config.xml")) {
            factory = new MybatisSqlSessionFactoryBuilder().build(in);
        }
        factory.getConfiguration().addInterceptor(counter);
    }

    @BeforeEach
    public void init() {
        sqlSession = factory.openSession();
        joinMapper = sqlSession.getMapper(EmpJoinMapper.class); // 从此获得一个empJoinMapper的对象实例
    }

    @AfterEach
    public void destroy() {
        if (sqlSession != null) {
            sqlSession.close();
        }
    }

    /** 跑一段查询、打出条数，返回条数（顺手把语句 id 也打出来，方便对着控制台截图讲） */
    private int runCounted(String scene, Runnable query) {
        counter.reset();
        query.run();
        int n = counter.count();
        System.out.println("【" + scene + "】SQL " + n + " 条 → " + counter.statementIds());
        return n;
    }

    /* ==================== ① 多对一：员工 + 部门 ==================== */

    @Test
    public void testEmpWithDept() {
        System.out.println("========== ① 多对一：一条 LEFT JOIN 把员工与他的部门一次查回来 ==========");

        counter.reset();
        Emp emp = joinMapper.selectEmpWithDept(1);
        int sql = counter.count();
        System.out.println("【多对一（查 1 个员工 + 部门）】SQL " + sql + " 条 → " + counter.statementIds());

        assertEquals(1, sql);
        assertEquals("罗浩然", emp.getEmpName());

        // <association> 生效的证据：部门被装成了一个对象，而不是几个散落的前缀列
        assertNotNull(emp.getDeptInfo());
        assertEquals("技术部", emp.getDeptInfo().getDeptName());
        assertEquals("北京", emp.getDeptInfo().getLoc());

        // 员工自己的外键来自别名列 emp_dept_id（不起别名就会被部门的 dept_id 抢走）
        assertEquals(Integer.valueOf(10), emp.getDeptId());
    }

    @Test
    public void testAllWithDeptJoinVsNested() {
        System.out.println("========== ① 多对一：JOIN 版 vs 嵌套 select 版（N+1 对照） ==========");

        int joinSql = runCounted("JOIN 版（4 个员工 + 各自部门）", () -> joinMapper.selectAllWithDept());
        int nestedSql = runCounted("嵌套 select 版（同上的需求）", () -> joinMapper.selectAllWithDeptNested());

        List<Emp> joinList = joinMapper.selectAllWithDept();
        List<Emp> nestedList = joinMapper.selectAllWithDeptNested();

        assertEquals(4, joinList.size());

        // 一条 SQL 就够：这就是 JOIN 的价值（不是"少写代码"，是少打数据库 N 次）
        assertEquals(1, joinSql);

        // 嵌套 select 版：1 条主查询 + 每个"关联键"一次子查询。
        // 注意不是 1+4：蒋雨桐与罗浩然同部门（dept_id=10），第二条子查询命中一级缓存、没再查库 ——
        // 所以真实条数是 1 + 去重后的部门个数，数 SQL 要按实测，不能机械按 1+N 算。
        long distinctDept = joinList.stream().map(Emp::getDeptId).distinct().count();

        // 假如我们再把现在的版本注掉，并改回原来机械的版本
        assertEquals(1 + distinctDept, nestedSql);
        //assertEquals(1+joinList.size(),nestedSql);

        // 两条路线拿到的数据必须一致，否则"优化"就等于改了语义
        assertEquals(joinList.size(), nestedList.size());
        for (int i = 0; i < joinList.size(); i++) {
            assertEquals(joinList.get(i).getEmpId(), nestedList.get(i).getEmpId());
            assertNotNull(nestedList.get(i).getDeptInfo());
            assertEquals(joinList.get(i).getDeptInfo().getDeptName(),
                    nestedList.get(i).getDeptInfo().getDeptName());
        }
        System.out.println("两条路线数据一致，SQL 条数：" + joinSql + " vs " + nestedSql);
    }

    /* ==================== ② 一对多：部门 + 员工列表 ==================== */

    @Test
    public void testDeptWithEmps() {
        System.out.println("========== ② 一对多：一条 LEFT JOIN + <collection>，把 N 行归并成「1 个部门 + N 个员工」 ==========");

        int sql = runCounted("一对多（3 个部门 + 各自员工）", () -> joinMapper.selectAllDeptWithEmps());
        List<Dept> depts = joinMapper.selectAllDeptWithEmps();

        assertEquals(1, sql);

        // 归并的证据：JOIN 出来是 4 行（员工数），但结果是 3 个部门对象 —— 靠 dept 的 <id> 合并的
        assertEquals(3, depts.size());

        Dept tech = depts.get(0);
        assertEquals(Integer.valueOf(10), tech.getDeptId());
        assertEquals("技术部", tech.getDeptName());
        assertEquals("北京", tech.getLoc());
        assertEquals(2, tech.getEmps().size());          // 技术部两个人
        assertEquals("罗浩然", tech.getEmps().get(0).getEmpName());
        assertEquals("蒋雨桐", tech.getEmps().get(1).getEmpName());
        assertEquals(Integer.valueOf(10), tech.getEmps().get(0).getDeptId());

        assertEquals("运营部", depts.get(1).getDeptName());
        assertEquals("韩梦琪", depts.get(1).getEmps().get(0).getEmpName());
        assertEquals("财务部", depts.get(2).getDeptName());
        assertEquals("邱志远", depts.get(2).getEmps().get(0).getEmpName());

        System.out.println("技术部员工名单：" + tech.getEmps());
    }

    @Test
    public void testDeptWithEmpsJoinVsNested() {
        System.out.println("========== ② 一对多：JOIN 版 vs 嵌套 select 版（N+1 对照） ==========");

        int joinSql = runCounted("JOIN 版（3 个部门 + 员工）", () -> joinMapper.selectAllDeptWithEmps());
        int nestedSql = runCounted("嵌套 select 版（同上）", () -> joinMapper.selectAllDeptWithEmpsNested());

        List<Dept> joinList = joinMapper.selectAllDeptWithEmps();
        List<Dept> nestedList = joinMapper.selectAllDeptWithEmpsNested();

        assertEquals(1, joinSql);
        assertEquals(1 + joinList.size(), nestedSql);   // 3 个部门各查一次员工 → 1 + 3

        assertEquals(joinList.size(), nestedList.size());
        for (int i = 0; i < joinList.size(); i++) {
            assertEquals(joinList.get(i).getDeptName(), nestedList.get(i).getDeptName());
            assertEquals(joinList.get(i).getEmps().size(), nestedList.get(i).getEmps().size());
        }
    }

    /* ==================== ③ 多对多：员工 + 技能（穿中间表） ==================== */

    @Test
    public void testEmpWithSkills() {
        System.out.println("========== ③ 多对多：两次 LEFT JOIN 穿过中间表 employer_skill ==========");

        int sql = runCounted("多对多（4 个员工 + 各自技能）", () -> joinMapper.selectAllWithSkills());
        List<Emp> emps = joinMapper.selectAllWithSkills();

        assertEquals(1, sql);
        assertEquals(4, emps.size());

        Emp luo = emps.get(0);
        assertEquals("罗浩然", luo.getEmpName());
        assertEquals(2, luo.getSkills().size());
        assertEquals("Java", luo.getSkills().get(0).getName());
        assertEquals("MySQL", luo.getSkills().get(1).getName());

        Emp jiang = emps.get(1);
        assertEquals("蒋雨桐", jiang.getEmpName());
        assertEquals(1, jiang.getSkills().size());
        assertEquals("Axure", jiang.getSkills().get(0).getName());

        // LEFT JOIN 没关联上的人：集合是空的 —— 不是 null，也不会造出"一个全 null 的技能对象"
        // （MyBatis 判断嵌套结果的所有列都为 null 就跳过该元素）
        Emp han = emps.get(2);
        assertEquals("韩梦琪", han.getEmpName());
        assertNotNull(han.getSkills());
        assertTrue(han.getSkills().isEmpty());

        System.out.println("罗浩然技能：" + luo.getSkills());
        System.out.println("韩梦琪技能（中间表里一条关系都没有）：" + han.getSkills());
    }

    @Test
    public void testEmpWithSkillsJoinVsNested() {
        System.out.println("========== ③ 多对多：JOIN 版 vs 嵌套 select 版（N+1 对照） ==========");

        int joinSql = runCounted("JOIN 版（4 个员工 + 技能）", () -> joinMapper.selectAllWithSkills());
        int nestedSql = runCounted("嵌套 select 版（同上）", () -> joinMapper.selectAllWithSkillsNested());

        List<Emp> joinList = joinMapper.selectAllWithSkills();
        List<Emp> nestedList = joinMapper.selectAllWithSkillsNested();

        assertEquals(1, joinSql);
        assertEquals(1 + joinList.size(), nestedSql);   // 4 个员工各查一次技能 → 1 + 4（各人编号不同，没有缓存可蹭）

        assertEquals(joinList.size(), nestedList.size());
        for (int i = 0; i < joinList.size(); i++) {
            assertEquals(joinList.get(i).getEmpId(), nestedList.get(i).getEmpId());
            assertEquals(joinList.get(i).getSkills().size(), nestedList.get(i).getSkills().size());
        }
    }

    /* ==================== 子查询单独验证（嵌套 select 的靶子） ==================== */

    @Test
    public void testSubQueries() {
        System.out.println("========== 子查询（嵌套 select 的靶子，也可以单独调用） ==========");

        Dept dept = joinMapper.selectDeptById(20);
        assertEquals("运营部", dept.getDeptName());
        assertEquals("上海", dept.getLoc());

        // 单独调用子查询同样只发一条 SQL
        counter.reset();
        List<Emp> emps = joinMapper.selectEmpsByDeptId(10);
        int firstSql = counter.count();
        System.out.println("【selectEmpsByDeptId(10)】SQL " + firstSql + " 条：" + counter.statementIds());

        assertEquals(2, emps.size());
        assertEquals("罗浩然", emps.get(0).getEmpName());
        assertEquals(Integer.valueOf(10), emps.get(0).getDeptId());   // resultType + 驼峰映射也认得 dept_id
        assertEquals(1, firstSql);

        // 同一会话里再调一次同样的语句 → 0 条 SQL：命中 SqlSession 一级缓存（statement + 参数 完全相同）
        joinMapper.selectEmpsByDeptId(10);
        int secondSql = counter.count() - firstSql;
        System.out.println("【再调一次同样的子查询】新增 SQL " + secondSql + " 条（一级缓存命中，没查库）");
        assertEquals(0, secondSql);

        List<Skill> skills = joinMapper.selectSkillsByEmpId(1);
        assertEquals(2, skills.size());
        assertEquals("Java", skills.get(0).getName());
        assertEquals(0, joinMapper.selectSkillsByEmpId(3).size());    // 韩梦琪：空集合
    }
}
