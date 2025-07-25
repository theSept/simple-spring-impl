package com.liuxu.springframework.beans.annotion;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 标记为主要的
 * - 当存在同类型的多个 bean 实例时，优先取 {@link Primary} 标记类
 *
 * @date: 2025-07-05
 * @author: liuxu
 */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Primary {

}