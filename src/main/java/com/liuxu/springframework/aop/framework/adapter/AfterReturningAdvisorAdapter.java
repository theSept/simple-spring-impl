package com.liuxu.springframework.aop.framework.adapter;

import com.liuxu.springframework.aop.Advice;
import com.liuxu.springframework.aop.Advisor;
import com.liuxu.springframework.aop.AfterReturningAdvice;
import com.liuxu.springframework.aop.MethodInterceptor;
import com.liuxu.springframework.aop.interceptor.AfterReturningAdviceInterceptor;

/**
 * 将 Advisor 适配成返回值拦截器
 *
 * @date: 2025-08-25
 * @author: liuxu
 */
public class AfterReturningAdvisorAdapter implements AdvisorAdapter {
    @Override
    public boolean supportsAdvice(Advice advice) {
        return advice instanceof AfterReturningAdvice;
    }

    @Override
    public MethodInterceptor getInterceptor(Advisor advisor) {
        return new AfterReturningAdviceInterceptor((AfterReturningAdvice) advisor.getAdvice());
    }

}
