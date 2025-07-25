package com.liuxu.springframework.utils;

import java.lang.reflect.Method;

/**
 * bean工具类
 *
 * @date: 2025-07-19
 * @author: liuxu
 */
public abstract class BeanUtils {

    /**
     * 根据方法名在指定类中获取参数最少的方法实例
     * - 如果无参更好
     * - 如果不存在返回null
     *
     * @param clazz      类
     * @param methodName 方法名
     * @return 方法实例 或 null(如果不存在)
     */
    public static Method findMethodWithMinimalParameters(Class<?> clazz, String methodName) {
        // getDeclaredMethods() 返回所有声明的方法（不包括继承的
        // getMethods() 返回所有public 方法（包括继承的）
        Method method = findMethodWithMinimalParameters(clazz.getDeclaredMethods(), methodName);

        if (method == null && clazz.getSuperclass() != null) {
            method = findMethodWithMinimalParameters(clazz.getSuperclass(), methodName);
        }

        return method;
    }

    /**
     * 从方法列表中获取指定名称且参数最少的方法
     *
     * @param methods    方法列表
     * @param methodName 方法名称
     * @return 方法实例 或者 null (如果方法名称不存在)
     */
    public static Method findMethodWithMinimalParameters(Method[] methods, String methodName) {
        // 目标方法
        Method targetMethod = null;
        // 当前最小参数下找到的方法数量
        int numMethodsFoundWithCurrentMinimumArgs = 0;

        for (Method method : methods) {
            if (method.getName().equals(methodName)) {
                int currCount = method.getParameterCount();
                if (targetMethod == null || currCount < targetMethod.getParameterCount()) {
                    targetMethod = method;
                    numMethodsFoundWithCurrentMinimumArgs = 1;
                } else if (!method.isBridge() && currCount == targetMethod.getParameterCount()) {
                    if (targetMethod.isBridge()) {
                        targetMethod = method;
                    } else {
                        // 不是桥接方法，参数一样，记录
                        numMethodsFoundWithCurrentMinimumArgs++;
                    }
                }
            }
        }

        if (numMethodsFoundWithCurrentMinimumArgs > 1) {
            throw new IllegalStateException("methodName: " + methodName + ". 存在多个方法参数一致的初始化方法，请检查");
        }

        return targetMethod;
    }


}
