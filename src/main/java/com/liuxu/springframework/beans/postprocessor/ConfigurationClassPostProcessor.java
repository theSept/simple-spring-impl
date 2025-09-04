package com.liuxu.springframework.beans.postprocessor;

import com.liuxu.springframework.beans.DefaultListableBeanFactory;
import com.liuxu.springframework.beans.annotion.AnnotationMateData;
import com.liuxu.springframework.beans.annotion.Configuration;
import com.liuxu.springframework.beans.annotion.Import;
import com.liuxu.springframework.beans.interfaces.ImportBeanDefinitionRegistrar;
import com.liuxu.springframework.beans.interfaces.BeanDefinition;
import com.liuxu.springframework.beans.interfaces.BeanDefinitionRegistry;
import com.liuxu.springframework.beans.interfaces.BeanDefinitionRegistryPostProcessor;
import com.liuxu.springframework.utils.ClassUtils;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

/**
 * 解析配置，@Bean @Import 。。。。
 * 这里就只是实现 @Import注解了
 * <p>
 * {@link AnnotationMateData}
 *
 * @date: 2025-08-29
 * @author: liuxu
 */
public class ConfigurationClassPostProcessor implements BeanDefinitionRegistryPostProcessor {


    @Override
    public void postProcessBeanDefinitionRegistry(BeanDefinitionRegistry registry) {
        // 解析注册beanDefinition
        if (registry instanceof DefaultListableBeanFactory beanFactory) {
            processConfigBeanDefinitions(beanFactory);
        }


    }

    // 处理配置方式注册bean,将其记录beanDefinition
    public void processConfigBeanDefinitions(DefaultListableBeanFactory beanFactory) {

        // 解析所有配置类上的注解
        String[] configBeanName = beanFactory.getBeanNamesForType(Configuration.class);
        // key:注解的类型  value:配置类型数组
        Map<Annotation, Class<?>[]> candidateBeanDefinition = new HashMap<>();
        for (String beanName : configBeanName) {
            BeanDefinition beanDefinition = beanFactory.getBeanDefinition(beanName);
            HashSet<Class<?>> visited = new HashSet<>(5);

            // 处理元注解使用了 @Import 的注解
            getImports(candidateBeanDefinition, beanDefinition.getBeanType(), visited);
        }

        if (candidateBeanDefinition.isEmpty()) {
            return;
        }

        // 实例化 @Import 注解配置的类 并将注解信息读取处理 一并做映射关联
        List<AnnotationMateDataAndRegister> annotationMateDataAndRegisters = new ArrayList<>(candidateBeanDefinition.size());

        for (Map.Entry<Annotation, Class<?>[]> entry : candidateBeanDefinition.entrySet()) {
            AnnotationMateData annotationMateData = new AnnotationMateData(entry.getKey());
            for (Class<?> importClass : entry.getValue()) {
                if (ImportBeanDefinitionRegistrar.class.isAssignableFrom(importClass)) {
                    ImportBeanDefinitionRegistrar importBeanDefinitionRegistrar = ClassUtils.instantiateClass(importClass, ImportBeanDefinitionRegistrar.class, beanFactory);
                    annotationMateDataAndRegisters.add(new AnnotationMateDataAndRegister(annotationMateData, importBeanDefinitionRegistrar));
                }
            }
        }

        // 调用实例方法,注册对应的BeanDefinition
        loadBeanDefinitionsFromRegistrars(annotationMateDataAndRegisters,beanFactory);
    }


    private void loadBeanDefinitionsFromRegistrars(List<AnnotationMateDataAndRegister> annotationMateDataAndRegisters, DefaultListableBeanFactory beanFactory) {
        for (AnnotationMateDataAndRegister amdAndbdr : annotationMateDataAndRegisters) {
            ImportBeanDefinitionRegistrar bdr = amdAndbdr.getBeanDefinitionRegistrar();
            bdr.registerBeanDefinitions(amdAndbdr.getAnnotationMateData(), beanFactory);
        }
    }

    /**
     * 递归查询 @Import 配置的类型
     *
     * @param candidateBeanDefinition 候选的类型,已读取出来配置的类型
     * @param clazz                   查询的类
     * @param visited                 缓存已查询过的注解
     */
    private void getImports(Map<Annotation, Class<?>[]> candidateBeanDefinition, Class<?> clazz, HashSet<Class<?>> visited) {

        for (Annotation annotation : clazz.getAnnotations()) {
            Class<? extends Annotation> annotationType = annotation.annotationType();

            if (!visited.add(annotationType)) {
                // 已经处理过
                return;
            }

            if (annotationType.getPackageName().startsWith("java.lang.annotation")) {
                // 系统的注解,不处理
                return;
            }

            // 我们只找元注解使用了 @Import 的, 不处理直接使用 @Import 注解在配置类上的
            Import imp = annotationType.getAnnotation(Import.class);
            if (imp != null) {
                candidateBeanDefinition.put(annotation, imp.value());
            }


            // 继续递归查询
            getImports(candidateBeanDefinition, annotationType, visited);
        }

    }


    @Override
    public void postProcessBeanFactory(DefaultListableBeanFactory beanFactory) {


    }


    /** 注解元数据和 ImportBeanDefinitionRegistrar 组合类 */
    private static class AnnotationMateDataAndRegister {
        AnnotationMateData annotationMateData;
        ImportBeanDefinitionRegistrar beanDefinitionRegistrar;

        public AnnotationMateDataAndRegister(AnnotationMateData annotationMateData, ImportBeanDefinitionRegistrar beanDefinitionRegistrar) {
            this.annotationMateData = annotationMateData;
            this.beanDefinitionRegistrar = beanDefinitionRegistrar;
        }

        public AnnotationMateData getAnnotationMateData() {
            return annotationMateData;
        }

        public ImportBeanDefinitionRegistrar getBeanDefinitionRegistrar() {
            return beanDefinitionRegistrar;
        }
    }


}


