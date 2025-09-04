package com.liuxu.springframework.aop.aspectj.advice;

import com.liuxu.springframework.aop.MethodInterceptor;
import com.liuxu.springframework.aop.MethodInvocation;
import com.liuxu.springframework.aop.ProxyMethodInvocation;
import com.liuxu.springframework.aop.aspectj.MethodInvocationProceedingJoinPoint;
import com.liuxu.springframework.aop.aspectj.instance.AspectInstanceFactory;
import com.liuxu.springframework.aop.aspectj.pointcut.AspectJExpressionPointcut;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.weaver.tools.JoinPointMatch;

import java.lang.reflect.Method;

/**
 * 环绕通知，成型的拦截器
 *
 * @date: 2025-08-17
 * @author: liuxu
 */
public class AspectJAroundAdvice extends AbstractAspectJAdvice implements MethodInterceptor {


    public AspectJAroundAdvice(Method aspectJAdviceMethod, AspectJExpressionPointcut pointcut, AspectInstanceFactory aspectInstanceFactory) {
        super(aspectJAdviceMethod, pointcut, aspectInstanceFactory);
    }

    @Override
    public Object invoke(MethodInvocation mi) throws Throwable {
        if (!(mi instanceof ProxyMethodInvocation pmi)) {
            throw new IllegalStateException("MethodInvocation is not a Spring ProxyMethodInvocation: " + mi);
        }
        ProceedingJoinPoint pjp = lazyGetProceedingJoinPoint(pmi);
        JoinPointMatch jpm = getJoinPointMatch();
        return invokeAdviceMethod(pjp, jpm, null, null);
    }

    protected boolean supportsProceedingJoinPoint() {
        return true;
    }

    @Override
    public boolean isBeforeAdvice() {
        return false;
    }

    @Override
    public boolean isAfterAdvice() {
        return false;
    }

    protected ProceedingJoinPoint lazyGetProceedingJoinPoint(ProxyMethodInvocation rmi) {
        // 包装一个 ProceedingJoinPoint，封装 ProxyMethodInvocation 内部API
        return new MethodInvocationProceedingJoinPoint(rmi);
    }


}
