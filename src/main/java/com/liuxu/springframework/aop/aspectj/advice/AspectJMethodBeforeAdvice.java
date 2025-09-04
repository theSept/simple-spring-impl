package com.liuxu.springframework.aop.aspectj.advice;


import com.liuxu.springframework.aop.MethodBeforeAdvice;
import com.liuxu.springframework.aop.aspectj.instance.AspectInstanceFactory;
import com.liuxu.springframework.aop.aspectj.pointcut.AspectJExpressionPointcut;

import java.lang.reflect.Method;

/**
 * 方法前置通知
 *
 * @date: 2025-08-17
 * @author: liuxu
 */
public class AspectJMethodBeforeAdvice extends AbstractAspectJAdvice implements MethodBeforeAdvice {


    public AspectJMethodBeforeAdvice(Method aspectJAdviceMethod, AspectJExpressionPointcut pointcut, AspectInstanceFactory aspectInstanceFactory) {
        super(aspectJAdviceMethod, pointcut, aspectInstanceFactory);
    }


    @Override
    public boolean isBeforeAdvice() {
        return true;
    }

    @Override
    public boolean isAfterAdvice() {
        return false;
    }


    @Override
    public void before(Method method, Object[] args, Object target) throws Throwable {
        invokeAdviceMethod(getJoinPointMatch(), null, null);
    }


}
