package com.liuxu.example.test;

import com.liuxu.springframework.beans.annotion.Component;
import com.liuxu.springframework.beans.annotion.PostConstruct;
import com.liuxu.springframework.beans.interfaces.InitializingBean;

/**
 * 测试初始化方法
 *
 * @date: 2025-07-21
 * @author: liuxu
 */
@Component
public class Bean4TestInit implements InitializingBean {
    @Override
    public void afterPropertiesSet() throws Exception {
        System.out.println("Bean4TestInit.afterPropertiesSet() 实现接口，初始化方法执行了");
    }

    @PostConstruct
    public void init2() {
        System.out.println("Bean4TestInit.init2() 使用 @PostConstruct 注解，初始化方法执行了");
    }
}
