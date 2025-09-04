package com.liuxu.springframework.aop.framework;

import com.liuxu.springframework.aop.autoproxy.AopProxy;
import com.liuxu.springframework.aop.autoproxy.AopProxyFactory;
import com.liuxu.springframework.aop.autoproxy.DefaultAopProxyFactory;

/**
 * 第三层：真正创建代理的逻辑。
 * 职责：只管怎么创建代理
 *
 * @date: 2025-08-25
 * @author: liuxu
 */
public class ProxyCreatorSupport extends AdvisedSupport {

    /**
     * AOP代理工厂
     */
    private AopProxyFactory aopProxyFactory;

    /**
     * 无参构造，默认使用 {@link DefaultAopProxyFactory#INSTANCE}
     */
    public ProxyCreatorSupport() {
        this.aopProxyFactory = DefaultAopProxyFactory.INSTANCE;
    }

    public ProxyCreatorSupport(AopProxyFactory aopProxyFactory) {
        this.aopProxyFactory = aopProxyFactory;
    }


    /**
     * 创建合适的 AopProxy 对象
     *
     * @return AopProxy对象
     */
    protected AopProxy createAopProxy() {
        return getAopProxyFactory().createAopProxy(this);
    }


    public AopProxyFactory getAopProxyFactory() {
        return this.aopProxyFactory;
    }

    public void setAopProxyFactory(AopProxyFactory aopProxyFactory) {
        this.aopProxyFactory = aopProxyFactory;
    }


}
