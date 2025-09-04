package com.liuxu.springframework.aop.matches;

import com.liuxu.springframework.aop.Pointcut;

/**
 * 类级别过滤功能 接口
 * <p>
 * 判断指定类是否可能满足切点表达式
 *
 * @date: 2025-08-21
 * @author: liuxu
 * @see Pointcut
 */

@FunctionalInterface
public interface ClassFilter {

    /**
     * 判断指定类是否可能满足切点表达式
     *
     * @param clazz 目标类
     * @return true: 可能满足切点表达式
     */
    boolean matches(Class<?> clazz);


    /**
     * Canonical instance of a {@code ClassFilter} that matches all classes.
     */
    ClassFilter TRUE = TrueClassFilter.INSTANCE;


}
