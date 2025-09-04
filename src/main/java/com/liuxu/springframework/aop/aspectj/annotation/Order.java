package com.liuxu.springframework.aop.aspectj.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * @date: 2025-08-17
 * @author: liuxu
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Order {
    /**
     * 排序序号，值越小优先级越高
     */
    int value() default Integer.MAX_VALUE;



}
