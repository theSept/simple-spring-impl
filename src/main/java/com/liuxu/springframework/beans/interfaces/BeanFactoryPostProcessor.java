package com.liuxu.springframework.beans.interfaces;

import com.liuxu.springframework.beans.DefaultListableBeanFactory;

/**
 * BeanFactory后处理器 -补充Bean定义
 *
 * @date: 2025-06-20
 * @author: liuxu
 */
public interface BeanFactoryPostProcessor {

    /**
     * 在标准初始化后修改应用程序上下文的内部 bean 工厂。
     * 所有 bean 定义都已加载，但尚未实例化任何 bean。
     * 这允许覆盖或添加属性，甚至对预先初始化的 bean。
     *
     * @param beanFactory the bean factory used by the application context
     */
    void postProcessBeanFactory(DefaultListableBeanFactory beanFactory);

}
