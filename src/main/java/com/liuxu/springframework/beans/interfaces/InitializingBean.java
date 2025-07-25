package com.liuxu.springframework.beans.interfaces;

import com.liuxu.springframework.beans.destroy.DisposableBean;

/**
 * 核心生命周期接口，它定义了 Bean 初始化完成的回调机制。
 * - 执行时机：Bean实例化且注入依赖后
 * <p>
 * Interface to be implemented by beans that need to react once all their properties
 * have been set by a {@link BeanFactory}: e.g. to perform custom initialization,
 * or merely to check that all mandatory properties have been set.
 *
 * <p>An alternative to implementing {@code InitializingBean} is specifying a custom
 * init method, for example in an XML bean definition. For a list of all bean
 * lifecycle methods, see the {@link BeanFactory BeanFactory javadocs}.
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @see DisposableBean
 * @see org.springframework.beans.factory.config.BeanDefinition#getPropertyValues()
 * @see org.springframework.beans.factory.support.AbstractBeanDefinition#getInitMethodName()
 */
public interface InitializingBean {

    /**
     * 在设置了所有 bean 属性和 satisfied BeanFactoryAware 等后由 BeanFactory 调用。
     * 此方法允许 Bean 实例在设置所有 Bean 属性后执行其整体配置和最终初始化的验证。
     */
    void afterPropertiesSet() throws Exception;

}