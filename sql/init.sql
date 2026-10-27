CREATE DATABASE IF NOT EXISTS ssm_emp DEFAULT CHARACTER SET utf8mb4;
USE ssm_emp;

-- ==================== 1. 部门表：多对一 / 一对多的"一方" ====================
-- 主键手工指定 10 / 20 / 30（不是自增）：建表脚本与种子数据、外键回填都能对齐
CREATE TABLE IF NOT EXISTS dept (
    dept_id   INT PRIMARY KEY COMMENT '部门编号',
    dept_name VARCHAR(50) NOT NULL COMMENT '部门名称',
    loc       VARCHAR(50) COMMENT '办公地点'
) COMMENT '部门表：与 employer 构成 多对一 / 一对多 关系';

-- ==================== 2. 员工表（联查的"多方"，dept_id 指向 dept.dept_id） ====================
-- dept 文本列保留不动：实验一既有的多条件查询 / 动态 SQL / MP 用例都在用它做条件，
-- 新加的 dept_id 才是多表查询的关联键（两列并存是"历史文本列 + 规范化外键"的过渡形态）
CREATE TABLE IF NOT EXISTS employer (
    emp_id    INT PRIMARY KEY AUTO_INCREMENT COMMENT '员工编号',
    emp_name  VARCHAR(50) NOT NULL COMMENT '姓名',
    gender    CHAR(1) DEFAULT '男' COMMENT '性别',
    dept      VARCHAR(50) COMMENT '部门（文本列）',
    dept_id   INT COMMENT '所属部门编号（外键 → dept.dept_id，多表查询的关联键）',
    post      VARCHAR(50) COMMENT '岗位',
    salary    DECIMAL(10,2) COMMENT '薪资',
    hire_date DATE COMMENT '入职时间',
    status    TINYINT DEFAULT 1 COMMENT '状态：1在职 0离职',
    KEY fk_emp_dept (dept_id),
    CONSTRAINT fk_emp_dept FOREIGN KEY (dept_id) REFERENCES dept (dept_id)
);

-- ==================== 3. 技能表 + 中间表：多对多的物理落点 ====================
-- 一项技能被多个员工掌握、一个员工掌握多项技能 → 关系型数据库没法用外键直接表达，
-- 必须引入中间表 employer_skill（联合主键 = 两条外键，它本身就是"关系"的载体）
CREATE TABLE IF NOT EXISTS skill (
    id          INT PRIMARY KEY AUTO_INCREMENT COMMENT '技能编号',
    name        VARCHAR(50) NOT NULL COMMENT '技能名称',
    description VARCHAR(255) COMMENT '技能说明'
) COMMENT '技能表';

CREATE TABLE IF NOT EXISTS employer_skill (
    emp_id   INT NOT NULL COMMENT '员工编号（外键 → employer.emp_id）',
    skill_id INT NOT NULL COMMENT '技能编号（外键 → skill.id）',
    PRIMARY KEY (emp_id, skill_id),
    CONSTRAINT fk_es_emp   FOREIGN KEY (emp_id)   REFERENCES employer (emp_id),
    CONSTRAINT fk_es_skill FOREIGN KEY (skill_id) REFERENCES skill (id)
) COMMENT '员工-技能中间表：多对多的物理落点';

-- ==================== 4. 已有库的增量：employer.dept_id 列与外键 ====================
-- MySQL 的 ADD COLUMN / ADD CONSTRAINT 都不支持 IF NOT EXISTS（那是 MariaDB 的扩展），
-- 于是先查 information_schema 探一次，再决定拼哪条 DDL —— 整段重复执行不会报 1060 / 1826
SET @has_col := (SELECT COUNT(*) FROM information_schema.COLUMNS
                 WHERE TABLE_SCHEMA = 'ssm_emp' AND TABLE_NAME = 'employer' AND COLUMN_NAME = 'dept_id');
SET @ddl := IF(@has_col = 0,
               'ALTER TABLE employer ADD COLUMN dept_id INT NULL COMMENT ''所属部门编号（外键 → dept.dept_id）'' AFTER dept',
               'SELECT ''employer.dept_id 已存在，跳过加列'' AS note');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @has_fk := (SELECT COUNT(*) FROM information_schema.TABLE_CONSTRAINTS
                WHERE TABLE_SCHEMA = 'ssm_emp' AND TABLE_NAME = 'employer'
                  AND CONSTRAINT_NAME = 'fk_emp_dept' AND CONSTRAINT_TYPE = 'FOREIGN KEY');
