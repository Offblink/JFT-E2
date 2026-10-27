package com.offblink.util;

import com.baomidou.mybatisplus.annotation.DbType;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.MybatisSqlSessionFactoryBuilder;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import com.offblink.lab01.EmpMapper;
import org.apache.ibatis.datasource.pooled.PooledDataSource;
import org.apache.ibatis.io.Resources;
import org.apache.ibatis.logging.slf4j.Slf4jImpl;
import org.apache.ibatis.mapping.Environment;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.transaction.jdbc.JdbcTransactionFactory;

import java.io.InputStream;
import java.util.Properties;

/**
 * MyBatis 会话工厂工具类（指导书步骤 2.4：封装工厂，避免每个测试重复构建）
 *
 * 生命周期（课件核心概念）：配置只读一次 → SqlSessionFactory 全局唯一 →
 *                          SqlSession 每次开、用完必须关（非线程安全，类似 connection）
 *
 * 本项目采用双工厂设计，原因有两个硬约束：
 * <ul>
 *   <li>{@link #getFactory()}：由 mybatis-config.xml 驱动（任务 2 的全局配置文件），
 *       服务 XML 版基础 CRUD 与动态 SQL 测试。用 MP 的 MybatisSqlSessionFactoryBuilder
 *       构建——它内部是 MybatisConfiguration，对普通 Mapper 完全兼容；</li>
 *   <li>{@link #getMpFactory()}：程序化配置，专供 MyBatis-Plus 测试，绕开两个坑：
 *       ① 分页插件必须代码注册（MybatisPlusInterceptor + PaginationInnerInterceptor），
 *          指导书 4.5 那种 XML <property name="@..."> 写法构造不出 InnerInterceptor，
 *          分页会静默不生效、返回全量数据；
 *       ② 程序化配置不读 mybatis-config.xml，实体别名要手动 registerAliases。</li>
 * </ul>
 */

// 这里给几个总流程：
// 读配置文件->建工厂->用工厂new会话->会话getMapper->mapper操作->最后关会话

public final class MyBatisUtil {

    /** 通用工厂：读 mybatis-config.xml（XML 版 CRUD / 动态 SQL 用） */
    private static volatile SqlSessionFactory factory;

    /** MP 专用工厂：MybatisConfiguration 程序化构建 + 分页插件（任务 4 用） */
    private static volatile SqlSessionFactory mpFactory;

    private MyBatisUtil() {
    }

    /** 通用工厂：全局只构建一次 */
    public static SqlSessionFactory getFactory() {
        if (factory == null) {
            synchronized (MyBatisUtil.class) {
                if (factory == null) {

                    // 输入流读入配置文件
                    try (InputStream in = Resources.getResourceAsStream("mybatis-config.xml")) {
                        factory = new MybatisSqlSessionFactoryBuilder().build(in); // 建工厂了
                    } catch (Exception e) {
                        throw new IllegalStateException("构建 SqlSessionFactory 失败", e);
                    }
                }
            }
        }
        return factory;
    }

    /** 开会话：用完必须 close（线程不安全） */
    public static SqlSession openSession() {
        return getFactory().openSession();
    }

    /** MP 分页专用工厂：程序化配置 + 分页插件（正确姿势，见类头注释） */
    public static SqlSessionFactory getMpFactory() {
        if (mpFactory == null) {
            synchronized (MyBatisUtil.class) {
                if (mpFactory == null) {

                    // 首先写入必须的配置
                    MybatisConfiguration cfg = new MybatisConfiguration();
                    cfg.setLogImpl(Slf4jImpl.class);          // SQL 日志走 SLF4J → Logback
                    cfg.setMapUnderscoreToCamelCase(true);      // 与 mybatis-config.xml 保持一致
                    cfg.setEnvironment(new Environment("mp", new JdbcTransactionFactory(), pooledDataSource()));
                    cfg.getTypeAliasRegistry().registerAliases("com.offblink.entity");
                    cfg.addMapper(EmpMapper.class);

                    // 分页插件：MybatisPlusInterceptor 是外壳，分页能力挂在 PaginationInnerInterceptor 上
                    MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
                    interceptor.addInnerInterceptor(new PaginationInnerInterceptor(DbType.MYSQL));

                    // 也加进配置
                    cfg.addInterceptor(interceptor);

                    mpFactory = new MybatisSqlSessionFactoryBuilder().build(cfg); // 也建工厂
                }
            }
        }
        return mpFactory;
    }

    /** 连接四要素外置在 db.properties（与 mybatis-config.xml 同源） */
    private static PooledDataSource pooledDataSource() {

        // 读入数据库配置
        try (InputStream in = Resources.getResourceAsStream("db.properties")) {
            Properties p = new Properties();

            // 加载配置
            p.load(in);
            // 你看咯，这就是一个构造方法，本类由这四个参数组成
            return new PooledDataSource(
                    p.getProperty("jdbc.driver"),
                    p.getProperty("jdbc.url"),
                    p.getProperty("jdbc.username"),
                    p.getProperty("jdbc.password"));

        } catch (Exception e) {
            throw new IllegalStateException("读取 db.properties 失败", e);
        }
    }
}
