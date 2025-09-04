package com.liuxu.springframework.beans.annotion;

import com.liuxu.springframework.beans.interfaces.BeanDefinitionRegistry;
import com.liuxu.springframework.beans.postprocessor.ConfigurationClassPostProcessor;

/**
 * {@link Import} 注解进行注册BeanDefinition
 *
 * @date: 2025-08-29
 * @author: liuxu
 */
public interface ImportBeanDefinitionRegistrar {


    default void registerBeanDefinitions(AnnotationMateData annotationMateData, BeanDefinitionRegistry registry) {
        registerBeanDefinitions(registry);
    }


    default void registerBeanDefinitions(BeanDefinitionRegistry registry) {
    }

}
