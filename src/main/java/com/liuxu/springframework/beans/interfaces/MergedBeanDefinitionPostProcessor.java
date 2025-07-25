package com.liuxu.springframework.beans.interfaces;

import com.liuxu.springframework.beans.beandefinition.RootBeanDefinition;

/**
 * 干预 Bean 定义合并过程的核心扩展接口，允许在 Bean 定义（BeanDefinition）完成合并后、实例化前对元数据进行深度处理。
 */
public interface MergedBeanDefinitionPostProcessor extends BeanPostProcessor {

    /**
     * 后处理器- 对合并的 bean 定义进行深度处理，允许可以修改合并的 bean 定义
     *
     * @param beanDefinition the merged bean definition for the bean
     * @param beanType       the actual type of the managed bean instance
     * @param beanName       the name of the bean
     */
    void postProcessMergedBeanDefinition(RootBeanDefinition beanDefinition, Class<?> beanType, String beanName);

    /**
     * A notification that the bean definition for the specified name has been reset,
     * and that this post-processor should clear any metadata for the affected bean.
     * <p>The default implementation is empty.
     *
     * @param beanName the name of the bean
     * @since 5.1
     */
    default void resetBeanDefinition(String beanName) {
    }

}