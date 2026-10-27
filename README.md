# JFT-E2 · 实验二（SSM 整合 + EMP 员工管理 MIS）

《Java 框架技术（一）》（AI 赋能版）课程实验二：**AI 赋能 SSM 框架整合与 EMP 员工管理 MIS 系统开发**。

在实验一（MyBatis / MyBatis-Plus / Spring 全注解）的工程基础上**增量**补上 SpringMVC 一层，
集齐 SSM，并把它做成一个能跑起来、接口逐个可验证的员工管理后端。

| 名字 | 是什么 |
|---|---|
| `JFT-E1` | 实验一：MyBatis + MyBatis-Plus + Spring IOC/AOP/DAO 全注解 |
| `JFT-E2` | 本仓：实验一代码原样继承 + SpringMVC 表现层 + 健壮性 + 事务实证 |
| `mybatis01` | 课程项目那一支，跟课件做的第 1–5 章练习 |

## 技术栈

| 类别 | 选型 | 说明 |
|---|---|---|
| 语言 / JDK | Java 8 语法目标（本机 JDK 17 编译） | 与实验一一致 |
| 构建 | Maven 3.9.x，`war` 打包（产物 `JFT-E2.war`） | 课程要求 war；同时保留内嵌运行能力 |
| Web 容器 | **Tomcat 9.0.83**（`tomcat-embed-core`，免安装） | Servlet 4.0 / javax 命名空间 |
| 表现层 | SpringMVC 5.3.31（`@EnableWebMvc` + `WebMvcConfigurer`） | 无 web.xml，走 `ServletContainerInitializer` |
| 业务 / 事务 | Spring 5.3.31 `@Service` + `@Transactional` | 事务由 `DataSourceTransactionManager` 执行 |
| 持久层 | MyBatis-Plus 3.5.3.1（`BaseMapper` + `LambdaQueryWrapper` + 分页插件） | 继承实验一 |
| 连接池 | HikariCP 4.0.3 | Spring 默认搭档；4.x 是 Java 8 的上限 |
| 校验 | Hibernate Validator 6.2.5.Final | **必须 6.2.x**：7.x 已迁到 `jakarta.validation`，与 Tomcat 9 的 javax 栈不兼容 |
| JSON | Jackson 2.15.3（`jackson-bom` 收敛） | 全局日期 `yyyy-MM-dd`、忽略未知字段 |
| 日志 | SLF4J 1.7.36 + Logback 1.2.13 | 控制台 + `logs/mybatis.log` |
| 测试 | JUnit 5.10.2（surefire 3.2.5） | 45 个用例 |
| 数据库 | MySQL 8/9，库 `ssm_emp` | 沿用实验一四张表 |

## 与指导书不同、且刻意为之的地方

| 项 | 指导书 | 本工程 | 为什么 |
|---|---|---|---|
| Web 初始化 | `web.xml` + `ContextLoaderListener` | `AbstractAnnotationConfigDispatcherServletInitializer` | Servlet 3.0+ 无 XML 初始化是当前主流，也与实验一「零 XML 容器配置」一致 |
| 连接池 | Druid | HikariCP | Spring Boot 默认池；实验一已选它，不来回换 |
| 测试框架 | JUnit 4 | JUnit 5 | 实验一已统一 JUnit 5 |
| 日志 | —（示例用 `System.out`） | SLF4J + Logback | 实验中 `System.out.println` 打日志无法分级、无法落文件 |
| 校验失败出口 | 每个方法 `if (br.hasErrors())` | 全局异常处理器统一翻译 | 10 个接口只留 1 处错误出口，避免重复样板 |
| 运行方式 | 装 Tomcat 9 再部署 | `mvn package exec:exec`（内嵌 Tomcat 9） | 免安装；同时 war 产物可在真机 Tomcat 9 上部署 |
| 包结构 | `com.lab.emp.*` | `com.offblink.*`（沿用实验一） | 直接复用实验一的 entity / mapper / service，不产生两套同名实体 |

### 为什么不用 `tomcat7-maven-plugin`

