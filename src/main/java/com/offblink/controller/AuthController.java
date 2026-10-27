package com.offblink.controller;

import com.offblink.common.BusinessException;
import com.offblink.common.Result;
import com.offblink.interceptor.LoginInterceptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import java.util.HashMap;
import java.util.Map;

/**
 * 登录/登出：演示「会话级鉴权」——登录成功把用户写进 Session，
 * 之后每个请求由 LoginInterceptor 检查 Session 里有没有这个人。
 *
 * 演示账号写死在代码里（admin / 123456）：本实验没有用户表，登录只为验证拦截器链路，
 * 真实项目应换成用户表 + 密码摘要（BCrypt）校验，绝不能明文比对。
 */
@RestController
public class AuthController {

    private static final Logger log = LoggerFactory.getLogger(AuthController.class);

    private static final String DEMO_USERNAME = "admin";
    private static final String DEMO_PASSWORD = "123456";

    /**
     * 登录：请求体 {"username":"admin","password":"123456"}
     * 成功返回登录名，并把用户名放进 Session（SESSION_KEY = loginUser）。
     */
    @PostMapping("/login")
    public Result<Map<String, Object>> login(@RequestBody Map<String, String> body, HttpServletRequest request) {
        String username = body.get("username");
        String password = body.get("password");

        if (!DEMO_USERNAME.equals(username) || !DEMO_PASSWORD.equals(password)) {
            throw new BusinessException("用户名或密码错误");
        }

        // 登录成功必须换新 Session（防会话固定攻击），再把身份写进去
        HttpSession oldSession = request.getSession(false);
        if (oldSession != null) {
            oldSession.invalidate();
        }
        request.getSession(true).setAttribute(LoginInterceptor.SESSION_KEY, username);

        log.info("[AUTH] {} 登录成功", username);
        Map<String, Object> data = new HashMap<>();
        data.put("username", username);
        return Result.ok(data);
    }

    /** 登出：销毁 Session，之后业务接口重新变回 401 */
    @GetMapping("/logout")
    public Result<Void> logout(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session != null) {
            session.invalidate();
        }
        return Result.ok();
    }
}
