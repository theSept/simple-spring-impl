package com.liuxu.springframework.beans.annotion;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 注入优先级注解
 * <p>
 * - 注入bean实例时，当存在多个候选实例，取优先级最高的
 * - 同类实例如果优先级相同将抛出异常
 *
 * @date: 2025-07-05
 * @author: liuxu
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface Priority {

    /**
     * 优先级：值越小优先级越高
     */
    int value();
}
