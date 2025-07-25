package com.liuxu.springframework.beans.interfaces;

import com.liuxu.springframework.beans.config.DependencyDescriptor;

import java.util.Set;

/**
 * 自动装配接口
 *
 * @date: 2025-06-20
 * @author: liuxu
 */
public interface AutowireCapableBeanFactory extends BeanFactory {

    <T> T createBean(Class<T> beanClass);

    Object initializeBean(Object existingBean, String beanName);

    void destroyBean(Object existingBean);


    /**
     * 解析依赖，返回所需要的对象
     * 针对此工厂中定义的 bean 解析指定的依赖项。
     *
     * @param descriptor         依赖项的描述符
     * @param requestingBeanName 声明正在给属性依赖注入的 bean 的名称
     * @param autowiredBeanNames (暂未处理，传递空即可)一个 Set，所有自动装配的 bean 的名称（用于解析给定的依赖关系）都应该添加到该集合中
     * @return 已解析的对象，或者 null (如果未找到)
     */
    Object resolveDependency(DependencyDescriptor descriptor, String requestingBeanName, Set<String> autowiredBeanNames);


}
