package com.liuxu.springframework.utils;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.function.Consumer;

/**
 * 反射工具类
 *
 * @date: 2025-07-02
 * @author: liuxu
 */
public abstract class ReflectionUtils {


    /**
     * 消费指定 Class 的每个 Field
     *
     * @param targetClass Class
     * @param consumer    Field 消费者
     */
    public static void doWithLocalFields(Class<?> targetClass, Consumer<Field> consumer) {
        if (targetClass == null) {
            throw new RuntimeException("处理类型的字段出现异常，targetClass is null exception");
        }
        for (Field field : targetClass.getDeclaredFields()) {
            consumer.accept(field);
        }
    }

    /**
     * 迭代消费指定 Class 的每个 Method
     *
     * @param targetClass Class
     * @param consumer    Method 消费者
     */
    public static void doWithLocalMethods(Class<?> targetClass, Consumer<Method> consumer) {
        if (targetClass == null) {
            throw new RuntimeException("处理类型的方法时出现异常，targetClass is null exception");
        }
        for (Method method : targetClass.getDeclaredMethods()) {
            consumer.accept(method);
        }
    }

    /**
     * 使给定字段可访问，必要时显式设置它可访问。
     * {@code setAccessible(true)} 该方法仅在实际需要时调用，以避免不必要的冲突。
     *
     * @param field 要检查的字段
     */
    public static void makeAccessible(Field field) {
        if ((!Modifier.isPublic(field.getModifiers()) ||
                !Modifier.isPublic(field.getDeclaringClass().getModifiers()) ||
                Modifier.isFinal(field.getModifiers())) && !field.isAccessible()) {
            field.setAccessible(true);
        }
    }


    /**
     * 使给定方法可访问，必要时显式设置它可访问。
     * {@code setAccessible(true)} 该方法仅在实际需要时调用，以避免不必要的冲突。
     *
     * @param method 要检查的方法
     */
    public static void makeAccessible(Method method) {
        if ((!Modifier.isPublic(method.getModifiers()) ||
                !Modifier.isPublic(method.getDeclaringClass().getModifiers())) && !method.isAccessible()) {
            method.setAccessible(true);
        }
    }

}
