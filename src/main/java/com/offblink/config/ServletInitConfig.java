package com.offblink.config;

import com.offblink.spring.SpringConfig;
import org.springframework.web.filter.CharacterEncodingFilter;
import org.springframework.web.servlet.support.AbstractAnnotationConfigDispatcherServletInitializer;

import javax.servlet.Filter;
import javax.servlet.ServletRegistration;

/**
 * Web 容器初始化（取代 web.xml）：Servlet 3.0+ 的 ServletContainerInitializer 机制。
 *
 * Spring 在 spring-web 里注册了 SpringServletContainerInitializer，容器启动时它会回调
 * 本类（WebApplicationInitializer 的实现），由此注册 DispatcherServlet 与过滤器 —— 全程无 XML。
 *
 * 两个容器的分工（父子关系，子容器能看到父容器的 Bean，反之不行）：
 *   根容器  SpringConfig    → Service / Mapper / 事务 / 数据源（业务与持久层，与 Web 技术解耦）
 *   子容器  SpringMvcConfig → Controller / 拦截器 / 消息转换器（Web 层）
 * 拆开的好处：Controller 拿不到「被 web 线程污染的」业务 Bean 配置，
 * 且单元测试里只起根容器就能测 Service，不用拖着一个 Servlet 容器。
 */
public class ServletInitConfig extends AbstractAnnotationConfigDispatcherServletInitializer {

    @Override
    protected Class<?>[] getRootConfigClasses() {
        return new Class<?>[]{SpringConfig.class};
    }

    @Override
    protected Class<?>[] getServletConfigClasses() {
        return new Class<?>[]{SpringMvcConfig.class};
    }

    @Override
    protected String[] getServletMappings() {
        return new String[]{"/"};
    }

    @Override
    protected Filter[] getServletFilters() {
        // forceEncoding=true：请求与响应都按 UTF-8，中文不乱码（JSON 的 Content-Type 也带 charset）
        return new Filter[]{new CharacterEncodingFilter("UTF-8", true)};
    }

    @Override
    protected void customizeRegistration(ServletRegistration.Dynamic registration) {
        // 没有匹配的 Handler 时抛 NoHandlerFoundException，交给 GlobalExceptionHandler 统一成 JSON，
        // 而不是由容器直接吐一个 HTML 404 页面
        registration.setInitParameter("throwExceptionIfNoHandlerFound", "true");
    }
}
