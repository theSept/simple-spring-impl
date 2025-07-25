package com.liuxu.springframework.beans.destroy;

import com.liuxu.springframework.utils.ClassUtils;

/**
 * 方法描述符
 *
 * @date: 2025-07-19
 * @author: liuxu
 */
public record MethodDescriptor(Class<?> declaringClass, String methodName, Class<?>... parameterTypes) {

    /**
     * 创建MethodDescriptor
     * - 处理配置的方法名是包名+类名的场景
     *
     * @param beanName
     * @param beanClass
     * @param methodName
     * @return
     */
    static MethodDescriptor create(String beanName, Class<?> beanClass, String methodName) {
        try {
            Class<?> declaringClass = beanClass;
            String methodNameToUse = methodName;

            /* 处理配置的方法名是包名+类名的场景，注意只能配置父类或自己类的方法 */
            int indOf = methodNameToUse.lastIndexOf(".");
            if (indOf != -1) {
                String className = methodNameToUse.substring(0, indOf);
                methodNameToUse = methodNameToUse.substring(indOf + 1);
                if (!declaringClass.getName().equals(className)) {
                    declaringClass = ClassUtils.forName(className, declaringClass.getClassLoader());
                }
            }

            return new MethodDescriptor(declaringClass, methodNameToUse);
        } catch (Exception e) {
            throw new RuntimeException("beanName: " + beanName + " 创建方法描述符出现错误. " + e);
        }
    }

}