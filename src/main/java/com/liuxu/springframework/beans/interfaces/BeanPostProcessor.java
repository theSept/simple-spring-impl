package com.liuxu.springframework.beans.interfaces;

/**
 * Bean后处理器（围绕着Bean的生命周期）
 *
 * @date: 2025-06-20
 * @author: liuxu
 */
public interface BeanPostProcessor {
    /**
     * 初始化前
     *
     * @param bean     bean对象
     * @param beanName bean名称
     * @return 返回bean对象，可能是子类实现返回新实例对象
     */
    default Object postProcessBeforeInitialization(Object bean, String beanName) {
        return bean;
    }

    /**
     * 初始化完成后
     *
     * @param bean     bean对象
     * @param beanName bean名称
     * @return 返回bean对象，可能是子类实现返回新实例对象
     */
    default Object postProcessAfterInitialization(Object bean, String beanName) {
        return bean;
    }

}