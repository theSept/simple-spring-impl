package com.liuxu.example.testaop;

import com.liuxu.springframework.beans.DefaultListableBeanFactory;
import com.liuxu.springframework.beans.annotion.ComponentScan;
import com.liuxu.springframework.beans.annotion.Configuration;
import com.liuxu.springframework.beans.annotion.EnableAspectJAutoProxy;

/**
 * @date: 2025-08-28
 * @author: liuxu
 */
@EnableAspectJAutoProxy() // 开启Spring AOP 注解代理
@Configuration // 作为配置类
@ComponentScan // 开启扫描,默认当前类所在的包及其子包进行扫描
public class TestAopMain {
    public static void main(String[] args) {
        DefaultListableBeanFactory beanFactory = DefaultListableBeanFactory.run(TestAopMain.class);

        // testJDKProxy(beanFactory);

        // testCglibProxy(beanFactory);

        testCycle(beanFactory);


    }

    // 测试存在切面需要代理的场景下循环依赖
    private static void testCycle(DefaultListableBeanFactory beanFactory) {
        BeanAopSupport beanAop1 = beanFactory.getBean("beanAop1", BeanAopSupport.class);
        beanAop1.getBeanAop3().getBeanAop2().getBeanAop1().foo();
    }

    private static void testCglibProxy(DefaultListableBeanFactory beanFactory) {
        // 开启Cglib代理需要添加JVM参数：--add-opens java.base/java.lang=ALL-UNNAMED
        BeanAop2 beanAop2 = beanFactory.getBean("beanAop2", BeanAop2.class);
        beanAop2.foo();
        beanAop2.foo2(10, "abcd");
    }

    private static void testJDKProxy(DefaultListableBeanFactory beanFactory) {
        // 测试jdk代理
        BeanAopSupport beanAop1 = beanFactory.getBean("beanAop1", BeanAopSupport.class);
        // beanAop1.foo();

        int i = beanAop1.foo2Sum(10, 20);
        System.out.println("目标方法执行结果:" + i);
    }
}
