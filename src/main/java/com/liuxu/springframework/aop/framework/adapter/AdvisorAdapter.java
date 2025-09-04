package com.liuxu.springframework.aop.framework.adapter;

import com.liuxu.springframework.aop.Advice;
import com.liuxu.springframework.aop.Advisor;
import com.liuxu.springframework.aop.MethodInterceptor;

/**
 * Advisor 适配器。将没有实现拦截接口的Advisor适配成对应的拦截器
 *
 * @date: 2025-08-25
 * @author: liuxu
 */
public interface AdvisorAdapter {

    /**
     * 此适配器是否支持此通知对象？使用此通知作为参数的 Advisor 调用该方法是否 {@link #getInterceptor(Advisor)}} 有效？
     *
     * @param advice 通知对象
     * @return 是否支持
     */
    boolean supportsAdvice(Advice advice);

    /**
     * 获取通知对应的拦截器
     *
     * @param advisor 通知
     * @return 拦截器
     */
    MethodInterceptor getInterceptor(Advisor advisor);
}
