package com.liuxu.springframework.beans.interfaces;

/**
 * 创建bean实例时感知接口，环绕在bean实例化的整个声明周期
 *
 * @date: 2025-06-25
 * @author: liuxu
 */
public interface InstantiationAwareBeanPostProcessor extends BeanPostProcessor {

    /**
     * 后处理操作 - 在实例化之前
     * 可返回自定义的实例对象作为该beanName的实例
     *
     * @param beanClass 实例化的类
     * @param beanName  实例化的名称
     * @return 实例化对象，返回非null将不会执行内部后续的实例化逻辑
     */
    default Object postProcessBeforeInstantiation(Class<?> beanClass, String beanName) {
        return null;
    }


    /**
     * 在实例化后、填充属性前，进行后处理操作
     * 返回false将不会进行后续的属性注入
     *
     * @param bean     待处理的bean
     * @param beanName beanName
     * @return 返回false不执行后续的属性注入
     */
    default boolean postProcessAfterInstantiation(Object bean, String beanName) {
        return true;
    }

    /**
     * 后处理属性，进行属性注入
     *
     * @param bean     待处理的bean
     * @param beanName beanName
     */
    default void postProcessProperties(Object bean, String beanName) {
    }


}
