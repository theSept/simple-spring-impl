package com.liuxu.springframework.utils;

import java.lang.annotation.Annotation;
import java.util.Collection;

/**
 * 注解相关工具类
 *
 * @date: 2025-07-02
 * @author: liuxu
 */
public abstract class AnnotationUtils {

    /**
     * 提前验证当前 Class 是否为候选者
     * - 不是直接检测注解是否存在，而是进行前置过滤筛选。
     * - 通过该方法快速排除明显不符合条件的类，避免后续昂贵的反射操作
     *
     * @param clazz           待校验的Class
     * @param annotationTypes 注解类型
     * @return true:候选者
     */
    public static boolean isCandidateClass(Class<?> clazz, Collection<Class<? extends Annotation>> annotationTypes) {
        for (Class<? extends Annotation> annotationType : annotationTypes) {
            // 只处理不是java包下的类 或者 是java包下的注解
            if ((!clazz.getName().startsWith("java.")) ||
                    (annotationType != null && annotationType.getName().startsWith("java."))) {
                return true;
            }
        }
        return false;
    }
}
