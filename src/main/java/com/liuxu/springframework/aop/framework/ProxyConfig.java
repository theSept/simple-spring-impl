package com.liuxu.springframework.aop.framework;

/**
 * 第一层：负责代理配置超类。
 * 职责：只管配置
 *
 * @date: 2025-08-24
 * @author: liuxu
 */
public class ProxyConfig {

    /**
     * 是否强制走目标类代理（CGLIB）
     */
    private boolean proxyTargetClass;

    /**
     * 是否在 ThreadLocal 里暴露当前代理对象
     * 解决 代理方法内部的自调用（self-invocation）失效问题
     */
    boolean exposeProxy = false;

    /**
     * 配置是否被冻结
     */
    private boolean frozen = false;


    public boolean isProxyTargetClass() {
        return proxyTargetClass;
    }

    public void setProxyTargetClass(boolean proxyTargetClass) {
        this.proxyTargetClass = proxyTargetClass;
    }

    public boolean isExposeProxy() {
        return exposeProxy;
    }

    public void setExposeProxy(boolean exposeProxy) {
        this.exposeProxy = exposeProxy;
    }

    public boolean isFrozen() {
        return frozen;
    }

    public void setFrozen(boolean frozen) {
        this.frozen = frozen;
    }


    /**
     * 拷贝代理配置
     *
     * @param config 源代理配置
     */
    public void copyFrom(ProxyConfig config) {
        this.exposeProxy = config.exposeProxy;
        this.frozen = config.frozen;
        this.proxyTargetClass = config.proxyTargetClass;
    }


}