SET @ddl := IF(@has_fk = 0,
               'ALTER TABLE employer ADD CONSTRAINT fk_emp_dept FOREIGN KEY (dept_id) REFERENCES dept (dept_id)',
               'SELECT ''外键 fk_emp_dept 已存在，跳过'' AS note');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- ==================== 5. 种子数据（自拟，非指导书示例数据；可重复执行） ====================
-- 被引用的先插（dept / skill），引用的后插（employer / employer_skill）
INSERT INTO dept (dept_id, dept_name, loc) VALUES
(10, '技术部', '北京'),
(20, '运营部', '上海'),
(30, '财务部', '广州')
ON DUPLICATE KEY UPDATE dept_name = VALUES(dept_name), loc = VALUES(loc);

INSERT INTO skill (id, name, description) VALUES
(1, 'Java',  '后端开发'),
(2, 'MySQL', '数据库'),
(3, 'Vue',   '前端框架'),
(4, 'Axure', '原型设计')
ON DUPLICATE KEY UPDATE name = VALUES(name), description = VALUES(description);

-- 员工：主键写死 1 ~ 4 并 ON DUPLICATE KEY UPDATE —— 重跑只更新、不新增，
-- 也保证 employer_skill 里按编号写的关系始终指向这四个人
INSERT INTO employer (emp_id, emp_name, gender, dept, dept_id, post, salary, hire_date, status) VALUES
(1, '罗浩然', '男', '技术部', 10, '系统架构师', 15500.00, '2023-04-17', 1),
(2, '蒋雨桐', '女', '技术部', 10, '交互设计师', 11200.00, '2024-02-26', 1),
(3, '韩梦琪', '女', '运营部', 20, '内容运营',    8600.00, '2022-09-05', 1),
(4, '邱志远', '男', '财务部', 30, '成本会计',    7800.00, '2021-11-08', 0)
ON DUPLICATE KEY UPDATE emp_name = VALUES(emp_name), gender = VALUES(gender), dept = VALUES(dept),
                        dept_id = VALUES(dept_id), post = VALUES(post), salary = VALUES(salary),
                        hire_date = VALUES(hire_date), status = VALUES(status);

-- 已有库的补数据：按部门名把文本列回填成外键（<=> 是 NULL 安全等号，两边都为 NULL 时不会误判）
UPDATE employer e JOIN dept d ON e.dept = d.dept_name
SET e.dept_id = d.dept_id
WHERE NOT (e.dept_id <=> d.dept_id);

-- 员工-技能关系（先按这几个人清一遍再插，可重复执行）：
--   罗浩然(1) 会 Java + MySQL，蒋雨桐(2) 会 Axure；
--   韩梦琪(3)、邱志远(4) 故意一条都不给 —— 留给 LEFT JOIN 看"没有技能的人"长什么样
DELETE FROM employer_skill WHERE emp_id IN (1, 2, 3, 4);
INSERT INTO employer_skill (emp_id, skill_id) VALUES (1, 1), (1, 2), (2, 4);


-- ==================== 6. 实验二增量：emp_name 普通索引（任务 4.1） ====================
-- 姓名是列表接口的模糊查询字段（LIKE '%x%' 用不上索引，但前缀/等值匹配能用），
-- 同时它应当唯一可检索、常被用作排序与去重的前置条件，故建普通索引。
-- MySQL 的 ADD INDEX 不支持 IF NOT EXISTS，先查 information_schema 再拼 DDL —— 整段可重复执行。
SET @has_emp_name_idx := (SELECT COUNT(*) FROM information_schema.STATISTICS
                          WHERE TABLE_SCHEMA = 'ssm_emp' AND TABLE_NAME = 'employer'
                            AND INDEX_NAME = 'idx_emp_name');
SET @ddl := IF(@has_emp_name_idx = 0,
               'ALTER TABLE employer ADD INDEX idx_emp_name (emp_name)',
               'SELECT ''idx_emp_name 已存在，跳过'' AS note');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 自检：索引就位情况（idx_emp_name + 外键 fk_emp_dept 都会列出来）
SELECT INDEX_NAME, COLUMN_NAME, NON_UNIQUE
FROM information_schema.STATISTICS
WHERE TABLE_SCHEMA = 'ssm_emp' AND TABLE_NAME = 'employer'
ORDER BY INDEX_NAME, SEQ_IN_INDEX;
