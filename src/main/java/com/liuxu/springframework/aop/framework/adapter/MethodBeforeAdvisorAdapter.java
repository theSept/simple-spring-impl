package com.liuxu.springframework.aop.framework.adapter;

import com.liuxu.springframework.aop.Advice;
import com.liuxu.springframework.aop.Advisor;
import com.liuxu.springframework.aop.MethodBeforeAdvice;
import com.liuxu.springframework.aop.MethodInterceptor;
import com.liuxu.springframework.aop.interceptor.MethodBeforeAdviceInterceptor;

/**
 * 将Advisor适配成方法前置通知
 *
 * @date: 2025-08-25
 * @author: liuxu
 */
public class MethodBeforeAdvisorAdapter implements AdvisorAdapter {
    @Override
    public boolean supportsAdvice(Advice advice) {
        return advice instanceof MethodBeforeAdvice;
    }

    @Override
    public MethodInterceptor getInterceptor(Advisor advisor) {
        return new MethodBeforeAdviceInterceptor((MethodBeforeAdvice) advisor.getAdvice());
    }
}
