package com.liuxu.springframework.aop.aspectj.advice;

import com.liuxu.springframework.aop.AfterAdvice;
import com.liuxu.springframework.aop.MethodInterceptor;
import com.liuxu.springframework.aop.MethodInvocation;
import com.liuxu.springframework.aop.aspectj.instance.AspectInstanceFactory;
import com.liuxu.springframework.aop.aspectj.pointcut.AspectJExpressionPointcut;

import java.lang.reflect.Method;

/**
 * 后置（最终）通知 - 成型的拦截器
 * 该通知是最后执行的因为它被放在finally代码块中
 *
 * @date: 2025-08-17
 * @author: liuxu
 */
public class AspectJAfterAdvice extends AbstractAspectJAdvice implements MethodInterceptor, AfterAdvice {

    public AspectJAfterAdvice(Method aspectJAdviceMethod, AspectJExpressionPointcut pointcut, AspectInstanceFactory aspectInstanceFactory) {
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
    public Object invoke(MethodInvocation methodInvocation) throws Throwable {
        try {
            return methodInvocation.proceed();
        } finally {
            // 后置（最终）通知
            invokeAdviceMethod(getJoinPointMatch(), null, null);
        }
    }
}
