package com.liuxu.springframework.aop.framework.adapter;

import com.liuxu.springframework.aop.Advisor;
import com.liuxu.springframework.aop.MethodInterceptor;

/**
 * @date: 2025-08-25
 * @author: liuxu
 */
public interface AdvisorAdapterRegistry {

    /**
     * 适配:返回给定通知的包装类 {@link Advisor}.
     * 默认情况下支持：
     * {@link AfterReturningAdvisorAdapter}
     * {@link MethodBeforeAdvisorAdapter}
     *
     * @param advice 一个通知对象
     * @return 包转成对应的的切面
     */
    Advisor wrap(Object advice) throws IllegalArgumentException;

    /**
     * 返回 AOP MethodInterceptors 数组，适配器支持的转换的拦截器
     *
     * @param advisor 一个切面对象
     * @return MethodInterceptor 数组
     */
    MethodInterceptor[] getInterceptor(Advisor advisor);

    /**
     * 注册一个 {@link AdvisorAdapter} 适配器
     *
     * @param advisorAdapter 一个适配器
     */
    void registerAdvisorAdapter(AdvisorAdapter advisorAdapter);
}
