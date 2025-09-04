package com.liuxu.springframework.aop;

import java.lang.reflect.Method;

/**
 * 方法前置通知
 *
 * @date: 2025-08-12
 * @author: liuxu
 */
public interface MethodBeforeAdvice extends BeforeAdvice {

    /**
     * 前置通知
     *
     * @param method
     * @param args
     * @param target
     * @throws Throwable
     */
    void before(Method method, Object[] args, Object target) throws Throwable;


}
