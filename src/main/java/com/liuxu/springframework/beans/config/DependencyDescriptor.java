package com.liuxu.springframework.beans.config;

import com.liuxu.springframework.beans.interfaces.BeanFactory;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;

/**
 * 即将完成特定注入的依赖详细描述
 *
 * @date: 2025-07-05
 * @author: liuxu
 */
public class DependencyDescriptor {
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(DependencyDescriptor.class);
    private static final String LOG_PREFIX = "[DependencyDescriptor]";

    /** 说明需要的类型 */
    private final Class<?> declaringClass;

    /** 字段 */
    protected Field field;

    /** 字段的注解 */
    private volatile Annotation[] fieldAnnotations;

    /** 字段名 */
    private String fieldName;

    /** 是否必须 */
    private final boolean required;


    public DependencyDescriptor(Field field, boolean required) {
        this.field = field;
        this.required = required;
        this.declaringClass = field.getDeclaringClass();
        this.fieldName = field.getName();
    }

    public DependencyDescriptor(DependencyDescriptor original) {
        this.field = original.field;
        this.required = original.required;
        this.declaringClass = original.declaringClass;
        this.fieldName = original.fieldName;
    }

    /**
     * 获取字段的所有注解
     *
     * @return 注解
     */
    public Annotation[] getAnnotations() {
        if (this.field != null) {
            Annotation[] annotations = this.fieldAnnotations;
            if (annotations == null) {
                annotations = this.field.getAnnotations();
                this.fieldAnnotations = annotations;
            }
            return annotations;
        }
        log.error("{} 注入的依赖描述中不存在任何注解.", LOG_PREFIX);
        return new Annotation[]{};
    }

    /**
     * 返回依赖的类型
     *
     * @return 依赖的Class<?> 或者 null (如果不存在)
     */
    public Class<?> getDependencyType() {
        if (this.field != null) {
            return this.field.getType();
        }
        log.error("{} 注入的依赖描述符中不存在 filed.", LOG_PREFIX);
        return null;
    }

    public String getFieldName() {
        return fieldName;
    }

    /** 是否必须 */
    public boolean isRequired() {
        return required;
    }

    /**
     * 根据 BeanName 解析获取候选的 bean 对象
     *
     * @param beanName     beanName
     * @param requiredType requiredType
     * @param beanFactory  beanFactory
     * @return 候选的 bean 对象
     */
    public Object resolveCandidate(String beanName, Class<?> requiredType, BeanFactory beanFactory) {

        return beanFactory.getBean(beanName);
    }

    /**
     * 解析 bean 实例的捷径
     * - 不同的依赖描述应当扩展自己的解析逻辑，详见子类实现
     *
     * @param beanFactory beanFactory
     * @return 候选的 bean 对象
     */
    public Object resolveShortcut(BeanFactory beanFactory) {
        return null;
    }


}
