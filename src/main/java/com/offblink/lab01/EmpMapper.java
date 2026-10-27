package com.offblink.lab01;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.offblink.entity.Emp;
import org.apache.ibatis.annotations.*;

import java.util.List;

/**
 * 员工 Mapper 接口（指导书任务 2.3 / 任务 3 / 任务 4.3）
 *
 * 本接口是「注解 + XML」混合写法（对应课件第 3 节注解开发、第 4 节动态 SQL）：
 *   · 静态单表 CRUD → 注解：@Select / @Insert / @Update / @Delete 写静态 SQL，
 *     主键回填 @Options(useGeneratedKeys, keyProperty)，
 *     结果映射 @ResultMap 引用 XML 里定义的 empMap（注解写 SQL、映射复用 XML——课件 4-2
 *     讲的"跨方式引用"）；
 *   · 动态 SQL → XML：注解里拼动态标签可读性极差（课件结论：复杂动态 SQL 99% 写 XML），
 *     指导书任务 3 的 <if>/<where>/<set>/<choose>/<foreach> 全部落在 EmpMapper.xml。
 *
 * MyBatis-Plus 的核心：BaseMapper<T>;
 *   继承 BaseMapper 后，无需写任何 SQL，自动拥有通用 CRUD（本实验用到的）：
 *
 *     —— 插入 ——
 *     insert(T entity)                       插入一条记录（自增主键回填）
 *
 *     —— 删除 ——
 *     deleteById(Serializable id)            根据主键删除
 *     deleteBatchIds(Collection<?>)         根据主键批量删除
 *
 *     —— 更新 ——
 *     updateById(T entity)                   根据主键更新（未赋值字段不动）
 *
 *     —— 查询 ——
 *     selectById(Serializable id)            根据主键查询
 *     selectList(Wrapper<T> wrapper)         按条件构造器查列表
 *     selectPage(IPage<T>, Wrapper<T>)      分页查询（需先注册分页插件，见 MyBatisUtil）
 *
 * 自定义方法名刻意避开 BaseMapper 同名（insert / selectById / updateById / deleteById），
 * 否则通用方法注入与自家语句会相互冲突。
 */
public interface EmpMapper extends BaseMapper<Emp> {

    /* ==================== 基础 CRUD：注解方式（课件第 3 节） ==================== */

    /**
     * 查询所有员工。
     * 注解 SQL 里写 SELECT * 也没问题——列怎么进属性由 empMap 统一管，
     * 这正是 resultMap 与 resultType 的区别（映射规则集中在一处）
     */
    @Select("SELECT * FROM ssm_emp.employer")
    @ResultMap("com.offblink.lab01.EmpMapper.empMap")  // 引用 XML 的 empMap：带点号的值按原样使用
    List<Emp> selectAll();

    /** 根据员工编号查询（#{} 预编译占位，防注入） */
    @Select("SELECT * FROM ssm_emp.employer WHERE emp_id = #{empId}")
    @ResultMap("com.offblink.lab01.EmpMapper.empMap")
    Emp selectEmpById(Integer empId);

    /** 新增员工：@Options 等价 XML 的 useGeneratedKeys="true" keyProperty="empId"，自增主键回填 */
    @Insert("INSERT INTO ssm_emp.employer (emp_name, gender, dept, post, salary, hire_date, status) " +
            "VALUES (#{empName}, #{gender}, #{dept}, #{post}, #{salary}, #{hireDate}, #{status})")
    @Options(useGeneratedKeys = true, keyProperty = "empId")
    int insertEmp(Emp emp);

    /** 更新员工：全字段覆盖式更新（选择性更新在 XML 的 updateDynamic） */
    @Update("UPDATE ssm_emp.employer SET emp_name = #{empName}, gender = #{gender}, dept = #{dept}, " +
            "post = #{post}, salary = #{salary}, status = #{status} WHERE emp_id = #{empId}")
    int updateEmp(Emp emp);

    /** 根据员工编号删除 */
    @Delete("DELETE FROM ssm_emp.employer WHERE emp_id = #{empId}")
    int deleteEmpById(Integer empId);

    /* ==================== 动态 SQL：XML 方式（指导书任务 3） ==================== */

    /** 1. 多条件任意组合查询（<if> + <where>），语句在 EmpMapper.xml */
    List<Emp> selectByCondition(Emp cond);

    /** 2. 等效条件查询（<trim> 自定义裁剪，where 标签就是 trim 的特例） */
    List<Emp> selectByTrim(Emp cond);

    /** 3. 选择性更新（<set>：只更新非空字段） */
    int updateDynamic(Emp emp);

    /** 4. 批量删除（<foreach> 拼 IN 条件） */
    int deleteBatch(@Param("ids") List<Integer> ids);

    /** 5. 批量插入（<foreach> 拼多值 VALUES） */
    int insertBatch(@Param("list") List<Emp> emps);

    /** 6. 分支选择（<choose> / <when> / <otherwise>）：优先按姓名，其次按部门，否则查全部 */
    List<Emp> selectByChoice(@Param("empName") String empName, @Param("dept") String dept);
}
