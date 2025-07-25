package com.liuxu.springframework.utils;

import io.github.classgraph.ClassGraph;
import io.github.classgraph.ScanResult;

import java.lang.reflect.Method;
import java.util.List;

/**
 * @date: 2025-06-20
 * @author: liuxu
 */
public abstract class ClassUtils {


    /**
     * 查找指定包名下的所有类
     *
     * @param packageName 包名
     * @return 类列表
     */
    public static List<Class<?>> reflectionsFindClassByPath(String packageName) {
        try (ScanResult scan = new ClassGraph()
                .acceptPackages(packageName)  // 只扫描这个包及其子包
                .scan()) {
            return scan.getAllClasses().loadClasses();
        }
    }


    /**
     * 返回给定方法的限定名称，由完全限定的接口/类名 + “.” + 方法名组成。
     *
     * @param method 方法
     * @return 方法的限定名称
     */
    public static String getQualifiedMethodName(Method method) {
        return getQualifiedMethodName(method, null);
    }

    /**
     * 返回给定方法的限定名称，由完全限定的接口/类名 + “.” + 方法名组成。
     *
     * @param method 方法
     * @param clazz  方法所属的类
     * @return 方法的限定名称
     */
    public static String getQualifiedMethodName(Method method, Class<?> clazz) {
        return clazz != null ? clazz.toString() : method.getDeclaringClass().getName() + "." + method.getName();
    }


    /**
     * 根据类名获取Class
     *
     * @param className   类名
     * @param classLoader 类加载器
     * @return Class
     */
    public static Class<?> forName(String className, ClassLoader classLoader) throws ClassNotFoundException {
        try {
            return classLoader.loadClass(className);
        } catch (ClassNotFoundException e) {
            throw new ClassNotFoundException("class：" + className + " 加载类失败..");
        }

    }
}
