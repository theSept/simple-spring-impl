package com.liuxu.springframework.aop;

/**
 * 代理方法调用接口
 *
 * @date: 2025-08-12
 * @author: liuxu
 */
public interface ProxyMethodInvocation extends MethodInvocation {

    /**
     * 返回通过此方法调用的代理
     *
     * @return 代理
     */
    Object getProxy();


    /**
     * 创建此对象的克隆，里面的拦截链状态会被保留下来。
     * 如果克隆是在对此对象调用 proceed() 之前完成的，那么克隆对象可以执行后面未执行的拦截链，且不影响原始的对象拦截链。
     * <p>
     * 目的：为了安全地支持多次执行 joinpoint，避免用户破坏责任链的索引推进规则。
     *
     * @return 克隆对象
     */
    MethodInvocation invocableClone();


    /**
     * 创建此对象的克隆，里面的拦截链状态会被保留下来。
     * 如果克隆是在对此对象调用 proceed() 之前完成的，那么克隆对象可以执行后面未执行的拦截链，且不影响原始的对象拦截链。
     * <p>
     * 目的：为了安全地支持多次执行 joinpoint，避免用户破坏责任链的索引推进规则。
     *
     * @param arguments 克隆调用应该使用的参数，覆盖原始参数
     * @return 克隆对象
     */
    MethodInvocation invocableClone(Object... arguments);


    /**
     * 设置用户属性
     *
     * @param key
     * @param value
     */
    void setUserAttribute(String key, Object value);

    /**
     * 返回指定用户属性的值.
     *
     * @param key the name of the attribute
     * @return the value of the attribute, or {@code null} if not set
     * @see #setUserAttribute
     */
    Object getUserAttribute(String key);


}
