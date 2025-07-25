package com.liuxu.springframework.beans.interfaces;

/**
 * 扩展接口，它赋予 Bean 主动感知并获取所属 BeanFactory 的能力
 *
 * @author Rod Johnson
 * @author Chris Beams
 * @see BeanNameAware
 */
public interface BeanFactoryAware extends Aware {

    /**
     * 向 bean 实例提供所属工厂的回调。
     * 在填充普通 bean 属性之后但在初始化回调（例如 InitializingBean.afterPropertiesSet（） 或自定义 init-method）之前调用。
     */
    void setBeanFactory(BeanFactory beanFactory);

}