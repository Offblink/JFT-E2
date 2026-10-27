package com.offblink.spring.aop;

/**
 * 切面的通知对象：普通业务方法，本身不知道切面的存在
 */
public interface UserService {

    void saveUser(String name);

    int deleteUser(String name);

    String findUser(String name);

    /** 必炸的危险操作：触发 @AfterThrowing / @Around 异常分支 */
    void riskyOperation();
}