试过，不成立，理由是硬的：该插件最后版本 2013 年的 2.2，编译期按 Tomcat 7 API 写死了
`Tomcat.setDefaultRealm(Realm)`，而该方法在 Tomcat 8 就被删除了 —— 把 `tomcat-embed-core`
顶到 9.x 必然 `NoSuchMethodError`（实测）；用它自带的 Tomcat 7 又只有 Servlet 3.0，
低于 Spring 5.3 要求的 3.1。所以本工程用 40 行的 `EmbeddedTomcatLauncher` 直接拉起 embed-core 9。

## 快速开始

```bash
# 1. 建库建表 + 种子 + 索引（可重复执行；沿用实验一的数据，另加 emp_name 索引）
mysql -u root -p < sql/init.sql

# 2. 连接配置：src/main/resources/db.properties（占位符改成你自己的）
#    仓库里只有 CHANGE_ME，真实口令走 skip-worktree、不进 Git

# 3. 跑全量测试（45 个用例，需要本机 MySQL）
mvn test

# 4. 启动（先打包出展开的 war，再由内嵌 Tomcat 9 部署；端口 8082）
mvn package exec:exec
#   改端口：改 pom 的 tomcat.port，或 mvn package exec:exec -Dexec.args="-Dserver.port=8090"

# 5. 部署到独立 Tomcat 9（可选）
#   mvn package  → target/JFT-E2.war 丢进 %CATALINA_HOME%\webapps
```

打开 <http://localhost:8082/> 是接口清单页；演示账号 `admin / 123456`。

> 端口为什么是 8082：本机 8080 与 8081 被 `xerj`（本地检索节点）占用。

## 接口

| 方法 | 路径 | 说明 |
|---|---|---|
| POST | `/login` | `{"username":"admin","password":"123456"}`，成功写 Session |
| GET | `/logout` | 销毁会话 |
| GET | `/emps` | 分页 + 多条件：`name`（模糊）、`gender`、`dept`、`page`、`size` |
| GET | `/emps/{id}` | 单条查询 |
| GET | `/emps/list` | 全量列表 |
| POST | `/emps` | 新增（`@Valid` 校验） |
| PUT | `/emps` | 修改（必须带 `empId`；只改传入字段） |
| DELETE | `/emps/{id}` | 单条删除 |
| DELETE | `/emps` | 批量删除，body 为 id 数组 |

统一返回：`{"code":200,"message":"success","data":…}`。
HTTP 状态码与业务码一致：200 成功 / 400 参数或业务失败 / 401 未登录 / 404 路径不存在 / 500 兜底。

## 分层与关键文件

```
src/main/java/com/offblink/
├── config/
│   ├── ServletInitConfig.java     # 取代 web.xml：根容器 + 子容器 + 编码过滤器
│   └── SpringMvcConfig.java       # @EnableWebMvc、CORS、拦截器、Jackson 转换器、切面开关
├── controller/                    # EmpController（RESTful 七件事）、AuthController（登录）
├── service → spring/dao/          # EmpService / EmpServiceImpl（实验一的包，增量加方法）
├── mapper   → lab01/EmpMapper     # extends BaseMapper，实验一原样继承
├── entity/                        # Emp（新增 JSR-303 注解）、Dept、Skill
├── dto/EmpQueryDTO.java           # 查询条件（name/gender/dept/page/size）
├── common/                        # Result、BusinessException、GlobalExceptionHandler
├── interceptor/LoginInterceptor   # 未登录 → 401 JSON
├── aspect/WebLogAspect            # @Around 记录入参/耗时/异常
└── launcher/EmbeddedTomcatLauncher# 内嵌 Tomcat 9 启动器
```

**父子容器分工**：根容器 `SpringConfig` 管 Service / Mapper / 数据源 / 事务；
子容器 `SpringMvcConfig` 只扫 `controller` 与 `aspect` 两个包。
两边的组件扫描范围不重叠，同一个 Bean 不会在父子容器里各注册一份。
AOP 切面必须与目标 Bean 同容器（Spring AOP 是容器内的代理），所以日志切面放在子容器。

