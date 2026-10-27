package com.offblink.interceptor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

/**
 * 登录拦截器：没登录就别碰业务接口。
 *
 * 与 AOP 的分工：
 *   拦截器工作在 DispatcherServlet 的「Handler 已选出、方法还没调用」阶段，
 *   天然持有 HttpServletRequest/Response，能直接改写状态码和响应体、能直接拒绝请求 —— 适合做鉴权；
 *   AOP 切面只拿到方法参数，看不到请求上下文，更适合做日志/计时这类与 Web 无关的横切。
 *
 * @return false 表示请求到此为止，后面的 Controller 不会被调用。
 */
public class LoginInterceptor implements HandlerInterceptor {

    /** Session 中存放登录名的键（AuthController 写入，这里读） */
    public static final String SESSION_KEY = "loginUser";

    private static final Logger log = LoggerFactory.getLogger(LoginInterceptor.class);

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
            throws Exception {
        // 静态资源走 DefaultServlet，handler 不是 HandlerMethod —— 直接放行
        if (!(handler instanceof HandlerMethod)) {
            return true;
        }

        HttpSession session = request.getSession(false);
        if (session != null && session.getAttribute(SESSION_KEY) != null) {
            return true;
        }

        log.info("[AUTH] 未登录访问 {} {} → 401", request.getMethod(), request.getRequestURI());
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json;charset=UTF-8");
        // 手写 JSON：此处还没进消息转换器，无法复用 Result 的序列化
        response.getWriter().write("{\"code\":401,\"message\":\"未登录或会话已过期\",\"data\":null}");
        return false;
    }
}
