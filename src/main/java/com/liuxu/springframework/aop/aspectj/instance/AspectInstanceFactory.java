package com.liuxu.springframework.aop.aspectj.instance;

/**
 * AspectJ切面类的实例工厂
 *
 * @date: 2025-08-20
 * @author: liuxu
 */
public interface AspectInstanceFactory {

    /**
     * 此工厂创建 aspect切面类的实例
     *
     * @return aspect切面类实例对象
     */
    Object getAspectInstance();


    /**
     * 获取 aspect切面类加载器
     *
     * @return aspect切面类加载器
     */
    ClassLoader getAspectClassLoader();
}
