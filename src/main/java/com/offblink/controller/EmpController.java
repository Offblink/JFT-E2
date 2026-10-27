package com.offblink.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.offblink.common.BusinessException;
import com.offblink.common.Result;
import com.offblink.dto.EmpQueryDTO;
import com.offblink.entity.Emp;
import com.offblink.spring.dao.EmpService;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;
import java.util.List;

/**
 * 员工管理 RESTful 接口。
 *
 * 路径与动词的语义（REST 的基本约定）：
 *   GET    /emps/{id} → 查（幂等、无副作用）
 *   POST   /emps      → 增（非幂等，每次调用产生新资源）
 *   PUT    /emps      → 改（幂等，整体替换一个已知资源）
 *   DELETE /emps/{id} → 删（幂等，删两次结果一致）
 *
 * 校验失败不写 BindingResult：统一由 GlobalExceptionHandler 把异常翻译成 Result ——
 * 十个接口只留一处错误出口，避免每个方法重复 if (br.hasErrors())。
 */
@RestController
@RequestMapping("/emps")
public class EmpController {

    private final EmpService empService;

    /** 构造器注入：依赖显式、不可变，也免去反射注入失败时的排查成本 */
    public EmpController(EmpService empService) {
        this.empService = empService;
    }

    /** 新增：主键由数据库自增，忽略前端传入的 empId */
    @PostMapping
    public Result<Emp> add(@RequestBody @Valid Emp emp) {
        emp.setEmpId(null);
        empService.registerEmployee(emp);
        return Result.ok(emp);
    }

    /** 修改：必须带主键；只更新传入的字段（MyBatis-Plus 的 updateById 语义） */
    @PutMapping
    public Result<Void> update(@RequestBody @Valid Emp emp) {
        if (emp.getEmpId() == null) {
            throw new BusinessException("修改员工时必须提供 empId");
        }
        if (!empService.updateEmployee(emp)) {
            throw new BusinessException("员工不存在：" + emp.getEmpId());
        }
        return Result.ok();
    }

    /** 单条删除 */
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Integer id) {
        if (!empService.removeEmployee(id)) {
            throw new BusinessException("员工不存在：" + id);
        }
        return Result.ok();
    }

    /** 批量删除（主键列表放在请求体里，避免超长 URL） */
    @DeleteMapping
    public Result<Integer> deleteBatch(@RequestBody List<Integer> ids) {
        if (ids == null || ids.isEmpty()) {
            throw new BusinessException("请选择要删除的员工");
        }
        return Result.ok(empService.removeEmployees(ids));
    }

    /** 单条查询：查不到按「资源不存在」处理（400 + 明确消息），而不是返回 data:null 让人猜 */
    @GetMapping("/{id}")
    public Result<Emp> getById(@PathVariable Integer id) {
        Emp emp = empService.findById(id);
        if (emp == null) {
            throw new BusinessException("员工不存在：" + id);
        }
        return Result.ok(emp);
    }

    /** 全量列表（不分页）：给前端下拉框、导出这类场景用 */
    @GetMapping("/list")
    public Result<List<Emp>> list() {
        return Result.ok(empService.findAll());
    }

    /**
     * 分页 + 多条件：name 模糊、gender/dept 精确，条件为空则不加该条件。
     * 查询条件绑进 DTO（page/size 也在其中），避免方法签名上一长串参数。
     */
    @GetMapping
    public Result<Page<Emp>> page(EmpQueryDTO query) {
        return Result.ok(empService.pageQuery(query));
    }
}
