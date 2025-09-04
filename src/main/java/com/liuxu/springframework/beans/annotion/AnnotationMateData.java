package com.liuxu.springframework.beans.annotion;

import com.liuxu.springframework.beans.postprocessor.ConfigurationClassPostProcessor;
import com.liuxu.springframework.utils.ReflectionUtils;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

/**
 * 注解的元数据
 * {@link ConfigurationClassPostProcessor}
 */
public class AnnotationMateData {
    // 注解的类型
    private final Class<? extends Annotation> annotationClass;
    // 注解的属性和值映射
    private Map<String, Object> attributes;

    public AnnotationMateData(Annotation annotation) {
        if (annotation == null) {
            throw new IllegalArgumentException("annotation must not be null");
        }
        this.annotationClass = annotation.annotationType();
        resolveAttribute(annotation);
    }

    /**
     * 解析注解的属性
     *
     * @param annotation 注解实例
     */
    private void resolveAttribute(Annotation annotation) {
        Method[] declaredMethods = this.annotationClass.getDeclaredMethods();
        this.attributes = new HashMap<>(declaredMethods.length);
        for (Method method : declaredMethods) {
            String methodName = method.getName();

            try {
                ReflectionUtils.makeAccessible(method);
                Object value = ReflectionUtils.invokeMethod(method, annotation);
                attributes.put(methodName, value);
            } catch (Exception e) {
                throw new RuntimeException("获取" + this.annotationClass + "注解的[" + methodName + "()] 属性值出现异常:", e);
            }
        }
    }


    /**
     * 获取指定属性的值
     *
     * @param attribute    属性名称
     * @param defaultValue 默认值
     * @param <T>          属性值类型
     * @return 属性值
     */
    public <T> T getAttributeValue(String attribute, T defaultValue) {
        Object value = this.attributes.get(attribute);
        if (value == null) {
            return defaultValue;
        }
        return (T) value;
    }

}