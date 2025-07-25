package com.liuxu.example.test;

import com.liuxu.springframework.beans.DefaultListableBeanFactory;
import com.liuxu.springframework.beans.annotion.Component;
import com.liuxu.springframework.beans.interfaces.BeanFactory;
import com.liuxu.springframework.beans.interfaces.BeanFactoryAware;
import com.liuxu.springframework.beans.interfaces.BeanNameAware;

/**
 * 测试内置感知接口
 *
 * @date: 2025-07-21
 * @author: liuxu
 */
@Component
public class Bean6TestAware implements BeanNameAware, BeanFactoryAware {
    private DefaultListableBeanFactory defaultListableBeanFactory;
    private String beanName;

    @Override
    public void setBeanFactory(BeanFactory beanFactory) {
        this.defaultListableBeanFactory = (DefaultListableBeanFactory) beanFactory;
    }

    @Override
    public void setBeanName(String name) {
        this.beanName = name;
    }

    public void print(){
        System.out.println("Bean6TestAware.print()  beanName:" + beanName);
        System.out.println("Bean6TestAware.print()  beanFactory:" + defaultListableBeanFactory);
    }


}
