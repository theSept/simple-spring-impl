package com.liuxu.springframework.beans.interfaces;

/**
 * Spring上下文顶级父类
 *
 * @date: 2025-06-20
 * @author: liuxu
 */
public interface BeanFactory {


    /**
     * 是否包含bean
     *
     * @param name beanName
     * @return true/false
     */
    boolean containsBean(String name);


    /**
     * 获取bean
     *
     * @param name beanName
     * @return bean对象
     */
    Object getBean(String name);

    /**
     * 获取bean ，指定类型
     *
     * @param name         beanName
     * @param requiredType bean类型
     * @param <T>          泛型
     * @return bean
     */
    <T> T getBean(String name, Class<T> requiredType);


    /**
     * 根据给定的BeanName 获取 bean对象的类型
     *
     * @param name BeanName
     * @return Bean 的类型 或 {@code null} (如果不存在)
     */
    Class<?> getType(String name);

    /**
     * 根据给定的BeanName 获取 bean对象的类型
     *
     * @param name
     * @param allowFactoryBeanInit 是否允许初始化FactoryBean
     * @return Bean 的类型 或 {@code null} (如果不存在)
     */
    Class<?> getType(String name, boolean allowFactoryBeanInit);

    /**
     * 检查具有给定名称的 Bean 是否与指定的类型匹配。
     * 会将别名翻译成bean容器中存储的名称
     *
     * @param name        bean名称
     * @param typeToMatch 要匹配的类型
     * @return 如果匹配，则为true
     */
    boolean isTypeMatch(String name, Class<?> typeToMatch);


}
