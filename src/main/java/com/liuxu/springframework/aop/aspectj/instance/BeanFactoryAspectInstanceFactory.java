package com.liuxu.springframework.aop.aspectj.instance;

import com.liuxu.springframework.aop.aspectj.annotation.AspectMetadata;
import com.liuxu.springframework.beans.DefaultListableBeanFactory;
import com.liuxu.springframework.beans.interfaces.BeanFactory;
import com.liuxu.springframework.utils.ClassUtils;

/**
 * 核心作用：
 * 1. 保存 BeanFactory 引用和切面 Bean 的名字。
 * 2. 当 AOP 需要某个切面对象时，通过 BeanFactory 获取（支持单例和多例）。
 * 3. 提供切面的 AspectMetadata
 *
 * @date: 2025-08-16
 * @author: liuxu
 */
public class BeanFactoryAspectInstanceFactory implements MetadataAwareAspectInstanceFactory {

    private final BeanFactory beanFactory;

    // AspectJ 切面类在Spring容器中的 BeanName
    private final String name;

    // AspectJ 切面类元信息
    private final AspectMetadata aspectMetadata;


    public BeanFactoryAspectInstanceFactory(String name, BeanFactory beanFactory) {
        this(beanFactory, name, null);
    }

    public BeanFactoryAspectInstanceFactory(BeanFactory beanFactory, String name, Class<?> type) {
        this.beanFactory = beanFactory;
        this.name = name;
        Class<?> aspectType = type;
        if (aspectType == null) {
            aspectType = this.beanFactory.getType(name);
        }
        // 创建该Aspect切面类的元数据信息类
        this.aspectMetadata = new AspectMetadata(name, aspectType);
    }

    @Override
    public AspectMetadata getAspectMetadata() {
        return this.aspectMetadata;
    }

    @Override
    public Object getAspectCreationMutex() {
        return null;
    }

    @Override
    public Object getAspectInstance() {
        return this.beanFactory.getBean(this.name);
    }

    @Override
    public ClassLoader getAspectClassLoader() {
        return (this.beanFactory instanceof DefaultListableBeanFactory dbf
                ? dbf.getBeanClassLoader() : ClassUtils.getDefaultClassLoader());
    }
}