## 健壮性（任务 6）

- **参数校验**：`Emp` 上 `@NotBlank/@Size/@NotNull/@DecimalMin/@Digits`，Controller 参数 `@Valid`。
- **全局异常**：业务异常、校验失败、JSON 解析失败、类型不匹配、方法不支持、404、兜底异常，
  全部翻译成 `Result`，兜底异常只记日志、对外统一文案。
- **登录拦截**：`LoginInterceptor` 拦 `/**`，放行 `/`、`/index.html`、`/login`、`/logout`、静态资源。
- **请求日志**：`WebLogAspect` 的 `@Around` 记录 Controller 入参、耗时、异常。
- **声明式事务**：写方法 `@Transactional(rollbackFor = Exception.class)`。
  `saveWithRollback` 是专门的回滚证据：INSERT 执行后抛异常，自增值已消耗、行却查不到。

### 事务失效的三个经典场景（本工程有意避开）

| 场景 | 现象 | 原因 |
|---|---|---|
| 异常被 `try-catch` 吞掉 | 不回滚 | 事务管理器靠异常感知失败，没有异常就照常提交 |
| 同类内部 `this.method()` 自调用 | 内层方法的事务注解不生效 | 调用绕过代理对象，直接进真实方法 |
| 注解加在 `private` 方法上 | 完全不生效 | Spring AOP 只代理可重写的方法（public/protected/包可见） |

## 测试

45 个用例：实验一的 34 个（Mapper / MP / 联查 / IOC / AOP / DAO）+ 新增 11 个。

| 用例类 | 覆盖 |
|---|---|
| `web/EmpQueryServiceTest` | 姓名模糊、性别+部门精确、空条件查全部、页码/size 边界收敛、超尾页返回空 |
| `web/TxRollbackTest` | INSERT 已执行（主键回填）但事务回滚后查不到 |
| `web/EmpValidationTest` | 空姓名、负薪资、空薪资、超长姓名被拒；合法对象通过 |

## 踩过的坑（都已在代码注释里标注）

1. **`tomcat7-maven-plugin` + Tomcat 9 = `NoSuchMethodError`**：见上文，改用内嵌启动器。
2. **SCI 父类加载器 / 进程内执行**：`exec:java` 在 Maven 进程内跑，webapp 类加载器默认父加载器是 Maven 的，
   扫不到 `javax.servlet` → `ClassNotFoundException: javax.servlet.ServletContainerInitializer`，
   `DispatcherServlet` 不注册、所有接口 404。显式 `context.setParentClassLoader(...)` 解决。
3. **部署目录必须是展开的 war**：直接从 `src/main/webapp` 起服务时依赖都在 JVM 类路径上、
   不在 `WEB-INF/lib` 里，SCI 扫不到 —— 用 `mvn package exec:exec`。
4. **Hibernate Validator 7 不兼容**：7.x 的包名是 `jakarta.validation`，`javax.validation.*` 编译失败，锁 6.2.5.Final。
5. **`class` 配置与 `mapperLocations`**：本工程的 Mapper XML 在实验一同包同名路径下，沿用其配置。
6. **`@Valid` 静默不生效**：纯 Spring（非 Boot）下 `@EnableWebMvc` 不保证 `WebDataBinder` 里有校验器，
   注解在、校验没跑，空姓名照样入库。解法是显式提供 `WebMvcConfigurer.getValidator()`（见 `SpringMvcConfig`）。
7. **EL 依赖**：Hibernate Validator 6.x 的消息插值要 `javax.el.ExpressionFactory`，缺了直接
   `HV000183` / `NoClassDefFoundError: javax/el/ELManager`，`tomcat-embed-el` 必须是 provided（真实 Tomcat 自带）。
8. **`configureDefaultServletHandling` 会让 404 变成 HTML**：开了它，未匹配路径交给容器 DefaultServlet，
   与本工程「所有响应都是 Result JSON」的契约冲突；首页改由 `HomeController` 输出。

## License

[MIT](LICENSE)
