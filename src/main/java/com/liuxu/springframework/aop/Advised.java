package com.liuxu.springframework.aop;

import javax.annotation.Nullable;

/**
 * AOP 运行时顶级配置接口
 *
 * @date: 2025-08-12
 * @author: liuxu
 */
@Nullable
public interface Advised {

    /**
     * 返回这个代理“切面”的配置是否被冻结，在这种情况下，无法进行任何建议更改。
     */
    boolean isFrozen();

    /**
     * 我们是否代理完整的目标类（CGLIB）而不是指定的接口代理（JDK）
     *
     * @return true表示代理完整目标类（CGLIB代理）
     */
    boolean isProxyTargetClass();

    /**
     * 更改{@code Advised} 对象使用的 {@code TargetSource}
     * 只有当 {@link #isFrozen()} 不成立时才支持更改
     *
     * @param targetSource 新的 {@code TargetSource}
     */
    void setTargetSource(TargetSource targetSource);


    /**
     * 获取当前{@code Advised}对象使用的 {@code TargetSource}
     *
     * @return 当前的 {@code TargetSource}
     */
    TargetSource getTargetSource();


    /**
     * 设置当前{@code Advised}对象是否在线程暴露代理对象
     *
     * @param exposeProxy
     */
    void setExposeProxy(boolean exposeProxy);


    /**
     * 获取当前{@code Advised}对象是否在线程暴露代理对象
     *
     * @return 当前{@code Advised}对象是否在线程暴露代理对象
     */
    boolean isExposeProxy();


    /**
     * 设置当前{@code Advised}对象是否已经预过滤类匹配
     *
     * @param preFiltered
     */
    void setPreFiltered(boolean preFiltered);


    /**
     * 返回是否已经预过滤当前代理类匹配（仅进行并且通过了类匹配）
     */
    boolean isPreFiltered();


    /**
     * 返回应用此代理对象的切面数组
     */
    Advisor[] getAdvisors();


}
