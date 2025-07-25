package com.liuxu.example.test;

import com.liuxu.springframework.beans.annotion.Component;
import com.liuxu.springframework.beans.annotion.PreDestroy;
import com.liuxu.springframework.beans.destroy.DisposableBean;

/**
 * @date: 2025-07-20
 * @author: liuxu
 */
@Component
public class Bean3TestDestroy implements DisposableBean, AutoCloseable {

    /*
    DisposableBeanAdapter.destroy()销毁顺序：
        ⅰ. 先执行 @PreDestroy 注解的方法
        ⅱ. 情况一：实现DisposableBean 接口
          1. 执行 DisposableBean 接口 destroy() 方法
          2. 如果显示配置了@Bean(destroyMethod="clean")方法则执行
        ⅲ. 情况二：未实现DisposableBean 接口
          1. 如果显示配置了@Bean(destroyMethod="clean")方法则执行，结束。
          2. 如果未显示配置，却实现了 AutoCloseable 接口，则执行AutoCloseable.close() 方法
          3. 如果都没有则结束。

    AutoCloseable.close()方法的优先级最低，当未实现DisposableBean接口，未显示配置@Bean(destroyMethod="clean")销毁才会执行。
        例如：只用@PreDestroy注解+实现AutoCloseable接口，会先执行@PreDestroy的方法，然后再close()方法。当然不使用@PreDestroy注解，则只执行close()
     */

    @PreDestroy
    public void testDestroy() {
        System.out.println("01 Bean3TestDestroy.testDestroy() 销毁方法执行");
    }

    @Override
    public void destroy() throws Exception {
        System.out.println("02 Bean3TestDestroy.destroy() 销毁方法执行");
    }

    @Override
    public void close() throws Exception {
        System.out.println("03 Bean3TestDestroy.close() 销毁方法执行");
    }
}
