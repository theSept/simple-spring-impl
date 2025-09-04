package com.liuxu.springframework.aop.aspectj.advice;

import com.liuxu.springframework.aop.AfterAdvice;
import com.liuxu.springframework.aop.MethodInterceptor;
import com.liuxu.springframework.aop.MethodInvocation;
import com.liuxu.springframework.aop.aspectj.instance.AspectInstanceFactory;
import com.liuxu.springframework.aop.aspectj.pointcut.AspectJExpressionPointcut;

import java.lang.reflect.Method;

/**
 * 异常通知 -成型的拦截器
 *
 * @date: 2025-08-17
 * @author: liuxu
 */
public class AspectJAfterThrowingAdvice extends AbstractAspectJAdvice implements MethodInterceptor, AfterAdvice {

    public AspectJAfterThrowingAdvice(Method aspectJAdviceMethod, AspectJExpressionPointcut pointcut, AspectInstanceFactory aspectInstanceFactory) {
        super(aspectJAdviceMethod, pointcut, aspectInstanceFactory);
    }

    @Override
    public boolean isBeforeAdvice() {
        return false;
    }

    @Override
    public boolean isAfterAdvice() {
        return true;
    }

    @Override
    public void setThrowingName(String throwingName) {
        super.setThrowingNameNoCheck(throwingName);
    }

    @Override
    public Object invoke(MethodInvocation methodInvocation) throws Throwable {
        try {
            return methodInvocation.proceed();
        } catch (Throwable ex) {
            // 只有当抛出的异常是指定的异常类型才调用异常通知
            if (shouldInvokeOnThrowing(ex)) {
                invokeAdviceMethod(getJoinPointMatch(), null, ex);
            }
            throw ex;
        }
    }

    /**
     * 在 AspectJ 语义中，仅当抛出的异常是给定抛出类型的子类型时，才会调用抛出指定抛出子句的通知。
     */
    private boolean shouldInvokeOnThrowing(Throwable ex) {
        Class<?> type = getDiscoveredThrowingType();
        return type.isAssignableFrom(ex.getClass());
    }


}
