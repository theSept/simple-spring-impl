package com.liuxu.springframework.utils;

import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
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

    /**
     * 获取指定注解
     *
     * @param clazz          类
     * @param annotationType 注解的类型
     * @param <A>            注解类型
     * @return 注解
     */
    public static <A extends Annotation> A findAnnotation(Class<?> clazz, Class<A> annotationType) {
        if (annotationType == null) {
            return null;
        }

        A annotation = clazz.getDeclaredAnnotation(annotationType);
        if (annotation != null) {
            return annotation;
        }

        return null;
    }

    /**
     * 获取指定注解
     *
     * @param method         方法
     * @param annotationType 注解的类型
     * @param <A>            注解类型
     * @return 注解
     */
    public static <A extends Annotation> A findAnnotation(Method method, Class<A> annotationType) {
        if (method == null || annotationType == null) {
            return null;
        }
        return method.getDeclaredAnnotation(annotationType);
    }

    /**
     * 获取注解属性值
     *
     * @param annotation 注解实例
     * @param attribute  属性名称
     * @return 属性值
     */
    public static Object getValue(Annotation annotation, String attribute) {
        if (annotation == null || attribute == null) {
            return null;
        }

        // 获取注解类型的所有方法(也就是注解的属性)
        for (Method method : annotation.annotationType().getDeclaredMethods()) {
            // 匹配的属性，并且无参，就是需要的注解属性
            if (method.getName().equals(attribute) && method.getParameterCount() == 0) {
                return invokeAnnotationMethod(method, annotation);
            }
        }

        return null;
    }

    /**
     * 调用注解方法（就是拿到注解指定属性的值）
     *
     * @param method     注解的方法
     * @param annotation 注解实例
     * @return 注解属性的值
     */
    private static Object invokeAnnotationMethod(Method method, Annotation annotation) {
        if (annotation == null) {
            return null;
        }

        // 判断是否是代理类的注解
        if (Proxy.isProxyClass(annotation.getClass())) {
            try {
                // 代理类需要 InvocationHandler 调用
                InvocationHandler invocationHandler = Proxy.getInvocationHandler(annotation);
                return invocationHandler.invoke(annotation, method, null);
            } catch (Throwable e) {
                // 无
            }
        }
        // 调用注解的属性方法，拿到注解的属性值
        return ReflectionUtils.invokeMethod(method, annotation);
    }
}
