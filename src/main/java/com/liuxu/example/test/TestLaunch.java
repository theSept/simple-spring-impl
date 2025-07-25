package com.liuxu.example.test;

import com.liuxu.springframework.beans.DefaultListableBeanFactory;

/**
 * 测试主启动
 *
 * @date: 2025-07-01
 * @author: liuxu
 */

public class TestLaunch {

    public static void main(String[] args) {
        DefaultListableBeanFactory beanFactory = new DefaultListableBeanFactory(MyConfig.class);
        // 测试生命周期
        // testBeanLifecycle(beanFactory);

        // 测试属性注入存在多个候选bean
        testMultiBean(beanFactory);

        // 测试内置增强功能. Aware 感知接口。
        // testBeanAware(beanFactory);


    }

    // 测试内置增强功能. Aware 感知接口。
    private static void testBeanAware(DefaultListableBeanFactory beanFactory) {
        System.out.println("测试内置增强功能. Aware 感知接口。");
        beanFactory.getBean("bean6TestAware",Bean6TestAware.class).print();
    }


    // 测试属性注入存在多个候选bean
    public static void testMultiBean(DefaultListableBeanFactory beanFactory) {
        beanFactory.getBean("bean5MultiBean", Bean5MultiBean.class).print();
    }


    // 测试生命周期
    private static void testBeanLifecycle(DefaultListableBeanFactory beanFactory) {
        Bean1 bean1 = beanFactory.getBean("bean1", Bean1.class);
        Bean2 bean2 = beanFactory.getBean("bean2", Bean2.class);

        System.out.println(bean1.getName());

        beanFactory.close();
    }


}
