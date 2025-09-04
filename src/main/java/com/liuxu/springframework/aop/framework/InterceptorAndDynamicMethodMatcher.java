package com.liuxu.springframework.aop.framework;

import com.liuxu.springframework.aop.MethodInterceptor;
import com.liuxu.springframework.aop.matches.MethodMatcher;

/**
 * 内部的拦截器和动态方法匹配器的记录，将 {@link MethodInterceptor}实例和
 * {@link MethodMatcher}实例组合在一起，用作切面链中的元素
 *
 * @date: 2025-08-25
 * @author: liuxu
 */
public record InterceptorAndDynamicMethodMatcher(MethodInterceptor methodInterceptor, MethodMatcher methodMatcher) {
}
