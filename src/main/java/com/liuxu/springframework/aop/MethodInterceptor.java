package com.liuxu.springframework.aop;

/**
 * 方法拦截器
 *
 * @date: 2025-08-12
 * @author: liuxu
 */
public interface MethodInterceptor extends Advice {
    /**
     * 方法调用
     *
     * @param methodInvocation 方法调用
     * @return
     * @throws Throwable
     */
    Object invoke(MethodInvocation methodInvocation) throws Throwable;
}
