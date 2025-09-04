package com.liuxu.springframework.aop;

import java.lang.reflect.Method;

/**
 * 方法调用器
 *
 * @date: 2025-08-12
 * @author: liuxu
 */
public interface MethodInvocation {

    /*方法拦截调用接口*/
    Object proceed() throws Throwable;

    // 获取目标方法
    Method getMethod();

    // 获取方法的参数
    Object[] getArguments();

    // 获取目标对象
    Object getThis();

}
