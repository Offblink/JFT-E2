package com.offblink.dto;

import lombok.Data;

/**
 * 员工列表查询条件（GET /emps 的查询参数体）。
 *
 * 为什么不直接用 Emp 实体接：
 *   实体是「数据库表的形状」（带 @TableId、关联对象、审计字段），
 *   查询条件只是「筛选项的集合」，两者职责不同。用实体接会把表结构当接口契约暴露出去，
 *   以后加字段/改映射都可能破坏前端调用。
 *
 * page/size 给默认值：前端不传就是第 1 页、每页 5 条；服务层会再把非法值夹到合法区间。
 */
@Data
public class EmpQueryDTO {

    /** 姓名：模糊匹配（like %name%） */
    private String name;

    /** 性别：精确匹配（男 / 女） */
    private String gender;

    /** 部门：精确匹配（dept 文本列） */
    private String dept;

    /** 页码，从 1 开始 */
    private Integer page = 1;

    /** 每页条数 */
    private Integer size = 5;
}
