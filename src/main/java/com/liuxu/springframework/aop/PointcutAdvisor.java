package com.liuxu.springframework.aop;

/**
 * 支持切点的切面接口
 *
 * @date: 2025-08-17
 * @author: liuxu
 */
public interface PointcutAdvisor extends Advisor {

    /**
     * 获取驱动该方法切面的切入点。
     */
    Pointcut getPointcut();

}
