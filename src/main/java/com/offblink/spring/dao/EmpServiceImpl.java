package com.offblink.spring.dao;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.offblink.dto.EmpQueryDTO;
import com.offblink.entity.Emp;
import com.offblink.lab01.EmpMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.List;

/**
 * 员工业务实现：Mapper 由 @MapperScan 代理注入，事务注解声明在方法上——
 * 对照 EmpMapperTest 里的 sqlSession.commit()：Spring 侧不用手写提交/回滚。
 */
@Service("empService")
public class EmpServiceImpl implements EmpService {

    @Autowired   // Mapper 代理是容器里的 Bean，按类型注入
    private EmpMapper empMapper;

    @Override
    @Transactional(readOnly = true)   // 只读事务：不加写锁，也省掉写回滚检查
    public List<Emp> findAll() {
        return empMapper.selectAll();
    }

    @Override
    @Transactional(readOnly = true)
    public Emp findById(Integer empId) {
        return empMapper.selectEmpById(empId);
    }

    @Override
    // rollbackFor = Exception.class：默认只对 RuntimeException 回滚，显式声明受检异常也回滚
    @Transactional(rollbackFor = Exception.class)
    public void registerEmployee(Emp emp) {
        empMapper.insertEmp(emp);   // @Options 主键回填在 Spring 侧照常工作
    }

    @Override
    @Transactional(rollbackFor = Exception.class, propagation = Propagation.REQUIRED)
    public void batchInsertWithTransaction(List<Emp> emps) {
        for (int i = 0; i < emps.size(); i++) {
            if (emps.get(i).getEmpName().contains("bad")) {
                throw new RuntimeException("第 " + (i + 1) + " 条姓名含 bad → 整批回滚!");
            }
            empMapper.insertEmp(emps.get(i));
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class, isolation = Isolation.READ_COMMITTED)
    public void transferSalary(Integer fromId, Integer toId, BigDecimal amount) {
        Emp from = empMapper.selectEmpById(fromId);
        if (from == null) {
            throw new RuntimeException("转出员工不存在: " + fromId);
        }
        Emp to = empMapper.selectEmpById(toId);
        if (to == null) {
            throw new RuntimeException("转入员工不存在: " + toId);
        }

        // 第一笔：先扣。方法内后续抛异常的话，这笔会被事务一起撤销（回滚实证在 SpringDaoTest）
        from.setSalary(from.getSalary().subtract(amount));
        empMapper.updateEmp(from);

        if (amount.compareTo(new BigDecimal("5000")) > 0) {
            throw new RuntimeException("划转金额超 5000 → 两笔一起回滚!");
        }

        // 第二笔：后加
        to.setSalary(to.getSalary().add(amount));
        empMapper.updateEmp(to);
    }

    /* ==================== 实验二新增：Web 层需要的业务能力 ==================== */

    @Override
    @Transactional(readOnly = true)
    public Page<Emp> pageQuery(EmpQueryDTO query) {
        EmpQueryDTO q = query == null ? new EmpQueryDTO() : query;

        // 边界收敛：页码至少 1；每页 1~100 条（size=0 或负数会让 MP 拼出非法 LIMIT，超尾页则自然返回空集）
        int pageNo = q.getPage() == null || q.getPage() < 1 ? 1 : q.getPage();
        int pageSize = q.getSize() == null ? 5 : Math.min(Math.max(q.getSize(), 1), 100);

        // 条件式拼接：hasText 为 false 时该条件根本不进 SQL，而不是拼一个 = null
        LambdaQueryWrapper<Emp> wrapper = new LambdaQueryWrapper<>();
        wrapper.like(StringUtils.hasText(q.getName()), Emp::getEmpName, q.getName())
                .eq(StringUtils.hasText(q.getGender()), Emp::getGender, q.getGender())
                .eq(StringUtils.hasText(q.getDept()), Emp::getDept, q.getDept())
                .orderByDesc(Emp::getEmpId);

        // 分页插件在 DaoConfig 里注册；没有它，selectPage 会静默退化成「查全部」
        return empMapper.selectPage(new Page<>(pageNo, pageSize), wrapper);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean updateEmployee(Emp emp) {
        // updateById 只拼非空字段：PUT 语义是整体替换，未传的字段保持原值
        return empMapper.updateById(emp) > 0;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean removeEmployee(Integer empId) {
        return empMapper.deleteById(empId) > 0;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int removeEmployees(List<Integer> ids) {
        return empMapper.deleteBatchIds(ids);
    }

    @Override
    // rollbackFor = Exception.class：默认只对 RuntimeException 回滚，显式声明后受检异常也回滚
    @Transactional(rollbackFor = Exception.class)
    public void saveWithRollback(Emp emp) {
        empMapper.insert(emp);
        // 插入已发出、事务尚未提交 —— 这里抛异常，Spring 会回滚整笔操作
        throw new IllegalStateException("故意抛异常：验证 INSERT 随事务一起撤销");
    }
}
