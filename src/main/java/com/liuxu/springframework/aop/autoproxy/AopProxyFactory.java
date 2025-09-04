package com.liuxu.springframework.aop.autoproxy;

import com.liuxu.springframework.aop.framework.AdvisedSupport;

/**
 * 获取AOP代理的工厂
 *
 * @date: 2025-08-12
 * @author: liuxu
 */
public interface AopProxyFactory {

    /**
     * 创建合适的AOP代理
     *
     * @param config 运行时配置
     * @return AOP代理
     */
    AopProxy createAopProxy(AdvisedSupport config);

}
