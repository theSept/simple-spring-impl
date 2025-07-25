package com.liuxu.springframework.beans.autowirecapable;

import com.liuxu.springframework.beans.interfaces.AutowireCapableBeanFactory;
import com.liuxu.springframework.beans.interfaces.BeanDefinition;

/**
 * @date: 2025-06-20
 * @author: liuxu
 */
public abstract class AbstractAutowireCapableBeanFactory implements AutowireCapableBeanFactory {
    @Override
    public <T> T createBean(Class<T> beanClass) {
        return null;
    }

    protected void populateBean(String beanName, BeanDefinition bd,Object bean){


    }

    @Override
    public Object initializeBean(Object existingBean, String beanName) {
        return null;
    }


    @Override
    public void destroyBean(Object existingBean) {

    }
}
