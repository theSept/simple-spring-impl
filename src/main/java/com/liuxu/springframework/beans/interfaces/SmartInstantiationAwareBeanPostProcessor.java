package com.liuxu.springframework.beans.interfaces;

/**
 * 接口的 {@link InstantiationAwareBeanPostProcessor} 扩展，添加一个回调，用于预测已处理 Bean 的最终类型。
 * 注意： 该接口是一个专用接口，主要用于框架内的内部使用。通常，应用程序提供的后处理器应该简单地实现普通 {@link BeanPostProcessor} 接口
 *
 * @date: 2025-08-13
 * @author: liuxu
 */
public interface SmartInstantiationAwareBeanPostProcessor extends InstantiationAwareBeanPostProcessor {


    /**
     * 预测最终要从此处理器的 {@link #postProcessBeforeInstantiation} 回调返回的 bean 类型，默认null
     *
     * @param beanClass
     * @param beanName
     * @return
     * @throws Exception
     */
    default Class<?> predictBeanType(Class<?> beanClass, String beanName) throws Exception {
        return null;
    }

    /**
     * 确定最终要从此处理器的 {@link #postProcessBeforeInstantiation} 回调返回的 Bean 类型
     *
     * @param beanClass
     * @param beanName
     * @return
     */
    default Class<?> determineBeanType(Class<?> beanClass, String beanName) throws Exception {
        return beanClass;
    }


    /**
     * 循环依赖时尝试从三级缓存获取依赖的对象
     *
     * @param bean
     * @param beanName
     * @return
     * @throws Exception
     */
    default Object getEarlyBeanReference(Object bean, String beanName) {
        return bean;
    }


}
