package com.offblink.web;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.offblink.dto.EmpQueryDTO;
import com.offblink.entity.Emp;
import com.offblink.spring.SpringConfig;
import com.offblink.spring.dao.EmpService;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 分页 + 多条件查询（任务 5.1）的实证。
 *
 * 只读用例：只起根容器（Service + Mapper + 数据源），不拖 Servlet 容器。
 * 断言不依赖「库里总共几行」——种子之外可能有手工数据，所以一律断言
 * 「结果非空 + 每行都满足筛选条件」，而不是写死条数。
 */
class EmpQueryServiceTest {

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
    void nameFilterIsFuzzy() {
        EmpQueryDTO query = new EmpQueryDTO();
        query.setName("罗浩");       // 种子数据是「罗浩然」，只给一半也算命中
        query.setSize(100);

        Page<Emp> page = empService.pageQuery(query);

        assertFalse(page.getRecords().isEmpty(), "种子数据里应有 罗浩然");
        page.getRecords().forEach(e ->
                assertTrue(e.getEmpName().contains("罗浩"), "不该出现不匹配的行：" + e.getEmpName()));
    }

    @Test
    void genderAndDeptFiltersAreExact() {
        EmpQueryDTO query = new EmpQueryDTO();
        query.setGender("女");
        query.setDept("技术部");
        query.setSize(100);

        Page<Emp> page = empService.pageQuery(query);

        assertFalse(page.getRecords().isEmpty(), "种子数据里应有 蒋雨桐（技术部/女）");
        page.getRecords().forEach(e -> {
            assertEquals("女", e.getGender());
            assertEquals("技术部", e.getDept());
        });
    }

    @Test
    void emptyConditionsQueryEverything() {
        EmpQueryDTO query = new EmpQueryDTO();
        query.setSize(100);

        Page<Emp> page = empService.pageQuery(query);

        assertTrue(page.getTotal() >= 4, "四个种子员工至少要都在");
        assertEquals(page.getTotal(), page.getRecords().size(), "条件为空且 size 足够大时应一次取全");
    }

    @Test
    void illegalPageAndSizeAreClamped() {
        EmpQueryDTO query = new EmpQueryDTO();
        query.setPage(0);    // 页码从 1 开始，0 视为第 1 页
        query.setSize(0);    // size=0 会让 SQL 拼出非法 LIMIT，夹到 1

        Page<Emp> page = empService.pageQuery(query);

        assertEquals(1, page.getCurrent());
        assertEquals(1, page.getSize());
    }

    @Test
    void oversizedPageSizeIsClampedToHundred() {
        EmpQueryDTO query = new EmpQueryDTO();
        query.setSize(100000);

        Page<Emp> page = empService.pageQuery(query);

        assertEquals(100, page.getSize());
    }

    @Test
    void pageBeyondLastReturnsEmptyRecords() {
        EmpQueryDTO query = new EmpQueryDTO();
        query.setPage(9999);
        query.setSize(10);

        Page<Emp> page = empService.pageQuery(query);

        assertTrue(page.getRecords().isEmpty(), "超出尾页应返回空列表而不是报错");
        assertTrue(page.getTotal() > 0, "总条数仍然要正确");
    }
}
