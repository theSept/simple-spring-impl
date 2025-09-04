package com.liuxu.springframework.beans.interfaces;

/**
 * BeanDefinition 的注册
 *
 * @date: 2025-06-20
 * @author: liuxu
 */
public interface BeanDefinitionRegistry {


    /**
     * 注册 Bean 定义
     *
     * @param name      beanName
     * @param beanClass 需要注入的 Bean 的 Class
     */
    void registryBeanDefinition(String name, Class<?> beanClass);

    /**
     * 注册 bean 定义
     *
     * @param beanName       beanName
     * @param beanDefinition bean定义
     */
    void registryBeanDefinition(String beanName, BeanDefinition beanDefinition);


    /**
     * 判断是否包含指定名称的 bean 定义
     *
     * @param beanName beanName
     * @return true：包含
     */
    boolean containsBeanDefinition(String beanName);


    /**
     * 获取指定名称的 bean 定义
     * @param beanName beanName
     * @return BeanDefinition
     */
    BeanDefinition getBeanDefinition(String beanName);



    // void removeBeanDefinition(String name);

    // BeanDefinition getBeanDefinition(String beanName);

}
