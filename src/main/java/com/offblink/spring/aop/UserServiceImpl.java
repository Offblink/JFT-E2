package com.offblink.spring.aop;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * 用户服务实现：纯业务逻辑，日志横切关注点全部交给 LogAspect
 */
@Component("userService")
public class UserServiceImpl implements UserService {

    // 选定一个类，为这个类（业务）创建自己的日志对象
    private static final Logger log = LoggerFactory.getLogger(UserServiceImpl.class);

    @Override
    public void saveUser(String name) {
        log.info("[saveUser] 正在保存用户: {}", name);
    }

    @Override
    public int deleteUser(String name) {
        log.info("[deleteUser] 正在删除用户: {}", name);
        return 1;
    }

    @Override
    public String findUser(String name) {
        log.info("[findUser] 正在查询用户: {}", name);
        return "用户[" + name + "]";
    }

    @Override
    public void riskyOperation() {
        log.info("[riskyOperation] 执行危险操作...");
        throw new RuntimeException("模拟数据库连接失败!");
    }
}
