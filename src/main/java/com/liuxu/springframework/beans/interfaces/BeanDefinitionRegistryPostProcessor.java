package com.liuxu.springframework.beans.interfaces;

/**
 * BeanFactory 解析Config，注册相应的 BeanDefinition
 *
 * @date: 2025-08-29
 * @author: liuxu
 */
public interface BeanDefinitionRegistryPostProcessor extends BeanFactoryPostProcessor {

    /**
     * 在标准初始化后修改应用程序上下文的内部 Bean 定义注册表。
     * 所有常规 bean 定义都已加载，但尚未实例化任何 bean。
     * 这允许在下一个后处理阶段开始之前添加更多的 bean 定义
     *
     * @param registry 支持注册 BeanDefinition 的bean容器
     */
    void postProcessBeanDefinitionRegistry(BeanDefinitionRegistry registry);

}
