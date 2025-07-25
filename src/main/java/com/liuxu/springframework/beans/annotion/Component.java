package com.liuxu.springframework.beans.annotion;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 定义为bean组件注解，使用该注解的类将会被容器管理
 *
 * @date: 2025-06-20
 * @author: liuxu
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.ANNOTATION_TYPE, ElementType.TYPE})
public @interface Component {
    /**
     * beanName 默认类名首字母小心
     */
    String value() default "";

    /**
     * 是否懒加载 默认false
     */
    boolean lazyInit() default false;
}
