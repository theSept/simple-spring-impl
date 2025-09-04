package com.liuxu.springframework.aop.interceptor;

import com.liuxu.springframework.aop.MethodBeforeAdvice;
import com.liuxu.springframework.aop.MethodInterceptor;
import com.liuxu.springframework.aop.MethodInvocation;

/**
 * 方法前置通知拦截
 *
 * @date: 2025-08-12
 * @author: liuxu
 */
public class MethodBeforeAdviceInterceptor implements MethodInterceptor {

    /**
     * 方法前置通知
     */
    MethodBeforeAdvice advice;

    public MethodBeforeAdviceInterceptor(MethodBeforeAdvice advice) {
        this.advice = advice;
    }

    @Override
    public Object invoke(MethodInvocation mi) throws Throwable {
        // 方法前置通知
        advice.before(mi.getMethod(), mi.getArguments(), mi.getThis());
        // 继续执行拦截链
        return mi.proceed();

    }
}
