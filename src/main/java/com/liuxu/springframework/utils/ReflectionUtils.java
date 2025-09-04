package com.liuxu.springframework.utils;

import org.aspectj.lang.annotation.Pointcut;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * 反射工具类
 *
 * @date: 2025-07-02
 * @author: liuxu
 */
public abstract class ReflectionUtils {


    /** 缓存来自基于 Java 8 的接口的等效默认方法，允许快速迭代。 */
    private static final Map<Class<?>, Method[]> declaredMethodsCache = new ConcurrentHashMap<>(256);

    private static final Method[] EMPTY_METHOD_ARRAY = new Method[0];

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

    // 默认参数
    private static final Object[] EMPTY_OBJECT_ARRAY = {};

    /**
     * 调用方法
     *
     * @param method 方法
     * @param target 目标对象
     * @return 方法返回值
     */
    public static Object invokeMethod(Method method, Object target) {
        return invokeMethod(method, target, EMPTY_OBJECT_ARRAY);
    }

    /**
     * 调用方法
     *
     * @param method 方法
     * @param target 目标对象
     * @param args   方法参数
     * @return
     */
    public static Object invokeMethod(Method method, Object target, Object... args) {
        try {
            return method.invoke(target, args);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }


    /**
     * 尝试在提供的类上查找具有提供的名称和参数类型的 Method
     * 如果找不到 Method 则返回null。
     *
     * @param clazz      类
     * @param name       方法名称
     * @param paramTypes 方法参数类型
     * @return 方法
     */
    public static Method findMethod(Class<?> clazz, String name, Class<?>... paramTypes) {
        Class<?> searchType = clazz;
        while (searchType != null) {
            Method[] methods = (searchType.isInterface() ? searchType.getMethods() :
                    getDeclaredMethods(searchType, false));
            for (Method method : methods) {
                if (name.equals(method.getName()) && (paramTypes == null || hasSameParams(method, paramTypes))) {
                    return method;
                }
            }
            searchType = searchType.getSuperclass();
        }
        return null;
    }


    /**
     * 获取指定类和所有超类上所有声明的方法。首先包含子类方法。
     *
     * @param clazz 给定类
     * @return 所有声明的方法数组
     */
    public static Method[] getAllDeclaredMethods(Class<?> clazz) {
        final List<Method> methods = new ArrayList<>(10);
        doWithMethods(clazz, methods::add);
        return methods.toArray(EMPTY_METHOD_ARRAY);
    }


    /**
     * 对给定类和超类（或给定接口和超接口）的所有匹配方法执行给定的回调。
     *
     * @param clazz    类
     * @param consumer 方法回调
     * @return 方法回调
     */
    public static void doWithMethods(Class<?> clazz, Consumer<Method> consumer) {

        Method[] declaredMethods = getDeclaredMethods(clazz, false);
        for (Method declaredMethod : declaredMethods) {
            consumer.accept(declaredMethod);
        }

        if (clazz.getSuperclass() != null && (clazz.getSuperclass() != Object.class)) {
            doWithMethods(clazz.getSuperclass(), consumer);
        } else if (clazz.isInterface()) {
            for (Class<?> superIfc : clazz.getInterfaces()) {
                doWithMethods(superIfc, consumer);
            }
        }

    }


    private static boolean hasSameParams(Method method, Class<?>[] paramTypes) {
        return (paramTypes.length == method.getParameterCount() &&
                Arrays.equals(paramTypes, method.getParameterTypes()));
    }

    private static Method[] getDeclaredMethods(Class<?> clazz, boolean defensive) {
        Method[] result = declaredMethodsCache.get(clazz);
        if (result == null) {
            try {
                Method[] declaredMethods = clazz.getDeclaredMethods();
                List<Method> defaultMethods = findConcreteMethodsOnInterfaces(clazz);
                if (defaultMethods != null) {
                    result = new Method[declaredMethods.length + defaultMethods.size()];
                    System.arraycopy(declaredMethods, 0, result, 0, declaredMethods.length);
                    int index = declaredMethods.length;
                    for (Method defaultMethod : defaultMethods) {
                        result[index] = defaultMethod;
                        index++;
                    }
                } else {
                    result = declaredMethods;
                }
                declaredMethodsCache.put(clazz, (result.length == 0 ? EMPTY_METHOD_ARRAY : result));
            } catch (Throwable ex) {
                throw new IllegalStateException("Failed to introspect Class [" + clazz.getName() +
                        "] from ClassLoader [" + clazz.getClassLoader() + "]", ex);
            }
        }
        return (result.length == 0 || !defensive) ? result : result.clone();
    }

    private static List<Method> findConcreteMethodsOnInterfaces(Class<?> clazz) {
        List<Method> result = null;
        for (Class<?> ifc : clazz.getInterfaces()) {
            for (Method ifcMethod : ifc.getMethods()) {
                if (!Modifier.isAbstract(ifcMethod.getModifiers())) {
                    if (result == null) {
                        result = new ArrayList<>();
                    }
                    result.add(ifcMethod);
                }
            }
        }
        return result;
    }

}
