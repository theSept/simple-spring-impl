package com.liuxu.springframework.aop.interceptor;

import com.liuxu.springframework.aop.AfterAdvice;
import com.liuxu.springframework.aop.AfterReturningAdvice;
import com.liuxu.springframework.aop.MethodInterceptor;
import com.liuxu.springframework.aop.MethodInvocation;

/**
 * 方法返回值后通知拦截器
 *
 * @date: 2025-08-12
 * @author: liuxu
 */
public class AfterReturningAdviceInterceptor implements MethodInterceptor, AfterAdvice {

    /**
     * 返回值后通知
     */
    private final AfterReturningAdvice advice;

    public AfterReturningAdviceInterceptor(AfterReturningAdvice advice) {
        this.advice = advice;
    }

    @Override
    public Object invoke(MethodInvocation mi) throws Throwable {
        // 先执行拦截链，拿到返回值
        Object result = mi.proceed();
        // 返回值后置通知
        this.advice.afterReturning(result, mi.getMethod(), mi.getArguments(), mi.getThis());
        return result;
    }
}
