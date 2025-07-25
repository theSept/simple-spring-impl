package com.liuxu.springframework.utils;

import com.liuxu.springframework.beans.annotion.Component;

/**
 * bean工厂常用工具类
 *
 * @date: 2025-06-20
 * @author: liuxu
 */
public abstract class BeanFactoryUtils {
    /**
     * 根据Class生成beanName
     * - 取值 {@link Component} 注解的 value
     * - 默认取值 class.getName()
     *
     * @param clazz Class对象
     * @return beanName
     */
    public static String generateBeanName(Class<?> clazz) {
        String beanName;
        if (clazz.isAnnotationPresent(Component.class) && !(beanName = clazz.getAnnotation(Component.class).value()).isBlank()) {
            return beanName;
        }
        return toLowerCaseName(clazz.getSimpleName());
    }


    /**
     * 首字母转换小写
     *
     * @param beanName beanName
     * @return 转换后的 beanName,首字母小写
     */
    public static String toLowerCaseName(String beanName) {
        if (beanName.isBlank()) {
            throw new RuntimeException("[toLowerCaseBeanName] beanName " + beanName + " 是空");
        }
        return beanName.substring(0, 1).toLowerCase() + beanName.substring(1);
    }

}
