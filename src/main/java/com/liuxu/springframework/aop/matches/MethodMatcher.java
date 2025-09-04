package com.liuxu.springframework.aop.matches;

import com.liuxu.springframework.aop.Pointcut;

import java.lang.reflect.Method;

/**
 * 方法匹配功能 接口
 *
 * @date: 2025-08-21
 * @author: liuxu
 * @see Pointcut
 * @see ClassFilter
 */
public interface MethodMatcher {


    /**
     * 执行静态检查以确定给定方法是否匹配。
     * 如果此方法返回 false 并且 {@link #isRuntime() }返回false，
     * 则不会进行运行时检查(即不会调用 {@link MethodMatcher#matches(Method, Class, Object...)})
     *
     * @param method      候选方法
     * @param targetClass 目标类
     * @return 此方法是否静态匹配
     */
    boolean matches(Method method, Class<?> targetClass);


    /**
     * 是否要运行时检查，如果返回 true，则将调用 {@link MethodMatcher#matches(Method, Class, Object...)}
     *
     * @return 运行时检查
     */
    boolean isRuntime();


    /**
     * 检查此方法是否存在运行时（动态）匹配项，该方法必须是静态匹配的。
     * 仅当{@link #matches(Method, Class)} 给定方法和目标类返回true时，才会调用此方法，并且如果isRuntime()返回 true。
     * 在通知链早期运行任何通知之后，在通知的潜在运行之前立即调用。
     *
     * @param method      候选方法
     * @param targetClass 目标类
     * @param args        方法参数
     * @return 是否存在运行时匹配项
     */
    boolean matches(Method method, Class<?> targetClass, Object... args);


    /**
     * Canonical instance of a {@code MethodMatcher} that matches all methods.
     */
    MethodMatcher TRUE = TrueMethodMatcher.INSTANCE;

}
