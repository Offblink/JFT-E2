package com.offblink.spring.dao;

import com.baomidou.mybatisplus.annotation.DbType;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import com.baomidou.mybatisplus.extension.spring.MybatisSqlSessionFactoryBean;
import com.zaxxer.hikari.HikariDataSource;
import org.apache.ibatis.logging.slf4j.Slf4jImpl;
import org.apache.ibatis.session.SqlSessionFactory;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.core.env.Environment;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import javax.sql.DataSource;

/**
 * Spring 整合 MyBatis 的 DAO 配置（课程 8 号材料 dao 块的注解版，落到本项目真实的 EmpMapper 上）
 *
 * 三件事：
 *   @PropertySource + Environment —— 连接四要素还是读 db.properties（与 mybatis-config.xml 同一份）
 *   @MapperScan                    —— 扫包注册 Mapper 代理，接口直接当 Bean 用（替代每个接口写 @Mapper）
 *   @EnableTransactionManagement   —— 声明式事务开关，@Transactional 由 DataSourceTransactionManager 执行
 */
@Configuration
@ComponentScan("com.offblink.spring.dao")   // 收编 @Service 的 EmpServiceImpl
@PropertySource("classpath:db.properties")
@MapperScan("com.offblink.lab01")   // EmpMapper / EmpJoinMapper 都在这里被代理成 Bean
@EnableTransactionManagement
public class DaoConfig {

    /** HikariCP 数据源：@PropertySource 读到的键从 Environment 注入 */
    @Bean
    public DataSource dataSource(Environment env) {
        HikariDataSource ds = new HikariDataSource();
        ds.setDriverClassName(env.getProperty("jdbc.driver"));
        ds.setJdbcUrl(env.getProperty("jdbc.url"));
        ds.setUsername(env.getProperty("jdbc.username"));
        ds.setPassword(env.getProperty("jdbc.password"));
        return ds;
    }

    /**
     * 工厂：用 MP 的 MybatisSqlSessionFactoryBean 而不是原生 SqlSessionFactoryBean——
     * 原生版配出来的是普通 Configuration，EmpMapper extends BaseMapper 的通用 CRUD 会全部失效。
     * 数据源交给 Spring 管之后，连接的获取/归还与事务同步也是 Spring 说了算（@Transactional 才管得住）。
     */
    @Bean
    public SqlSessionFactory sqlSessionFactory(DataSource dataSource) throws Exception {
        // 程序化配置，与 MyBatisUtil 的 MP 工厂同一套路：SLF4J 日志、驼峰、实体别名
        MybatisConfiguration cfg = new MybatisConfiguration();
        cfg.setLogImpl(Slf4jImpl.class);
        cfg.setMapUnderscoreToCamelCase(true);
        cfg.getTypeAliasRegistry().registerAliases("com.offblink.entity");

        MybatisSqlSessionFactoryBean factory = new MybatisSqlSessionFactoryBean();
        factory.setConfiguration(cfg);
        factory.setDataSource(dataSource);

        // XML 与接口同包同名，一条通配全部收编（EmpMapper.xml / EmpJoinMapper.xml）
        factory.setMapperLocations(new PathMatchingResourcePatternResolver()
                .getResources("classpath:com/offblink/lab01/*Mapper.xml"));

        // 分页插件与 MyBatisUtil 同款（Spring 侧的注册方式：塞进工厂的插件列表）
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
        interceptor.addInnerInterceptor(new PaginationInnerInterceptor(DbType.MYSQL));
        factory.setPlugins(interceptor);
        return factory.getObject();
    }

    /** 声明式事务的执行者：@Transactional 的开启/提交/回滚都由它落地 */
    @Bean
    public DataSourceTransactionManager transactionManager(DataSource dataSource) {
        return new DataSourceTransactionManager(dataSource);
    }
}
