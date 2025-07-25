package com.liuxu.springframework.beans.beandefinition;

/**
 * 预先的未注册的通用 bean 定义类
 * - 允许修改的 bean 定义
 *
 * @date: 2025-06-20
 * @author: liuxu
 */
public class GenericBeanDefinition extends AbstractBeanDefinition {

    public GenericBeanDefinition(Class<?> clazz) {
        super(clazz);
    }

    public GenericBeanDefinition(GenericBeanDefinition beanDefinition) {
        super(beanDefinition);
    }

    @Override
    public AbstractBeanDefinition cloneBeanDefinition() {
        return new GenericBeanDefinition(this);
    }
}
