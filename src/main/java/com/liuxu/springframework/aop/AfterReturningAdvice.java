package com.liuxu.springframework.aop;

import java.lang.reflect.Method;

/**
 * 返回值后执行通知
 *
 * @date: 2025-08-12
 * @author: liuxu
 */
public interface AfterReturningAdvice extends AfterAdvice {

    /**
     * 返回值后执行
     *
     * @param returnValue
     * @param method
     * @param args
     * @param target
     * @throws Throwable
     */
    void afterReturning(Object returnValue, Method method, Object[] args, Object target) throws Throwable;

}
