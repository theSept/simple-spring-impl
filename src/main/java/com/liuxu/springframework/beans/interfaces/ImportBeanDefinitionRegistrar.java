package com.liuxu.springframework.beans.interfaces;

import com.liuxu.springframework.beans.annotion.AnnotationMateData;
import com.liuxu.springframework.beans.annotion.Import;

/**
 * {@link Import} 注解进行注册BeanDefinition
 *
 * @date: 2025-08-29
 * @author: liuxu
 */
public interface ImportBeanDefinitionRegistrar {


    default void registerBeanDefinitions(AnnotationMateData importAnnotationMateData, BeanDefinitionRegistry registry) {

    }


}
