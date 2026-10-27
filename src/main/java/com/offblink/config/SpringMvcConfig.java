package com.offblink.config;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.offblink.interceptor.LoginInterceptor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.validation.Validator;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.List;

/**
 * SpringMVC 子容器配置（Web 层）。
 *
 * 扫描范围只有 controller 与 aspect 两个包 —— 业务层在根容器里，两个容器各扫各的，
 * 同一个 Bean 不会在父子容器里出现两份（双份 Mapper/Service 会导致事务代理、数据源各成一套）。
 *
 * 切面为什么放子容器：Spring AOP 是「容器内」的代理机制。@EnableAspectJAutoProxy 只代理
 * 当前容器里的 Bean，放根容器的切面找不到子容器的 Controller —— 日志切面必须与目标同容器。
 */
@Configuration
@EnableWebMvc
@EnableAspectJAutoProxy(proxyTargetClass = true)
// common 必须扫：@RestControllerAdvice（全局异常）就在那里，漏掉它业务异常会直接冒到容器变成 500 HTML
@ComponentScan({"com.offblink.controller", "com.offblink.aspect", "com.offblink.common"})
public class SpringMvcConfig implements WebMvcConfigurer {

    /**
     * 刻意不开启 configureDefaultServletHandling：
     * 一旦把未匹配的请求交给容器 DefaultServlet，任何「路径写错」都会得到 Tomcat 的 HTML 404，
     * 与本工程「所有响应都是 Result JSON」的契约不一致。
     * 关掉之后未匹配 → NoHandlerFoundException → GlobalExceptionHandler → 统一 JSON 404。
     * 首页由 HomeController 直接输出（静态文件仍在 webapp 下，改成它来读）。
     */

    /** 前后端分离场景的跨域许可（ApiFox / 独立前端页面直连时不被拦） */
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOriginPatterns("*")
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .allowCredentials(true)
                .maxAge(3600);
    }

    @Bean
    public LoginInterceptor loginInterceptor() {
        return new LoginInterceptor();
    }

    /**
     * JSR-303 校验器。
     *
     * 纯 Spring（非 Spring Boot）下 @EnableWebMvc 只注册了 mvcValidator 这个 Bean，
     * 而 WebDataBinder 的校验器取自 DelegatingWebMvcConfiguration.getValidator()
     * → 各 WebMvcConfigurer 的 getValidator()。不显式提供的话 binder 里没有校验器，
     * @Valid 会被静默跳过 —— 空姓名、负薪资照样入库（实测踩过）。
     */
    @Bean
    public LocalValidatorFactoryBean validator() {
        return new LocalValidatorFactoryBean();
    }

    @Override
    public Validator getValidator() {
        return validator();
    }

    /** 登录拦截：业务接口一律要登录；登录/登出、首页与静态资源放行 */
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(loginInterceptor())
                .addPathPatterns("/**")
                .excludePathPatterns("/", "/index.html", "/login", "/logout", "/favicon.ico",
                        "/css/**", "/js/**", "/images/**");
    }

    /**
     * 自定义 JSON 转换器。
     * 用 extend 而不是 configure：后者会把 Spring 默认的一整套转换器（String、字节数组、表单…）整体替换掉，
     * 只留 Jackson 会让别的请求类型全失效。
     */
    @Override
    public void extendMessageConverters(List<HttpMessageConverter<?>> converters) {
        converters.removeIf(c -> c instanceof MappingJackson2HttpMessageConverter);

        ObjectMapper objectMapper = new ObjectMapper();
        // 全局日期格式：java.util.Date 统一序列化成 yyyy-MM-dd（与 hire_date 的 DATE 列一致）
        objectMapper.setDateFormat(new SimpleDateFormat("yyyy-MM-dd"));
        // 未知字段忽略：前端多传一个字段不该把整个请求打成 500
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

        MappingJackson2HttpMessageConverter jackson = new MappingJackson2HttpMessageConverter(objectMapper);
        jackson.setDefaultCharset(StandardCharsets.UTF_8);
        converters.add(0, jackson);
    }
}
